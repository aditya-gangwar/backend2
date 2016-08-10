package com.mytest.utilities;

import com.backendless.Backendless;
import com.backendless.exceptions.BackendlessException;
import com.mytest.constants.*;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by adgangwa on 22-05-2016.
 */
public class CommonUtils {

    private static SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

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

    public static String generateTempPassword() {
        // random alphanumeric string
        Random random = new Random();
        char[] id = new char[BackendConstants.PASSWORD_LEN];
        for (int i = 0; i < BackendConstants.PASSWORD_LEN; i++) {
            id[i] = BackendConstants.pwdChars[random.nextInt(BackendConstants.pwdChars.length)];
        }
        return new String(id);
    }

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
    }*/

    public static String generateMerchantId(MerchantIdBatches batch, String countryCode, long regCounter) {
        // 8 digit merchant id format:
        // <1-3 digit country code> + <0-2 digit range id> + <2 digit batch id> + <3 digit s.no.>
        int serialNo = (int) (regCounter % BackendConstants.MERCHANT_ID_MAX_SNO_PER_BATCH);
        return countryCode+batch.getRangeId()+batch.getBatchId()+String.valueOf(serialNo);
    }

        public static String generateCustomerPIN() {
        // random numeric string
        Random random = new Random();
        char[] id = new char[CommonConstants.PIN_OTP_LEN];
        for (int i = 0; i < CommonConstants.PIN_OTP_LEN; i++) {
            id[i] = BackendConstants.pinAndOtpChars[random.nextInt(BackendConstants.pinAndOtpChars.length)];
        }
        return new String(id);
    }

    public static String generateOTP() {
        // random numeric string
        Random random = new Random();
        char[] id = new char[CommonConstants.PIN_OTP_LEN];
        for (int i = 0; i < CommonConstants.PIN_OTP_LEN; i++) {
            id[i] = BackendConstants.pinAndOtpChars[random.nextInt(BackendConstants.pinAndOtpChars.length)];
        }
        return new String(id);
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
    }

    public static void checkMerchantStatus(Merchants merchant) {
        String errorCode = null;
        String errorMsg = null;

        switch (merchant.getAdmin_status()) {
            case DbConstants.USER_STATUS_DISABLED:
                errorCode = BackendResponseCodes.BE_ERROR_ACC_DISABLED;
                errorMsg = "Account is not active";
                break;

            case DbConstants.USER_STATUS_LOCKED:
                // Check if temporary blocked duration is over
                Date blockedTime = merchant.getStatus_update_time();
                if (blockedTime != null && blockedTime.getTime() > 0) {
                    // check for temp blocking duration expiry
                    Date now = new Date();
                    long timeDiff = now.getTime() - blockedTime.getTime();
                    long allowedDuration = GlobalSettingsConstants.MERCHANT_ACCOUNT_BLOCKED_HOURS * 60 * 60 * 1000;

                    if (timeDiff > allowedDuration) {
                        try {
                            setMerchantStatus(merchant, DbConstants.USER_STATUS_ACTIVE, DbConstants.ENABLED_ACTIVE);
                        } catch (Exception e) {
                            // Failed to auto unlock the account
                            // Ignore for now - and let the operation proceed - but raise alarm for manual correction
                            // TODO: Raise alarm for manual correction
                        }
                    } else {
                        errorCode = BackendResponseCodes.BE_ERROR_ACC_LOCKED;
                        errorMsg = "Account is locked";
                    }
                }
                break;
        }

        if(errorCode != null) {
            throw CommonUtils.getException(errorCode, errorMsg);
        }
    }

    public static void checkCustomerStatus(Customers customer) {
        String errorCode = null;
        String errorMsg = null;
        switch(customer.getAdmin_status()) {
            case DbConstants.USER_STATUS_DISABLED:
                errorCode = BackendResponseCodes.BE_ERROR_ACC_DISABLED;
                errorMsg = "Account is not active";
                break;

            case DbConstants.USER_STATUS_LOCKED:
                // Check if temporary blocked duration is over
                Date blockedTime = customer.getStatus_update_time();
                if (blockedTime != null && blockedTime.getTime() > 0) {
                    // check for temp blocking duration expiry
                    Date now = new Date();
                    long timeDiff = now.getTime() - blockedTime.getTime();
                    long allowedDuration = GlobalSettingsConstants.CUSTOMER_ACCOUNT_BLOCKED_HOURS * 60 * 60 * 1000;

                    if (timeDiff > allowedDuration) {
                        try {
                            setCustomerStatus(customer, DbConstants.USER_STATUS_ACTIVE, DbConstants.ENABLED_ACTIVE);
                        } catch (Exception e) {
                            // Failed to auto unlock the account
                            // Ignore for now - and let the operation proceed - but raise alarm for manual correction
                            // TODO: Raise alarm for manual correction
                        }
                    } else {
                        errorCode = BackendResponseCodes.BE_ERROR_ACC_LOCKED;
                        errorMsg = "Account is locked";
                    }
                }
                break;
        }
        if(errorCode != null) {
            throw CommonUtils.getException(errorCode, errorMsg);
        }
    }

    public static void checkAgentStatus(Agents agent) {
        switch (agent.getAdmin_status()) {
            case DbConstants.USER_STATUS_DISABLED:
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_ACC_DISABLED, "");
            case DbConstants.USER_STATUS_LOCKED:
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_ACC_LOCKED, "");
        }
    }

    public static void checkCardForUse(CustomerCards card) {
        switch(card.getStatus()) {
            /*
            case DbConstants.CUSTOMER_CARD_STATUS_BLOCKED:
                return BackendResponseCodes.BE_ERROR_CARD_BLOCKED;*/

            case DbConstants.CUSTOMER_CARD_STATUS_WITH_MERCHANT:
            case DbConstants.CUSTOMER_CARD_STATUS_REMOVED:
            case DbConstants.CUSTOMER_CARD_STATUS_NEW:
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_WRONG_CARD, "");
        }
    }

    public static void checkCardForAllocation(CustomerCards card) {
        switch(card.getStatus()) {
            case DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED:
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_CARD_INUSE, "");
            /*
            case DbConstants.CUSTOMER_CARD_STATUS_BLOCKED:
                return BackendResponseCodes.BE_ERROR_CARD_BLOCKED;*/

            case DbConstants.CUSTOMER_CARD_STATUS_REMOVED:
            case DbConstants.CUSTOMER_CARD_STATUS_NEW:
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_WRONG_CARD, "");
        }
    }

    public static void handleWrongAttempt(Object userObject, int userType, String attemptType) {
        // find user id based on user type
        String userId = null;
        switch(userType) {
            case DbConstants.USER_TYPE_MERCHANT:
                userId = ((Merchants) userObject).getAuto_id();
                break;
            case DbConstants.USER_TYPE_CUSTOMER:
                userId = ((Customers) userObject).getMobile_num();
                break;
            case DbConstants.USER_TYPE_AGENT:
                userId = ((Agents) userObject).getId();
                break;
        }

        // check if related wrong attempt row already exists
        WrongAttempts attempt = null;
        try {
            attempt = BackendOps.fetchWrongAttempts(userId, attemptType);
        } catch(BackendlessException e) {
            // ignore exception, when data not found
            if(!e.getCode().equals(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND)) {
                throw e;
            }
        }

        if(attempt != null) {
            // related attempt row already available
            // lock customer account - if 'max attempts per day' crossed
            if( attempt.getAttempt_cnt() >= GlobalSettingsConstants.MERCHANT_WRONG_ATTEMPT_LIMIT) {
                // lock merchant account
                try {
                    switch(userType) {
                        case DbConstants.USER_TYPE_MERCHANT:
                            setMerchantStatus((Merchants)userObject, DbConstants.USER_STATUS_LOCKED, DbConstantsBackend.attemptTypeToAccLockedReason.get(attemptType));
                            break;
                        case DbConstants.USER_TYPE_CUSTOMER:
                            setCustomerStatus((Customers) userObject, DbConstants.USER_STATUS_LOCKED, DbConstantsBackend.attemptTypeToAccLockedReason.get(attemptType));
                            break;
                        case DbConstants.USER_TYPE_AGENT:
                            setAgentStatus((Agents) userObject, DbConstants.USER_STATUS_LOCKED, DbConstantsBackend.attemptTypeToAccLockedReason.get(attemptType));
                            break;
                    }
                } catch (Exception e) {
                    // ignore the failure to lock the account
                    // TODO: raise alarm
                }
                // throw max attempt limit reached exception
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD, "");
            }
        } else {
            // related attempt row not available - create the same
            attempt = new WrongAttempts();
            attempt.setUser_id(userId);
            attempt.setAttempt_type(attemptType);
            attempt.setAttempt_cnt(0);
            attempt.setUser_type(userType);
        }

        // increment the cnt and save the object
        attempt.setAttempt_cnt(attempt.getAttempt_cnt()+1);
        try {
            BackendOps.saveWrongAttempt(attempt);
        } catch(Exception e) {
            // ignore exception
            // TODO: raise alarm however
        }
    }

    /*
    public static void throwException(Logger logger, String errorCode, String errorMsg, boolean isNormalResponse) {
        if(isNormalResponse) {
            logger.info("Sending response as exception: "+errorCode+", "+errorMsg);
        } else {
            logger.error("Raising exception: "+errorCode+", "+errorMsg);
        }

        // to be removed once issue is fixed on backendless side
        errorMsg = CommonConstants.PREFIX_ERROR_CODE_AS_MSG + errorCode;
        BackendlessFault fault = new BackendlessFault(errorCode,errorMsg);
        //Backendless.Logging.flush();
        throw new BackendlessException(fault);
    }*/

    public static BackendlessException getException(String errorCode, String errorMsg) {
        // to be removed once issue is fixed on backendless side
        errorMsg = CommonConstants.PREFIX_ERROR_CODE_AS_MSG + errorCode;
        return new BackendlessException(errorCode, errorMsg);
    }

    public static int getUserType(String userdId) {
        switch(userdId.length()) {
            case CommonConstants.MERCHANT_ID_LEN:
                return DbConstants.USER_TYPE_MERCHANT;
            case CommonConstants.AGENT_ID_LEN:
                return DbConstants.USER_TYPE_AGENT;
            default:
                return -1;
        }
    }

    public static boolean customerPinRequired(Merchants merchant, Transaction txn) {
        int cl_credit_threshold = merchant.getCl_credit_limit_for_pin()==null ? GlobalSettingsConstants.CL_CREDIT_LIMIT_FOR_PIN : merchant.getCl_credit_limit_for_pin();
        int cl_debit_threshold = merchant.getCl_debit_limit_for_pin()==null ? GlobalSettingsConstants.CL_DEBIT_LIMIT_FOR_PIN : merchant.getCl_debit_limit_for_pin();
        int cb_debit_threshold = merchant.getCb_debit_limit_for_pin()==null ? GlobalSettingsConstants.CB_DEBIT_LIMIT_FOR_PIN : merchant.getCb_debit_limit_for_pin();

        return (txn.getCl_credit() > cl_credit_threshold
                || txn.getCl_debit() > cl_debit_threshold
                || txn.getCb_debit() > cb_debit_threshold );
    }

    public static boolean isTrustedDevice(String deviceId, Merchants merchant) {
        List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
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

    public static boolean customerIdMobile(String id) {
        return id.length()==CommonConstants.MOBILE_NUM_LENGTH;
    }

    // Dont use this fx. for internal status updates - i.e. ones not relevant for end user.
    // like from 'USER_STATUS_NEW_REGISTERED' -> 'USER_STATUS_ACTIVE'
    public static void setMerchantStatus(Merchants merchant, int status, int reason) {
        // update merchant account
        merchant.setAdmin_remarks("Last status was "+DbConstants.userStatusDesc[merchant.getAdmin_status()]
                + ", and status time was "+mSdfDateWithTime.format(merchant.getStatus_update_time()));
        merchant.setAdmin_status(status);
        merchant.setStatus_reason(reason);
        merchant.setStatus_update_time(new Date());
        BackendOps.updateMerchant(merchant);

        // Generate SMS to inform the same
        String smsText = getAccStatusSmsText(merchant.getAuto_id(), DbConstants.USER_TYPE_MERCHANT, reason);
        if(smsText != null) {
            if( !SmsHelper.sendSMS(smsText, merchant.getMobile_num()) )
            {
                // ignore failure to send SMS
                //TODO: generate alarm
            }
        }
    }

    public static void setCustomerStatus(Customers customer, int status, int reason) {
        // update merchant account
        customer.setAdmin_remarks("Last status was "+DbConstants.userStatusDesc[customer.getAdmin_status()]
                + ", and status time was "+mSdfDateWithTime.format(customer.getStatus_update_time()));
        customer.setAdmin_status(status);
        customer.setStatus_reason(reason);
        customer.setStatus_update_time(new Date());
        BackendOps.updateCustomer(customer);

        // Generate SMS to inform the same
        String smsText = getAccStatusSmsText(customer.getMobile_num(), DbConstants.USER_TYPE_CUSTOMER, reason);
        if(smsText != null) {
            if( !SmsHelper.sendSMS(smsText, customer.getMobile_num()) )
            {
                // ignore failure to send SMS
                //TODO: generate alarm
            }
        }
    }

    public static void setAgentStatus(Agents agent, int status, int reason) {
        // update merchant account
        agent.setAdmin_remarks("Last status was "+DbConstants.userStatusDesc[agent.getAdmin_status()]);
        agent.setAdmin_status(status);
        agent.setStatus_reason(reason);
        BackendOps.updateAgent(agent);

        // Generate SMS to inform the same
        String smsText = getAccStatusSmsText(agent.getMobile_num(), DbConstants.USER_TYPE_AGENT, reason);
        if(smsText != null) {
            if( !SmsHelper.sendSMS(smsText, agent.getMobile_num()) )
            {
                // ignore failure to send SMS
                //TODO: generate alarm
            }
        }
    }

    private static String getAccStatusSmsText(String userId, int userType, int statusReason) {
        int hours = (userType==DbConstants.USER_TYPE_MERCHANT)
                ? GlobalSettingsConstants.MERCHANT_ACCOUNT_BLOCKED_HOURS
                : GlobalSettingsConstants.CUSTOMER_ACCOUNT_BLOCKED_HOURS;

        switch(statusReason) {
            case DbConstants.LOCKED_WRONG_PASSWORD_LIMIT_RCHD:
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PASSWORD, userId, hours);
            case DbConstants.LOCKED_WRONG_PIN_LIMIT_RCHD:
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PIN, userId, hours);
            case DbConstants.LOCKED_FORGOT_PASSWORD_ATTEMPT_LIMIT_RCHD:
                if(userType==DbConstants.USER_TYPE_AGENT) {
                    return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PASSWD_RESET_AGENT, userId);
                }
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PASSWD_RESET, userId, hours);
            case DbConstants.LOCKED_FORGOT_USERID_ATTEMPT_LIMIT_RCHD:
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_FORGOT_USERID, userId, hours);
            default:
                // TODO: Raise alarm
                return null;
        }
    }

    public static void initTableToClassMappings() {
        Backendless.Data.mapTableToClass("CustomerCards", CustomerCards.class);
        Backendless.Data.mapTableToClass("Customers", Customers.class);
        Backendless.Data.mapTableToClass("Merchants", Merchants.class);
        Backendless.Data.mapTableToClass("CustomerOps", CustomerOps.class);
        Backendless.Data.mapTableToClass("AllOtp", AllOtp.class);
        Backendless.Data.mapTableToClass("Counters", Counters.class);
        Backendless.Data.mapTableToClass("MerchantOps", MerchantOps.class);
        Backendless.Data.mapTableToClass("WrongAttempts", WrongAttempts.class);
        Backendless.Data.mapTableToClass("MerchantDevice", MerchantDevice.class);
        Backendless.Data.mapTableToClass("Agents", Agents.class);

        /*
        Backendless.Data.mapTableToClass("MerchantIdBatches1", MerchantIdBatches.class);

        Backendless.Data.mapTableToClass("MerchantIds199", MerchantIds.class);

        Backendless.Data.mapTableToClass( "Transaction0", Transaction.class );
        Backendless.Data.mapTableToClass( "Cashback0", Cashback.class );

        Backendless.Data.mapTableToClass( "Transaction1", Transaction.class );
        Backendless.Data.mapTableToClass( "Cashback1", Cashback.class );
        */
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
                // TODO: raise minor alarm however
                // raise 'verification failed' exception
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED,
                        CommonConstants.PREFIX_ERROR_CODE_AS_MSG+BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED);
            }
        }

        if(attempt != null) {
            // related attempt row already available
            // lock customer account - if 'max attempts per day' crossed
            if( attempt.getAttempt_cnt() >= GlobalSettingsConstants.MERCHANT_WRONG_ATTEMPT_LIMIT) {
                // lock merchant account
                try {
                    setCustomerStatus(customer, DbConstants.USER_STATUS_LOCKED, DbConstants.attemptTypeToAccLockedReason.get(attemptType));
                } catch (Exception e) {
                    // ignore the failure to lock the alarm
                    // TODO: raise alarm
                }
                // throw max attempt limit reached exception
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD,
                        CommonConstants.PREFIX_ERROR_CODE_AS_MSG+BackendResponseCodes.BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD);
            }
        } else {
            // related attempt row not available - create the same
            attempt = new WrongAttempts();
            attempt.setUser_id(customer.getMobile_num());
            attempt.setAttempt_type(attemptType);
            attempt.setAttempt_cnt(0);
            attempt.setUser_type(DbConstants.USER_TYPE_MERCHANT);
        }

        // increment the cnt and save the object
        attempt.setAttempt_cnt(attempt.getAttempt_cnt()+1);
        try {
            BackendOps.saveWrongAttempt(attempt);
        } catch(Exception e) {
            // ignore exception - as we anyways be raising exception
            // TODO: raise minor alarm however
        }

        // raise 'verification failed' exception
        throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED,
                CommonConstants.PREFIX_ERROR_CODE_AS_MSG+BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED);
    }

    // returns true if max attempt limit reached
    public static boolean handleAgentWrongAttempt(Agents agent, String attemptType) {
        // fetch or create related wrong attempt row
        WrongAttempts attempt = BackendOps.fetchOrCreateWrongAttempt(agent.getMobile_num(), attemptType, DbConstants.USER_TYPE_AGENT);
        if(attempt != null) {
            // Lock account, if max wrong attempt limit reached
            if( attempt.getAttempt_cnt() >= GlobalSettingsConstants.MERCHANT_WRONG_ATTEMPT_LIMIT) {
                // lock customer account
                agent.setAdmin_status(DbConstants.USER_STATUS_LOCKED);
                agent.setStatus_reason(getAccLockedReason(attemptType));
                //agent.setStatus_update_time(new Date());
                if( BackendOps.updateAgent(agent)==null ) {
                    //TODO: generate alarm
                }
                // Generate SMS to inform the same
                String smsText = getAccLockSmsText(agent.getMobile_num(), 0, attemptType, DbConstants.USER_TYPE_AGENT);
                if(smsText != null) {
                    if( !SmsHelper.sendSMS(smsText, agent.getMobile_num()) )
                    {
                        //TODO: generate alarm
                    }
                }
                return true;
            }
            // increase attempt count
            attempt.setAttempt_cnt(attempt.getAttempt_cnt()+1);
            if( BackendOps.saveWrongAttempt(attempt) == null ) {
                //TODO: generate alarm
            }
        } else {
            //TODO: generate alarm
        }

        return false;
    }

    private static int getAccLockedReason(String wrongattemptType) {
        switch(wrongattemptType) {
            case DbConstants.ATTEMPT_TYPE_PASSWORD_RESET:
                return DbConstants.LOCKED_WRONG_PASSWORD_RESET_ATTEMPT_LIMIT_RCHD;
            case DbConstants.ATTEMPT_TYPE_USER_LOGIN:
                return DbConstants.LOCKED_WRONG_PASSWORD_LIMIT_RCHD;
            case DbConstants.ATTEMPT_TYPE_USER_PIN:
                return DbConstants.LOCKED_WRONG_PIN_LIMIT_RCHD;
            case DbConstants.ATTEMPT_TYPE_FORGOT_USERID:
                return DbConstants.LOCKED_FORGOT_USERID_ATTEMPT_LIMIT_RCHD;
        }
        return 100;
    }*/

