package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import com.mytest.constants.*;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 14-07-2016.
 */
public class MerchantServicesNoLogin implements IBackendlessService {

    private Logger mLogger;
    private BackendOps mBackendOps;

    /*
     * Public methods: Backend REST APIs
     */
    public void setDeviceForLogin(String loginId, String deviceInfo, String rcvdOtp) {
        initCommon();

        if(deviceInfo==null || deviceInfo.isEmpty()) {
            //return BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA;
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA, "Invalid input data: deviceInfo", false);
        }

        mLogger.debug("In setDeviceForLogin: "+loginId+": "+deviceInfo);
        mLogger.debug(InvocationContext.asString());
        mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

        // deviceInfo format: <device id>,<manufacturer>,<model>,<os version>
        String[] csvFields = deviceInfo.split(CommonConstants.CSV_DELIMETER);
        String deviceId = csvFields[0];

        BackendlessUser user = mBackendOps.fetchUser(loginId, DbConstants.USER_TYPE_MERCHANT);
        if(user==null) {
            //return BackendResponseCodes.BE_ERROR_NO_SUCH_USER;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        // check admin status
        String status = CommonUtils.checkMerchantStatus(merchant);
        if(status != null) {
            //return status;
            CommonUtils.throwException(mLogger,status, "Merchant status not active", false);
        }

        boolean matched = false;
        if(rcvdOtp==null || rcvdOtp.isEmpty()) {
            // first run - as did not rcv OTP

            // check if given device id matches trusted list
            List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
            int cnt = 0;
            if(trustedDevices != null) {
                cnt = trustedDevices.size();
                for (MerchantDevice device : trustedDevices) {
                    if(device.getDevice_id().equals(deviceId)) {
                        matched = true;
                        break;
                    }
                }
            }

            // generate OTP if device not matched
            if( !matched ) {
                // Check for max devices allowed per user
                if(cnt >= CommonConstants.MAX_DEVICES_PER_MERCHANT) {
                    //return BackendResponseCodes.BE_ERROR_TRUSTED_DEVICE_LIMIT_RCHD;
                    CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_TRUSTED_DEVICE_LIMIT_RCHD, "Trusted device limit reached", false);
                }
                // First login for this - generate OTP
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(loginId);
                newOtp.setMobile_num(merchant.getMobile_num());
                newOtp.setOpcode(DbConstants.MERCHANT_OP_NEW_DEVICE_LOGIN);
                newOtp = mBackendOps.generateOtp(newOtp);
                if(newOtp == null) {
                    //return BackendResponseCodes.BE_ERROR_OTP_GENERATE_FAILED;
                    CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_OTP_GENERATE_FAILED, mBackendOps.mLastOpStatus, false);
                } else {
                    //return BackendResponseCodes.BE_ERROR_OTP_GENERATED;
                    CommonUtils.throwException(mLogger,BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "OTP generated successfully", true);
                }
            }

        } else {
            // second run - as rcvd otp
            // update device only if OTP matches
            AllOtp fetchedOtp = mBackendOps.fetchOtp(loginId);
            if( fetchedOtp == null ||
                    !mBackendOps.validateOtp(fetchedOtp, rcvdOtp) ) {
                //return BackendResponseCodes.BE_ERROR_WRONG_OTP;
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_WRONG_OTP, "Wrong OTP value", false);
            }
        }

        // If here, means either 'device matched' or 'new device and otp matched'
        // Update device Info in merchant object
        merchant.setTempDevId(deviceInfo);
        user.setProperty("merchant",merchant);
        if( mBackendOps.updateUser(user)==null ) {
            //return BackendResponseCodes.BE_ERROR_GENERAL;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }

        Backendless.Logging.flush();
        //return BackendResponseCodes.BE_RESPONSE_NO_ERROR;
    }

    public void resetMerchantPwd(String userId, String deviceId, String brandName) {
        initCommon();
        mLogger.debug("In resetMerchantPwd: "+userId+": "+deviceId);

        // not required, as supposed to be called by user without logging in (forget password case)
        // this will ensure that backend operations are executed, as logged-in user who called this api using generated SDK
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        mLogger.debug("Before: "+InvocationContext.asString());
        mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

        // check if any request already pending
        ArrayList<MerchantOps> ops = mBackendOps.fetchMerchantOps(buildPwdWhereClause(userId));
        if(ops!=null) {
            // old password request exists
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_DUPLICATE_REQUEST, "Old password reset request pending", false);
        }

            // fetch user with the given id with related merchant object
        BackendlessUser user = mBackendOps.fetchUser(userId, DbConstants.USER_TYPE_MERCHANT);
        if(user==null) {
            //return mBackendOps.mLastOpStatus;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        // check admin status
        String status = CommonUtils.checkMerchantStatus(merchant);
        if(status != null) {
            //return status;
            CommonUtils.throwException(mLogger,status, "Merchant account not active", false);
        }

        // Check if from trusted device
        // don't check for first time after merchant is registered
        if(merchant.getAdmin_status() != DbConstants.USER_STATUS_NEW_REGISTERED) {
            boolean matched = false;
            List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
            if(trustedDevices != null &&
                    (deviceId != null && !deviceId.isEmpty())) {
                for (MerchantDevice device : trustedDevices) {
                    if(device.getDevice_id().equals(deviceId)) {
                        matched = true;
                        break;
                    }
                }
            } else {
                // this scenario should never encountered
                //TODO: raise alarm
            }
            if(!matched) {
                //return BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE;
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "This is not trusted device", false);
            }
        }

        // check for 'extra verification'
        String name = merchant.getName();
        if(name==null || !name.equalsIgnoreCase(brandName)) {

            if( CommonUtils.handleMerchantWrongAttempt(mBackendOps, merchant, DbConstants.ATTEMPT_TYPE_PASSWORD_RESET) ) {

                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD,
                        "Merchant wrong 'password reset' attempt limit reached"+merchant.getAuto_id(), false);
            } else {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED,
                        "Merchant password reset verification failed"+merchant.getAuto_id(), false);
            }
        }

        if(merchant.getAdmin_status() == DbConstants.USER_STATUS_NEW_REGISTERED) {
            String error = handlePasswdResetImmediate(user, merchant);
            if( error != null) {
                mLogger.error("Failed to process merchant reset password operation: "+merchant.getAuto_id()+", "+error);
                //return error;
                CommonUtils.throwException(mLogger,error, "Error in handlePasswdResetImmediate", false);
            } else {
                mLogger.debug("Processed passwd reset op for: "+merchant.getAuto_id());
            }
        } else {
            // create row in MerchantOps table
            MerchantOps op = new MerchantOps();
            op.setMerchant_id(merchant.getAuto_id());
            op.setMobile_num(merchant.getMobile_num());
            op.setOp_code(DbConstants.MERCHANT_OP_RESET_PASSWD);
            op.setOp_status(DbConstants.MERCHANT_OP_STATUS_PENDING);

            if( mBackendOps.addMerchantOp(op) == null )
            {
                //return mBackendOps.mLastOpStatus;
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            } else {
                mLogger.debug("Processed passwd reset op for: "+merchant.getAuto_id());
                //return BackendResponseCodes.BE_RESPONSE_OP_SCHEDULED;
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_RESPONSE_OP_SCHEDULED, "Merchant reset scheduled", true);
            }
        }

        Backendless.Logging.flush();
        //return BackendResponseCodes.BE_RESPONSE_NO_ERROR;
    }

    public void sendMerchantId(String mobileNum) {
        initCommon();
        mLogger.debug("In sendMerchantId: "+mobileNum);

        // not required, as supposed to be called by user without logging in (forget password case)
        // this will ensure that backend operations are executed, as logged-in user who called this api using generated SDK
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // fetch user with the registered mobile number
        Merchants merchant = mBackendOps.getMerchantByMobile(mobileNum);
        if(merchant==null) {
            //return mBackendOps.mLastOpStatus;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }

        // check admin status
        String status = CommonUtils.checkMerchantStatus(merchant);
        if(status != null) {
            //return status;
            CommonUtils.throwException(mLogger,status, "Merchant account not active", false);
        }

        // Not checking for trusted device

        // check for 'extra verification'
        String mobile = merchant.getMobile_num();
        if(mobile==null || !mobile.equalsIgnoreCase(mobileNum)) {

            // to keep it simple - not imposing any maximum limit on retries, unlike 'forgot password'
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED,
                    "Merchant forgot user id verification failed: "+mobileNum, false);
        }

        // send merchant id by SMS
        String smsText = buildUserIdSMS(merchant.getAuto_id());
        if( !SmsHelper.sendSMS(smsText, merchant.getMobile_num()) )
        {
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED,
                    "Merchant forgot user id send SMS failed: "+mobileNum, false);
        }
    }

    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.MerchantServicesNoLogin");
        mBackendOps = new BackendOps(mLogger);
        CommonUtils.initTableToClassMappings();
    }

    private String handlePasswdResetImmediate(BackendlessUser user, Merchants merchant) {

        // generate password
        String passwd = CommonUtils.generateTempPassword();
        mLogger.debug("Merchant Password: "+passwd);

        // update user account for the password
        user.setPassword(passwd);
        // delete any row, if exists, of earlier wrong attempts
        /*
        WrongAttempts attempt = mBackendOps.fetchWrongAttempts(merchant.getAuto_id(), DbConstants.ATTEMPT_TYPE_PASSWORD_RESET);
        if(attempt!=null) {
            mBackendOps.deleteWrongAttempt(attempt);
        }*/

        user = mBackendOps.updateUser(user);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }
        mLogger.debug("Updated merchant for password reset: "+merchant.getAuto_id());

        // Send SMS through HTTP
        String smsText = buildFirstPwdResetSMS(merchant.getAuto_id(), passwd);
        if( !SmsHelper.sendSMS(smsText, merchant.getMobile_num()) )
        {
            return BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED;
        }
        mLogger.debug("Sent first password reset SMS: "+merchant.getAuto_id());

        return null;
    }

    private String buildPwdWhereClause(String merchantId) {
        StringBuilder whereClause = new StringBuilder();

        // Single password reset request allowed in every 2 hours

        // for particular merchant
        whereClause.append("op_code = '").append(DbConstants.MERCHANT_OP_RESET_PASSWD).append("'");
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
