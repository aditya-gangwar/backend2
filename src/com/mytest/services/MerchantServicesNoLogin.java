package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import com.mytest.constants.*;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;
import com.mytest.utilities.MyLogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 14-07-2016.
 */
public class MerchantServicesNoLogin implements IBackendlessService {

    private MyLogger mLogger;
    //private BackendOps mBackendOps;

    /*
     * Public methods: Backend REST APIs
     */
    public void setDeviceForLogin(String loginId, String deviceInfo, String rcvdOtp) {
        initCommon();

        try {
            if (deviceInfo == null || deviceInfo.isEmpty()) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA, "");
            }

            mLogger.debug("In setDeviceForLogin: " + loginId + ": " + deviceInfo);
            //mLogger.debug(InvocationContext.asString());
            //mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

            // fetch merchant
            Merchants merchant = BackendOps.getMerchant(loginId, false);

            // deviceInfo format (from app): <device id>,<manufacturer>,<model>,<os version>
            // add time and otp at the end
            if (rcvdOtp == null || rcvdOtp.isEmpty()) {
                deviceInfo = deviceInfo+","+String.valueOf(System.currentTimeMillis())+",";
            } else {
                deviceInfo = deviceInfo+","+String.valueOf(System.currentTimeMillis())+","+rcvdOtp;
            }
            mLogger.debug("Updated DeviceInfo: "+deviceInfo);

            //TODO: encrypt 'deviceInfo' - as its not reset to NULL after login and hence will reach to the app

            // Update device Info in merchant object
            merchant.setTempDevId(deviceInfo);
            BackendOps.updateMerchant(merchant);

        } catch(Exception e) {
            mLogger.error("Exception in setDeviceForLogin: "+e.toString());
            mLogger.flush();
            throw e;
        }
    }

    public void resetMerchantPwd(String userId, String deviceId, String brandName) {
        initCommon();

        try {
            mLogger.debug("In resetMerchantPwd: " + userId + ": " + deviceId);
            //mLogger.debug("Before: " + InvocationContext.asString());
            //mLogger.debug("Before: " + HeadersManager.getInstance().getHeaders().toString());

            // check if any request already pending
            if( BackendOps.fetchMerchantOps(buildPwdResetWhereClause(userId)) != null) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_DUPLICATE_REQUEST, "");
            }

            // fetch user with the given id with related merchant object
            BackendlessUser user = BackendOps.fetchUser(userId, DbConstants.USER_TYPE_MERCHANT);
            Merchants merchant = (Merchants) user.getProperty("merchant");

            // check admin status
            CommonUtils.checkMerchantStatus(merchant);

            // Check if from trusted device
            // don't check for first time after merchant is registered
            List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
            if (merchant.getFirst_login_ok()) {
                if (!CommonUtils.isTrustedDevice(deviceId, trustedDevices)) {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "");
                }
            }
            /*
            if (merchant.getAdmin_status() != DbConstants.USER_STATUS_NEW_REGISTERED &&
                    trustedDevices!=null &&
                    !trustedDevices.isEmpty() ) {
                if (!CommonUtils.isTrustedDevice(deviceId, trustedDevices)) {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "");
                }
            }*/

            // check for 'extra verification'
            String name = merchant.getName();
            if (name == null || !name.equalsIgnoreCase(brandName)) {
                CommonUtils.handleWrongAttempt(userId, merchant, DbConstants.USER_TYPE_MERCHANT, DbConstantsBackend.ATTEMPT_TYPE_PASSWORD_RESET);
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
            }

            // For new registered merchant - send the password immediately
            if (!merchant.getFirst_login_ok()) {
                handlePasswdResetImmediate(user, merchant);
                mLogger.debug("Processed passwd reset op for: " + merchant.getAuto_id());
            } else {
                // create row in MerchantOps table
                MerchantOps op = new MerchantOps();
                op.setMerchant_id(merchant.getAuto_id());
                op.setMobile_num(merchant.getMobile_num());
                op.setOp_code(DbConstantsBackend.MERCHANT_OP_RESET_PASSWD);
                op.setOp_status(DbConstantsBackend.MERCHANT_OP_STATUS_PENDING);

                BackendOps.saveMerchantOp(op);
                mLogger.debug("Processed passwd reset op for: " + merchant.getAuto_id());
                throw new BackendlessException(BackendResponseCodes.BE_RESPONSE_OP_SCHEDULED, "");
            }

        } catch (Exception e) {
            mLogger.error("Exception in resetMerchantPwd: "+e.toString());
            mLogger.flush();
            throw e;
        }
    }

    public void sendMerchantId(String mobileNum) {
        initCommon();
        try {
            mLogger.debug("In sendMerchantId: " + mobileNum);
            mobileNum = CommonUtils.addMobileCC(mobileNum);

            // fetch user with the registered mobile number
            Merchants merchant = BackendOps.getMerchantByMobile(mobileNum);
            // check admin status
            CommonUtils.checkMerchantStatus(merchant);

            // Not checking for trusted device

            // check for 'extra verification'
            String mobile = merchant.getMobile_num();
            if (mobile == null || !mobile.equalsIgnoreCase(mobileNum)) {
                CommonUtils.handleWrongAttempt(merchant.getAuto_id(), merchant, DbConstants.USER_TYPE_MERCHANT, DbConstantsBackend.ATTEMPT_TYPE_FORGOT_USERID);
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
            }

            // send merchant id by SMS
            String smsText = buildUserIdSMS(merchant.getAuto_id());
            if (!SmsHelper.sendSMS(smsText, merchant.getMobile_num())) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
            }
        } catch (Exception e) {
            mLogger.error("Exception in sendMerchantId: "+e.toString());
            mLogger.flush();
            throw e;
        }
    }

    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.mytest.services.MerchantServicesNoLogin");
        mLogger = new MyLogger(logger);
        //mBackendOps = new BackendOps(mLogger);
        CommonUtils.initTableToClassMappings();
    }

    private void handlePasswdResetImmediate(BackendlessUser user, Merchants merchant) {
        // generate password
        String passwd = CommonUtils.generateTempPassword();
        // update user account for the password
        user.setPassword(passwd);
        BackendOps.updateUser(user);
        //TODO: remove printing passwd
        mLogger.debug("Updated merchant for password reset: "+merchant.getAuto_id()+": "+passwd);

        // Send SMS through HTTP
        String smsText = buildFirstPwdResetSMS(merchant.getAuto_id(), passwd);
        if( !SmsHelper.sendSMS(smsText, merchant.getMobile_num()) )
        {
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
        }
        mLogger.debug("Sent first password reset SMS: "+merchant.getAuto_id());
    }

    private String buildPwdResetWhereClause(String merchantId) {
        StringBuilder whereClause = new StringBuilder();

        // Single password reset request allowed in every 2 hours

        // for particular merchant
        whereClause.append("op_code = '").append(DbConstantsBackend.MERCHANT_OP_RESET_PASSWD).append("'");
        whereClause.append("AND merchant_id = '").append(merchantId).append("'");
        // greater than configured period
        long time = (new Date().getTime()) - (GlobalSettingsConstants.MERCHANT_PASSWORD_RESET_REQUEST_GAP_MINS * 60 * 1000);
        whereClause.append(" AND created > ").append(time);

        mLogger.debug("where clause: "+whereClause.toString());
        return whereClause.toString();
    }

    private String buildFirstPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_FIRST_PASSWD,userId,password);
    }

    private String buildUserIdSMS(String userId) {
        return String.format(SmsConstants.SMS_MERCHANT_ID,userId);
    }
}
