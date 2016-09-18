package com.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.myecash.constants.*;
import com.myecash.database.*;
import com.myecash.messaging.SmsHelper;
import com.myecash.utilities.BackendOps;
import com.myecash.utilities.CommonUtils;
import com.myecash.utilities.MyLogger;

import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 14-07-2016.
 */
public class MerchantServicesNoLogin implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.MerchantServicesNoLogin");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];

    /*
     * Public methods: Backend REST APIs
     */
    public void setDeviceForLogin(String loginId, String deviceInfo, String rcvdOtp) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "setDeviceForLogin";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = loginId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                deviceInfo+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                rcvdOtp;

        try {
            if (deviceInfo == null || deviceInfo.isEmpty()) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA, "");
            }

            mLogger.debug("In setDeviceForLogin: " + loginId + ": " + deviceInfo);
            //mLogger.debug(InvocationContext.asString());
            //mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

            // fetch merchant
            Merchants merchant = BackendOps.getMerchant(loginId, false, false);
            mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();
            mLogger.setProperties(merchant.getAuto_id(), DbConstants.USER_TYPE_MERCHANT, merchant.getDebugLogs());

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

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void resetMerchantPwd(String userId, String deviceId, String brandName) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "resetMerchantPwd";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = userId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                deviceId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                brandName;
        boolean positiveException = false;

        try {
            mLogger.debug("In resetMerchantPwd: " + userId + ": " + deviceId);
            //mLogger.debug("Before: " + InvocationContext.asString());
            //mLogger.debug("Before: " + HeadersManager.getInstance().getHeaders().toString());

            // check if any request already pending
            if( BackendOps.fetchMerchantOps(buildPwdResetWhereClause(userId)) != null) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_DUPLICATE_REQUEST, "");
            }

            // fetch user with the given id with related merchant object
            BackendlessUser user = BackendOps.fetchUser(userId, DbConstants.USER_TYPE_MERCHANT, false);
            int userType = (Integer)user.getProperty("user_type");
            if(userType != DbConstants.USER_TYPE_MERCHANT) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,userId+" is not a merchant.");
            }
            Merchants merchant = (Merchants) user.getProperty("merchant");
            mLogger.setProperties(merchant.getAuto_id(), DbConstants.USER_TYPE_MERCHANT, merchant.getDebugLogs());
            mEdr[BackendConstants.EDR_USER_ID_IDX] = (String)user.getProperty("user_id");
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(userType);
            mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();

            // check admin status
            CommonUtils.checkMerchantStatus(merchant, mLogger);

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
                CommonUtils.handleWrongAttempt(userId, merchant, DbConstants.USER_TYPE_MERCHANT, DbConstantsBackend.ATTEMPT_TYPE_PASSWORD_RESET, mLogger);
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
                op.setInitiatedBy(DbConstantsBackend.MERCHANT_OP_INITBY_MCHNT);
                op.setInitiatedVia(DbConstantsBackend.MERCHANT_OP_INITVIA_APP);

                BackendOps.saveMerchantOp(op);
                mLogger.debug("Processed passwd reset op for: " + merchant.getAuto_id());

                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.BE_RESPONSE_OP_SCHEDULED, "");
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void sendMerchantId(String mobileNum, String deviceId) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "sendMerchantId";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = mobileNum;

        try {
            mLogger.debug("In sendMerchantId: " + mobileNum);

            // fetch user with the registered mobile number
            //Merchants merchant = BackendOps.getMerchantByMobile(mobileNum);
            Merchants merchant = BackendOps.getMerchant(mobileNum, true, false);
            mLogger.setProperties(merchant.getAuto_id(), DbConstants.USER_TYPE_MERCHANT, merchant.getDebugLogs());
            mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();
            // check admin status
            CommonUtils.checkMerchantStatus(merchant, mLogger);

            // Check if from trusted device
            // don't check for first time after merchant is registered
            List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
            if (merchant.getFirst_login_ok()) {
                if (!CommonUtils.isTrustedDevice(deviceId, trustedDevices)) {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "");
                }
            }

            // check for 'extra verification'
            String mobile = merchant.getMobile_num();
            if (mobile == null || !mobile.equalsIgnoreCase(mobileNum)) {
                CommonUtils.handleWrongAttempt(merchant.getAuto_id(), merchant, DbConstants.USER_TYPE_MERCHANT, DbConstantsBackend.ATTEMPT_TYPE_FORGOT_USERID, mLogger);
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
            }

            // send merchant id by SMS
            String smsText = SmsHelper.buildUserIdSMS(merchant.getAuto_id());
            if (SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mLogger)) {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
            } else {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
            };

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
     * Private helper methods
     */
    private void handlePasswdResetImmediate(BackendlessUser user, Merchants merchant) {
        // generate password
        String passwd = CommonUtils.generateTempPassword();
        // update user account for the password
        user.setPassword(passwd);
        BackendOps.updateUser(user);
        //TODO: remove printing passwd
        mLogger.debug("Updated merchant for password reset: "+merchant.getAuto_id()+": "+passwd);

        // Send SMS through HTTP
        String smsText = SmsHelper.buildFirstPwdResetSMS(merchant.getAuto_id(), passwd);
        if( SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mLogger) ){
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
        };
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
}
