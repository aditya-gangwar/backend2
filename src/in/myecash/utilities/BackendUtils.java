package in.myecash.utilities;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import in.myecash.common.MyGlobalSettings;
import in.myecash.messaging.SmsConstants;
import in.myecash.messaging.SmsHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import in.myecash.common.database.*;
import in.myecash.common.constants.*;
import in.myecash.constants.*;
import in.myecash.database.*;

/**
 * Created by adgangwa on 22-05-2016.
 */
public class BackendUtils {

    /*
     * Password & ID generators
     */
    public static String generateTempPassword() {
        // random alphanumeric string
        Random random = new Random();
        char[] id = new char[BackendConstants.PASSWORD_LEN];
        for (int i = 0; i < BackendConstants.PASSWORD_LEN; i++) {
            id[i] = BackendConstants.pwdChars[random.nextInt(BackendConstants.pwdChars.length)];
        }
        return new String(id);
    }

    public static String generateMerchantId(MerchantIdBatches batch, String countryCode, long regCounter) {
        // 8 digit merchant id format:
        // <1-3 digit country code> + <0-2 digit range id> + <2 digit batch id> + <3 digit s.no.>
        int serialNo = (int) (regCounter % BackendConstants.MERCHANT_ID_MAX_SNO_PER_BATCH);
        return countryCode+batch.getRangeBatchId()+String.format("%03d",serialNo);
    }

    public static String generateCustomerPIN() {
        // random numeric string
        Random random = new Random();
        char[] id = new char[CommonConstants.PIN_LEN];
        for (int i = 0; i < CommonConstants.PIN_LEN; i++) {
            id[i] = BackendConstants.pinAndOtpChars[random.nextInt(BackendConstants.pinAndOtpChars.length)];
        }
        return new String(id);
    }

    public static String generateOTP() {
        // random numeric string
        Random random = new Random();
        char[] id = new char[CommonConstants.OTP_LEN];
        for (int i = 0; i < CommonConstants.OTP_LEN; i++) {
            id[i] = BackendConstants.pinAndOtpChars[random.nextInt(BackendConstants.pinAndOtpChars.length)];
        }
        return new String(id);
    }

    public static String generateTxnId(String merchantId) {
        // Txn Id : <7 chars for curr time in secs as Base35> + <6 char for merchant id as Base26> = total 13 chars
        long timeSecs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        long mchntIdLong = Long.parseUnsignedLong(merchantId);
        return Base35.fromBase10(timeSecs,7) + Base25.fromBase10(mchntIdLong, 6);
    }

    public static String generateLogId() {
        // random alphanumeric string
        Random random = new Random();
        char[] id = new char[BackendConstants.LOG_ID_LEN];
        for (int i = 0; i < BackendConstants.LOG_ID_LEN; i++) {
            id[i] = BackendConstants.pwdChars[random.nextInt(BackendConstants.pwdChars.length)];
        }
        return new String(id);
    }


    /*
     * Fetches User by given DB 'objectId'
     * If required, compares that uer type is as expected.
     * Checks for fetched user admin status
     * Set appropriate EDR values
     */
    public static Object fetchCurrentUser(String objectId, Integer allowedUserType, String[] edr, MyLogger logger, boolean allChild) {

        BackendlessUser user = BackendOps.fetchUserByObjectId(objectId, allChild);
        edr[BackendConstants.EDR_USER_ID_IDX] = (String) user.getProperty("user_id");
        int userType = (Integer)user.getProperty("user_type");

        edr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(userType);
        // don't check if allowedUserType == null
        if(allowedUserType!=null && allowedUserType!=userType) {
            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
        }

        switch (userType) {
            case DbConstants.USER_TYPE_MERCHANT:
                Merchants merchant = (Merchants) user.getProperty("merchant");
                edr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();
                logger.setProperties(edr[BackendConstants.EDR_USER_ID_IDX], userType, merchant.getDebugLogs());
                // check if merchant is enabled
                BackendUtils.checkMerchantStatus(merchant, edr, logger);
                return merchant;

            case DbConstants.USER_TYPE_CNT:
            case DbConstants.USER_TYPE_CC:
            case DbConstants.USER_TYPE_AGENT:
                InternalUser internalUser = (InternalUser) user.getProperty("internalUser");
                edr[BackendConstants.EDR_INTERNAL_USER_ID_IDX] = internalUser.getId();
                logger.setProperties(edr[BackendConstants.EDR_USER_ID_IDX], userType, internalUser.getDebugLogs());
                // check if agent is enabled
                BackendUtils.checkInternalUserStatus(internalUser);
                return internalUser;

            case DbConstants.USER_TYPE_CUSTOMER:
                Customers customer = (Customers) user.getProperty("customer");
                edr[BackendConstants.EDR_CUST_ID_IDX] = customer.getPrivate_id();
                logger.setProperties(edr[BackendConstants.EDR_USER_ID_IDX], userType, customer.getDebugLogs());
                // check if customer is enabled
                BackendUtils.checkCustomerStatus(customer, edr, logger);
                return customer;
        }

        throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Operation not allowed to this user");
    }


    /*
     * Functions to check for User Admin Status
     */
    public static void checkMerchantStatus(Merchants merchant, String[] edr, MyLogger logger) {
        Integer errorCode = null;
        String errorMsg = null;

        switch (merchant.getAdmin_status()) {
            case DbConstants.USER_STATUS_REG_ERROR:
            case DbConstants.USER_STATUS_DISABLED:
            case DbConstants.USER_STATUS_READY_TO_ACTIVE:
                errorCode = ErrorCodes.USER_ACC_DISABLED;
                errorMsg = "Account is not active";
                break;

            case DbConstants.USER_STATUS_LOCKED:
                // Check if temporary blocked duration is over
                if(!checkMchntStatusExpiry(merchant, MyGlobalSettings.getAccBlockHrs(DbConstants.USER_TYPE_MERCHANT), edr, logger)) {
                    // expiry duration is not over yet
                    errorCode = ErrorCodes.USER_ACC_LOCKED;
                    errorMsg = "Account is locked";
                }
                break;
        }

        if(errorCode != null) {
            throw new BackendlessException(errorCode.toString(), errorMsg);
        }
    }

    public static void checkCustomerStatus(Customers customer, String[] edr, MyLogger logger) {
        Integer errorCode = null;
        String errorMsg = null;
        switch(customer.getAdmin_status()) {
            case DbConstants.USER_STATUS_REG_ERROR:
            case DbConstants.USER_STATUS_DISABLED:
            case DbConstants.USER_STATUS_READY_TO_ACTIVE:
                errorCode = ErrorCodes.USER_ACC_DISABLED;
                errorMsg = "Account is not active";
                break;

            case DbConstants.USER_STATUS_MOB_CHANGE_RECENT:
                // Credit txns and data view is allowed when in this mode
                // Only Debit txns and profile/setting changes (like pin, passwd change etc) are not allowed
                // As this fx. does not have all this info to decide - so it will not raise any exception
                // calling fx. should check it itself for this status
                // However, if 'restricted duration' is passed, the status will be changed to 'Enabled' here only
                checkCustStatusExpiry(customer, MyGlobalSettings.getCustHrsAfterMobChange(), edr, logger);
                break;

            case DbConstants.USER_STATUS_LOCKED:
                // Check if temporary blocked duration is over
                if(!checkCustStatusExpiry(customer, MyGlobalSettings.getAccBlockHrs(DbConstants.USER_TYPE_CUSTOMER), edr, logger)) {
                    // expiry duration is not over yet
                    errorCode = ErrorCodes.USER_ACC_LOCKED;
                    errorMsg = "Account is locked";
                }
                break;
        }
        if(errorCode != null) {
            throw new BackendlessException(errorCode.toString(), errorMsg);
        }
    }

    public static void checkInternalUserStatus(InternalUser agent) {
        switch (agent.getAdmin_status()) {
            case DbConstants.USER_STATUS_DISABLED:
                throw new BackendlessException(String.valueOf(ErrorCodes.USER_ACC_DISABLED), "");
            case DbConstants.USER_STATUS_LOCKED:
                throw new BackendlessException(String.valueOf(ErrorCodes.USER_ACC_LOCKED), "");
        }
    }

    private static boolean checkCustStatusExpiry(Customers customer, int hours, String[] edr, MyLogger logger) {
        Date blockedTime = customer.getStatus_update_time();
        if (blockedTime != null && blockedTime.getTime() > 0) {
            // check for temp blocking duration expiry
            Date now = new Date();
            long timeDiff = now.getTime() - blockedTime.getTime();
            long allowedDuration = hours * 60 * 60 * 1000;

            if (timeDiff > allowedDuration) {
                try {
                    setCustomerStatus(customer, DbConstants.USER_STATUS_ACTIVE, DbConstantsBackend.ENABLED_ACTIVE, edr, logger);
                } catch (Exception e) {
                    // Failed to auto unlock the account
                    // Ignore for now - and let the operation proceed - but raise alarm for manual correction
                    edr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    edr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.IGNORED_ERROR_ACC_STATUS_CHANGE_FAILED;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private static boolean checkMchntStatusExpiry(Merchants merchant, int hours, String[] edr, MyLogger logger) {
        Date blockedTime = merchant.getStatus_update_time();
        if (blockedTime != null && blockedTime.getTime() > 0) {
            // check for temp blocking duration expiry
            Date now = new Date();
            long timeDiff = now.getTime() - blockedTime.getTime();
            long allowedDuration = hours * 60 * 60 * 1000;

            if (timeDiff > allowedDuration) {
                try {
                    setMerchantStatus(merchant, DbConstants.USER_STATUS_ACTIVE, DbConstantsBackend.ENABLED_ACTIVE, edr, logger);
                } catch (Exception e) {
                    // Failed to auto unlock the account
                    // Ignore for now - and let the operation proceed - but raise alarm for manual correction
                    edr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    edr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.IGNORED_ERROR_ACC_STATUS_CHANGE_FAILED;
                }
            } else {
                return false;
            }
        }
        return true;
    }


    /*
     * Functions to check Membership Card status
     */
    public static void checkCardForUse(CustomerCards card) {
        switch(card.getStatus()) {
            case DbConstants.CUSTOMER_CARD_STATUS_WITH_MERCHANT:
            case DbConstants.CUSTOMER_CARD_STATUS_REMOVED:
            case DbConstants.CUSTOMER_CARD_STATUS_NEW:
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_CARD), "");
        }
    }

    public static void checkCardForAllocation(CustomerCards card) {
        switch(card.getStatus()) {
            case DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED:
                throw new BackendlessException(String.valueOf(ErrorCodes.CARD_ALREADY_IN_USE), "");
            case DbConstants.CUSTOMER_CARD_STATUS_REMOVED:
            case DbConstants.CUSTOMER_CARD_STATUS_NEW:
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_CARD), "");
        }
    }


    /*
     * Handles 'wrong attempt' by any type of user
     */
    public static void handleWrongAttempt(String userId, Object userObject, int userType,
                                          String wrongParamType, String opCode, String[] edr, MyLogger logger) {

        // check similar wrong attempts today
        int cnt = BackendOps.getTodayWrongAttemptCnt(userId, wrongParamType);
        int confMaxAttempts = MyGlobalSettings.userTypeToWrongLimit.get(userType);

        if(cnt > confMaxAttempts) {
            // lock user account - if 'max attempts per day' crossed
            try {
                switch(userType) {
                    case DbConstants.USER_TYPE_MERCHANT:
                        setMerchantStatus((Merchants)userObject, DbConstants.USER_STATUS_LOCKED,
                                DbConstantsBackend.paramTypeToAccLockedReason.get(wrongParamType), edr, logger);
                        break;
                    case DbConstants.USER_TYPE_CUSTOMER:
                        setCustomerStatus((Customers) userObject, DbConstants.USER_STATUS_LOCKED,
                                DbConstantsBackend.paramTypeToAccLockedReason.get(wrongParamType), edr, logger);
                        break;
                    case DbConstants.USER_TYPE_CC:
                    case DbConstants.USER_TYPE_AGENT:
                    case DbConstants.USER_TYPE_CNT:
                        setAgentStatus((InternalUser) userObject, DbConstants.USER_STATUS_LOCKED,
                                DbConstantsBackend.paramTypeToAccLockedReason.get(wrongParamType), edr, logger);
                        break;
                }
            } catch (Exception e) {
                // ignore the failure to lock the account
                logger.error("Exception in handleWrongAttempt: "+e.toString());
                logger.error(stackTraceStr(e));
                edr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.IGNORED_ERROR_ACC_STATUS_CHANGE_FAILED;
            }
            // throw max attempt limit reached exception
            throw new BackendlessException(String.valueOf(ErrorCodes.FAILED_ATTEMPT_LIMIT_RCHD), "");

        } else {
            // limit not crossed yet - add row for this occurance
            WrongAttempts attempt = new WrongAttempts();
            attempt.setUser_id(userId);
            attempt.setParam_type(wrongParamType);
            attempt.setOpCode(opCode);
            //attempt.setAttempt_cnt(0);
            attempt.setUser_type(userType);
            try {
                BackendOps.saveWrongAttempt(attempt);
            } catch(Exception e) {
                // ignore exception
                logger.error("Exception in handleWrongAttempt: "+e.toString());
                logger.error(stackTraceStr(e));
                edr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.IGNORED_ERROR_WRONG_ATTEMPT_SAVE_FAILED;
            }
        }
    }


    /*
     * Sets user admin status to given value, and saves in DB
     * Sends SMS only if given stats is DISABLED or LOCKED
     */
    public static void setMerchantStatus(Merchants merchant, int status, String reason, String[] edr, MyLogger logger) {
        if(status == merchant.getAdmin_status()) {
            return;
        }
        // update merchant account
        merchant.setAdmin_status(status);
        merchant.setStatus_reason(reason);
        merchant.setStatus_update_time(new Date());
        merchant = BackendOps.updateMerchant(merchant);

        // Generate SMS to inform the same - only when acc is locked or disabled
        String smsText = null;
        if(status==DbConstants.USER_STATUS_LOCKED) {
            smsText = getAccLockedSmsText(merchant.getAuto_id(), DbConstants.USER_TYPE_MERCHANT, reason);
        } else if(status==DbConstants.USER_STATUS_DISABLED) {
            smsText = String.format(SmsConstants.SMS_ACCOUNT_DISABLE, getHalfVisibleId(merchant.getAuto_id()));
        }
        SmsHelper.sendSMS(smsText, merchant.getMobile_num(), edr, logger);
    }

    public static void setCustomerStatus(Customers customer, int status, String reason, String[] edr, MyLogger logger) {
        if(status == customer.getAdmin_status()) {
            return;
        }
        // update customer account
        customer.setAdmin_status(status);
        customer.setStatus_reason(reason);
        customer.setStatus_update_time(new Date());
        customer = BackendOps.updateCustomer(customer);

        // Generate SMS to inform the same - only when acc is locked or disabled
        String smsText = null;
        if(status==DbConstants.USER_STATUS_LOCKED) {
            smsText = getAccLockedSmsText(customer.getMobile_num(), DbConstants.USER_TYPE_MERCHANT, reason);
        } else if(status==DbConstants.USER_STATUS_DISABLED) {
            smsText = String.format(SmsConstants.SMS_ACCOUNT_DISABLE, getHalfVisibleId(customer.getMobile_num()));
        }
        SmsHelper.sendSMS(smsText, customer.getMobile_num(), edr, logger);
    }

    public static void setAgentStatus(InternalUser agent, int status, String reason, String[] edr, MyLogger logger) {
        if(status == agent.getAdmin_status()) {
            return;
        }
        // update account
        agent.setAdmin_remarks("Last status was "+DbConstants.userStatusDesc[agent.getAdmin_status()]);
        agent.setAdmin_status(status);
        agent.setStatus_reason(reason);
        agent = BackendOps.updateInternalUser(agent);

        // Generate SMS to inform the same - only when acc is locked or disabled
        String smsText = null;
        if(status==DbConstants.USER_STATUS_LOCKED) {
            smsText = getAccLockedSmsText(agent.getId(), DbConstants.USER_TYPE_MERCHANT, reason);
        } else if(status==DbConstants.USER_STATUS_DISABLED) {
            smsText = String.format(SmsConstants.SMS_ACCOUNT_DISABLE, getHalfVisibleId(agent.getId()));
        }
        SmsHelper.sendSMS(smsText, agent.getMobile_num(), edr, logger);
    }

    private static String getAccLockedSmsText(String userId, int userType, String statusReason) {

        switch(statusReason) {
            case DbConstantsBackend.LOCKED_WRONG_PASSWORD_LIMIT_RCHD:
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PASSWORD, getHalfVisibleId(userId), MyGlobalSettings.getAccBlockHrs(userType));
            case DbConstantsBackend.LOCKED_WRONG_PIN_LIMIT_RCHD:
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PIN, getHalfVisibleId(userId), MyGlobalSettings.getAccBlockHrs(userType));
            case DbConstantsBackend.LOCKED_WRONG_VERIFICATION_LIMIT_RCHD:
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_VERIFY_FAILED, getHalfVisibleId(userId), MyGlobalSettings.getAccBlockHrs(userType));
        }

        return null;
    }


    /*
     * Get User ID type - depending upon the length
     */
    public static int getCustomerIdType(String id) {
        switch (id.length()) {
            case CommonConstants.MOBILE_NUM_LENGTH:
                return BackendConstants.ID_TYPE_MOBILE;
            case CommonConstants.CUSTOMER_CARDID_LEN:
                return BackendConstants.ID_TYPE_CARD;
            case CommonConstants.CUSTOMER_INTERNAL_ID_LEN:
                return BackendConstants.ID_TYPE_AUTO;
            default:
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Invalid customer ID: "+id);
        }
    }

    public static int getMerchantIdType(String id) {
        switch (id.length()) {
            case CommonConstants.MOBILE_NUM_LENGTH:
                return BackendConstants.ID_TYPE_MOBILE;
            case CommonConstants.MERCHANT_ID_LEN:
                return BackendConstants.ID_TYPE_AUTO;
            default:
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Invalid Merchant ID: "+id);
        }
    }

    /*
     * Get User type - depending upon the length of ID
     */
    public static int getUserType(String userdId) {
        switch(userdId.length()) {
            case CommonConstants.MERCHANT_ID_LEN:
                return DbConstants.USER_TYPE_MERCHANT;
            case CommonConstants.INTERNAL_USER_ID_LEN:
                if(userdId.startsWith(BackendConstants.PREFIX_AGENT_ID)) {
                    return DbConstants.USER_TYPE_AGENT;
                } else if(userdId.startsWith(BackendConstants.PREFIX_CC_ID)) {
                    return DbConstants.USER_TYPE_CC;
                } else if(userdId.startsWith(BackendConstants.PREFIX_CCNT_ID)) {
                    return DbConstants.USER_TYPE_CNT;
                } else {
                    throw new BackendlessException(String.valueOf(ErrorCodes.USER_WRONG_ID_PASSWD),"Invalid user type for id: "+userdId);
                }
            case CommonConstants.CUSTOMER_INTERNAL_ID_LEN:
            case CommonConstants.MOBILE_NUM_LENGTH:
                return DbConstants.USER_TYPE_CUSTOMER;
            default:
                throw new BackendlessException(String.valueOf(ErrorCodes.USER_WRONG_ID_PASSWD),"Invalid user type for id: "+userdId);
        }
    }


    /*
     * Checks if for particular transaction - Card/PIN is required
     * The logic in these fxs. should match that in App
     */
    /*public static boolean customerPinRequired(Merchants merchant, Transaction txn) {
        int cl_credit_threshold = (merchant.getCl_credit_limit_for_pin()<0) ? MyGlobalSettings.CL_CREDIT_LIMIT_FOR_PIN : merchant.getCl_credit_limit_for_pin();
        int cl_debit_threshold = (merchant.getCl_debit_limit_for_pin()<0) ? MyGlobalSettings.CL_DEBIT_LIMIT_FOR_PIN : merchant.getCl_debit_limit_for_pin();
        int cb_debit_threshold = (merchant.getCb_debit_limit_for_pin()<0) ? MyGlobalSettings.CB_DEBIT_LIMIT_FOR_PIN : merchant.getCb_debit_limit_for_pin();

        int higher_debit_threshold = Math.max(cl_debit_threshold, cb_debit_threshold);

        return (txn.getCl_credit() > cl_credit_threshold ||
                txn.getCl_debit() > cl_debit_threshold ||
                txn.getCb_debit() > cb_debit_threshold ||
                (txn.getCl_debit()+txn.getCb_debit()) > higher_debit_threshold);
    }*/

    public static boolean customerCardRequired(Transaction txn) {
        return ( (txn.getCl_debit()>0 && MyGlobalSettings.getCardReqAccDebit()) ||
                (txn.getCb_debit()>0 && MyGlobalSettings.getCardReqCbRedeem()) ||
        txn.getCancelTime()!=null );
    }

    /*
     * Functions to handle exceptions
     */
    public static BackendlessException getNewException(BackendlessException be) {
        // to be removed once issue is fixed on backendless side
        // currently for 'custom error code' getCode() always returns 0 - from event handlers
        return new BackendlessException(be.getCode(),
                CommonConstants.PREFIX_ERROR_CODE_AS_MSG + be.getCode()+"/"+be.getMessage());
    }

    public static void handleException(Exception e, boolean validException, MyLogger logger, String[] edr) {
        try {
            edr[BackendConstants.EDR_EXP_EXPECTED] = String.valueOf(validException);

            if (validException) {
                edr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            } else {
                edr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_NOK;
                logger.error("Exception in " + edr[BackendConstants.EDR_API_NAME_IDX] + ": " + e.toString());
                logger.error(stackTraceStr(e));
            }

            //edr[BackendConstants.EDR_EXP_MSG_IDX] = e.getMessage().replaceAll(",", BackendConstants.BACKEND_EDR_SUB_DELIMETER);
            edr[BackendConstants.EDR_EXP_MSG_IDX] = e.getMessage();
            if (e instanceof BackendlessException) {
                edr[BackendConstants.EDR_EXP_CODE_IDX] = ((BackendlessException) e).getCode();
            }
        } catch(Exception ex) {
            logger.fatal("Exception in handleException: " + ex.toString());
            logger.fatal(stackTraceStr(ex));
        }
    }

    public static void finalHandling(long startTime, MyLogger logger, String[] edr) {
        try {
            long endTime = System.currentTimeMillis();
            long execTime = endTime - startTime;
            edr[BackendConstants.EDR_END_TIME_IDX] = String.valueOf(endTime);
            edr[BackendConstants.EDR_EXEC_DURATION_IDX] = String.valueOf(execTime);
            logger.debug(edr[BackendConstants.EDR_USER_TYPE_IDX]);
            logger.edr(edr);
            //logger.flush();
        } catch(Exception e) {
            logger.fatal("Exception in finalHandling: " + e.toString());
            logger.fatal(stackTraceStr(e));
        }
    }

    public static String stackTraceStr(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e1 : e.getStackTrace()) {
            sb.append("\t at ").append(e1.toString()).append("\n");
        }
        return sb.toString();
    }

    public static void writeOpNotAllowedEdr(MyLogger logger, String[] mEdr) {
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(System.currentTimeMillis());
        mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_NOK;
        mEdr[BackendConstants.EDR_EXP_CODE_IDX] = String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED);
        mEdr[BackendConstants.EDR_EXP_MSG_IDX] = mEdr[BackendConstants.EDR_API_NAME_IDX]+" not allowed.";
        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
        logger.edr(mEdr);
        //logger.flush();
    }


    /*
     * Other Miscellaneous functions
     */
    public static String getHalfVisibleId(String userId) {
        // build half visible userid : XXXXX91535
        StringBuilder halfVisibleUserid = new StringBuilder();
        int halflen = userId.length() / 2;
        for(int i=0; i<halflen; i++) {
            halfVisibleUserid.append("X");
        }
        halfVisibleUserid.append(userId.substring(halflen));
        return halfVisibleUserid.toString();
    }

    public static boolean isTrustedDevice(String deviceId, List<MerchantDevice> trustedDevices) {
        //List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
        if (trustedDevices != null &&
                (deviceId != null && !deviceId.isEmpty())) {
            for (MerchantDevice device : trustedDevices) {
                if (device.getDevice_id().equals(deviceId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getMchntDpFilename(String mchntId) {
        return "dp_" + String.valueOf(System.currentTimeMillis()) + "_" + mchntId + "."+CommonConstants.PHOTO_FILE_FORMAT;
    }

    public static void initAll() {

        MyGlobalSettings.initSync(MyGlobalSettings.RunMode.backend);

        // Table to class mapping - for backendless
        Backendless.Data.mapTableToClass("CustomerCards", CustomerCards.class);
        Backendless.Data.mapTableToClass("Customers", Customers.class);
        Backendless.Data.mapTableToClass("Merchants", Merchants.class);
        Backendless.Data.mapTableToClass("CustomerOps", CustomerOps.class);
        Backendless.Data.mapTableToClass("AllOtp", AllOtp.class);
        Backendless.Data.mapTableToClass("Counters", Counters.class);
        Backendless.Data.mapTableToClass("MerchantOps", MerchantOps.class);
        Backendless.Data.mapTableToClass("WrongAttempts", WrongAttempts.class);
        Backendless.Data.mapTableToClass("MerchantDevice", MerchantDevice.class);
        Backendless.Data.mapTableToClass("InternalUser", InternalUser.class);
        Backendless.Data.mapTableToClass("BusinessCategories", BusinessCategories.class);
        Backendless.Data.mapTableToClass("Address", Address.class);
        Backendless.Data.mapTableToClass("Cities", Cities.class);

        Backendless.Data.mapTableToClass("MerchantIdBatches1", MerchantIdBatches.class);

        Backendless.Data.mapTableToClass( "Transaction0", Transaction.class );
        Backendless.Data.mapTableToClass( "Cashback0", Cashback.class );

        Backendless.Data.mapTableToClass( "Transaction1", Transaction.class );
        Backendless.Data.mapTableToClass( "Cashback1", Cashback.class );
    }

}


    /*
    public static boolean handleCustomerWrongAttempt(Customers customer, String attemptType) {
        // check if related wrong attempt row already exists
        WrongAttempts attempt = null;
        try {
            attempt = BackendOps.fetchWrongAttempts(customer.getMobile_num(), attemptType);
        } catch(BackendlessException e) {
            if(!e.getCode().equals(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND)) {
                // ignore exception - as we anyways be raising exception
                // raise 'verification failed' exception
                throw new BackendlessException(BackendResponseCodes.VERIFICATION_FAILED,
                        CommonConstants.PREFIX_ERROR_CODE_AS_MSG+BackendResponseCodes.VERIFICATION_FAILED);
            }
        }

        if(attempt != null) {
            // related attempt row already available
            // lock customer account - if 'max attempts per day' crossed
            if( attempt.getAttempt_cnt() >= MyGlobalSettings.MERCHANT_WRONG_ATTEMPT_LIMIT) {
                // lock merchant account
                try {
                    setCustomerStatus(customer, DbConstants.USER_STATUS_LOCKED, DbConstants.paramTypeToAccLockedReason.get(attemptType));
                } catch (Exception e) {
                    // ignore the failure to lock the alarm
                }
                // throw max attempt limit reached exception
                throw new BackendlessException(BackendResponseCodes.FAILED_ATTEMPT_LIMIT_RCHD,
                        CommonConstants.PREFIX_ERROR_CODE_AS_MSG+BackendResponseCodes.FAILED_ATTEMPT_LIMIT_RCHD);
            }
        } else {
            // related attempt row not available - create the same
            attempt = new WrongAttempts();
            attempt.setUser_id(customer.getMobile_num());
            attempt.setParam_type(attemptType);
            attempt.setAttempt_cnt(0);
            attempt.setUser_type(DbConstants.USER_TYPE_MERCHANT);
        }

        // increment the cnt and save the object
        attempt.setAttempt_cnt(attempt.getAttempt_cnt()+1);
        try {
            BackendOps.saveWrongAttempt(attempt);
        } catch(Exception e) {
            // ignore exception - as we anyways be raising exception
        }

        // raise 'verification failed' exception
        throw new BackendlessException(BackendResponseCodes.VERIFICATION_FAILED,
                CommonConstants.PREFIX_ERROR_CODE_AS_MSG+BackendResponseCodes.VERIFICATION_FAILED);
    }

    // returns true if max attempt limit reached
    public static boolean handleAgentWrongAttempt(Agents agent, String attemptType) {
        // fetch or create related wrong attempt row
        WrongAttempts attempt = BackendOps.fetchOrCreateWrongAttempt(agent.getMobile_num(), attemptType, DbConstants.USER_TYPE_AGENT);
        if(attempt != null) {
            // Lock account, if max wrong attempt limit reached
            if( attempt.getAttempt_cnt() >= MyGlobalSettings.MERCHANT_WRONG_ATTEMPT_LIMIT) {
                // lock customer account
                agent.setAdmin_status(DbConstants.USER_STATUS_LOCKED);
                agent.setStatus_reason(getAccLockedReason(attemptType));
                //agent.setStatus_update_time(new Date());
                if( BackendOps.updateInternalUser(agent)==null ) {
                }
                // Generate SMS to inform the same
                String smsText = getAccLockSmsText(agent.getMobile_num(), 0, attemptType, DbConstants.USER_TYPE_AGENT);
                if(smsText != null) {
                    if( !SmsHelper.sendSMS(smsText, agent.getMobile_num()) )
                    {
                    }
                }
                return true;
            }
            // increase attempt count
            attempt.setAttempt_cnt(attempt.getAttempt_cnt()+1);
            if( BackendOps.saveWrongAttempt(attempt) == null ) {
            }
        } else {
        }

        return false;
    }

    private static int getAccLockedReason(String wrongattemptType) {
        switch(wrongattemptType) {
            case DbConstants.ATTEMPT_TYPE_PASSWORD_RESET:
                return DbConstants.LOCKED_WRONG_PASSWORD_RESET_ATTEMPT_LIMIT_RCHD;
            case DbConstants.WRONG_PARAM_TYPE_PASSWD:
                return DbConstants.LOCKED_WRONG_PASSWORD_LIMIT_RCHD;
            case DbConstants.WRONG_PARAM_TYPE_PIN:
                return DbConstants.LOCKED_WRONG_PIN_LIMIT_RCHD;
            case DbConstants.WRONG_PARAM_TYPE_VERIFICATION:
                return DbConstants.LOCKED_FORGOT_USERID_ATTEMPT_LIMIT_RCHD;
        }
        return 100;
    }*/

    /*
    public static String generateMerchantId(long regCounter) {
        // Generate unique merchant id based on merchant reg counter value
        // Id is alphanumeric with first 2 alphabets and then 4 digits

        // 9999 is max for 4 digits
        // not using 10,000 as divisor to avoid using 0000 in user id
        int divisor = 9999;
        int rem = (int)(regCounter%divisor);

        // first alphabet = counter / 26*9999
        // second alphabet = counter / 9999
        // digits = counter % 9999
        StringBuilder sb = new StringBuilder();
        sb.append(CommonConstants.numToChar[(int)(regCounter/(26*divisor))]);
        sb.append(CommonConstants.numToChar[(int) (regCounter/divisor)]);
        if(rem==0) {
            sb.append(divisor);
        } else {
            sb.append(String.format("%04d",rem));
        }

        return sb.toString();
    }

    public static String generateTxnId(String merchantId) {
        char[] id = new char[CommonConstants.TRANSACTION_ID_LEN];
        // unique id is base 32
        // seed = merchant id + curr time in secs
        String timeSecs = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        String seed =  merchantId + timeSecs;
        Random r = new SecureRandom(seed.getBytes());
        for (int i = 0;  i < CommonConstants.TRANSACTION_ID_LEN;  i++) {
            id[i] = BackendConstants.txnChars[r.nextInt(BackendConstants.txnChars.length)];
        }
        return CommonConstants.TRANSACTION_ID_PREFIX + new String(id);

        //String timeSecs = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        // 8 digit merchant id + 10 digit epoch time in secs = 18 digit number
        // unsigned long can store full extent of 19 digit number (i.e. 19 times 9) and some extent of 20 digit number
        // so 18 digit number can be safely stored in long
        // which is then converted into Base35 number - generating 12 digit id
        // Algo to be revisited - when multi-terminal support is added

        long txnIdLong = Long.parseUnsignedLong(merchantId+timeSecs);
        return Base35.fromBase10(txnIdLong, CommonConstants.TRANSACTION_ID_LEN);

        // Txn Id : <7 chars for curr time in secs as Base35> + <6 char for merchant id as Base26> = total 13 chars
        long timeSecs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        long mchntIdLong = Long.parseUnsignedLong(merchantId);
        return Base35.fromBase10(timeSecs,7) + Base25.fromBase10(mchntIdLong, 6);
    }*/

