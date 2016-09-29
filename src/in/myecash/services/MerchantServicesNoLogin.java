package in.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.CommonUtils;
import in.myecash.utilities.MyLogger;

import java.util.Date;
import java.util.List;

import in.myecash.common.database.*;
import in.myecash.common.constants.*;
import in.myecash.constants.*;

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
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "");
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

    public void resetMerchantPwd(String userId, String deviceId, String dob) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "resetMerchantPwd";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = userId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                deviceId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                dob;
        boolean positiveException = false;

        try {
            mLogger.debug("In resetMerchantPwd: " + userId + ": " + deviceId);
            //mLogger.debug("Before: " + InvocationContext.asString());
            //mLogger.debug("Before: " + HeadersManager.getInstance().getHeaders().toString());

            // check if any request already pending
            if( BackendOps.fetchMerchantOps(mchntPwdResetWhereClause(userId)) != null) {
                throw new BackendlessException(String.valueOf(ErrorCodes.DUPLICATE_ENTRY), "");
            }

            // fetch user with the given id with related merchant object
            BackendlessUser user = BackendOps.fetchUser(userId, DbConstants.USER_TYPE_MERCHANT, false);
            int userType = (Integer)user.getProperty("user_type");
            if(userType != DbConstants.USER_TYPE_MERCHANT) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED),userId+" is not a merchant.");
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
                    throw new BackendlessException(String.valueOf(ErrorCodes.NOT_TRUSTED_DEVICE), "");
                }
            }

            // check for 'extra verification'
            String storedDob = merchant.getDob();
            if (storedDob == null || !storedDob.equalsIgnoreCase(dob)) {
                CommonUtils.handleWrongAttempt(userId, merchant, DbConstants.USER_TYPE_MERCHANT, DbConstantsBackend.ATTEMPT_TYPE_PASSWORD_RESET, mLogger);
                throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
            }

            // For new registered merchant - send the password immediately
            if (!merchant.getFirst_login_ok()) {
                resetMchntPasswdImmediate(user, merchant);
                mLogger.debug("Processed passwd reset op for: " + merchant.getAuto_id());
            } else {
                // create row in MerchantOps table
                MerchantOps op = new MerchantOps();
                op.setMerchant_id(merchant.getAuto_id());
                op.setMobile_num(merchant.getMobile_num());
                op.setOp_code(DbConstantsBackend.MERCHANT_OP_RESET_PASSWD);
                op.setOp_status(DbConstantsBackend.USER_OP_STATUS_PENDING);
                op.setInitiatedBy(DbConstantsBackend.USER_OP_INITBY_MCHNT);
                op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_APP);

                BackendOps.saveMerchantOp(op);
                mLogger.debug("Processed passwd reset op for: " + merchant.getAuto_id());

                positiveException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OP_SCHEDULED), "");
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
                    throw new BackendlessException(String.valueOf(ErrorCodes.NOT_TRUSTED_DEVICE), "");
                }
            }

            // check for 'extra verification'
            String mobile = merchant.getMobile_num();
            if (mobile == null || !mobile.equalsIgnoreCase(mobileNum)) {
                CommonUtils.handleWrongAttempt(merchant.getAuto_id(), merchant, DbConstants.USER_TYPE_MERCHANT, DbConstantsBackend.ATTEMPT_TYPE_FORGOT_USERID, mLogger);
                throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
            }

            // send merchant id by SMS
            String smsText = SmsHelper.buildUserIdSMS(merchant.getAuto_id());
            if (SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mLogger)) {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
            } else {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                throw new BackendlessException(String.valueOf(ErrorCodes.SEND_SMS_FAILED), "");
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
    private void resetMchntPasswdImmediate(BackendlessUser user, Merchants merchant) {
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
            if(mEdr!=null) {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
            }
        } else {
            if(mEdr!=null) {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
            }
            throw new BackendlessException(String.valueOf(ErrorCodes.SEND_SMS_FAILED), "");
        }
        mLogger.debug("Sent first password reset SMS: "+merchant.getAuto_id());
    }

    private String mchntPwdResetWhereClause(String merchantId) {
        StringBuilder whereClause = new StringBuilder();

        whereClause.append("op_code = '").append(DbConstantsBackend.MERCHANT_OP_RESET_PASSWD).append("'");
        whereClause.append(" AND op_status = '").append(DbConstantsBackend.USER_OP_STATUS_PENDING).append("'");
        whereClause.append("AND merchant_id = '").append(merchantId).append("'");

        // created within last 'cool off mins'
        long time = (new Date().getTime()) - (GlobalSettingsConstants.MERCHANT_PASSWORD_RESET_COOL_OFF_MINS * 60 * 1000);
        whereClause.append(" AND created > ").append(time);
        return whereClause.toString();
    }
}
