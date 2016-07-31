package com.mytest.utilities;

import com.backendless.Backendless;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.logging.Logger;
import com.mytest.constants.*;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by adgangwa on 22-05-2016.
 */
public class CommonUtils {

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

    public static String checkMerchantStatus(Merchants merchant) {
        switch (merchant.getAdmin_status()) {
            case DbConstants.USER_STATUS_DISABLED:
                return BackendResponseCodes.BE_ERROR_ACC_DISABLED;

            case DbConstants.USER_STATUS_LOCKED:
                // Check if temporary blocked duration is over
                Date blockedTime = merchant.getStatus_update_time();
                if (blockedTime != null && blockedTime.getTime() > 0) {
                    // check for temp blocking duration expiry
                    Date now = new Date();
                    long timeDiff = now.getTime() - blockedTime.getTime();
                    long allowedDuration = GlobalSettingsConstants.MERCHANT_ACCOUNT_BLOCKED_HOURS * 60 * 60 * 1000;

                    if (timeDiff > allowedDuration) {
                        // reset blocked time to null and update the status
                        // do not persist now in DB - will be done, if and when called in afterCreate()
                        // where customer object is any way saved - against the given customer operation
                        merchant.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
                        merchant.setStatus_update_time(new Date());
                    } else {
                        return BackendResponseCodes.BE_ERROR_ACC_LOCKED;
                    }
                }
                break;
        }
        return null;
    }

    public static String checkCustomerStatus(Customers customer) {
        switch(customer.getAdmin_status()) {
            case DbConstants.USER_STATUS_DISABLED:
                return BackendResponseCodes.BE_ERROR_ACC_DISABLED;

            case DbConstants.USER_STATUS_LOCKED:
                // Check if temporary blocked duration is over
                Date blockedTime = customer.getStatus_update_time();
                if (blockedTime != null && blockedTime.getTime() > 0) {
                    // check for temp blocking duration expiry
                    Date now = new Date();
                    long timeDiff = now.getTime() - blockedTime.getTime();
                    long allowedDuration = GlobalSettingsConstants.CUSTOMER_ACCOUNT_BLOCKED_HOURS * 60 * 60 * 1000;

                    if (timeDiff > allowedDuration) {
                        // reset blocked time to null and update the status
                        // do not persist now in DB - will be done, if and when called in afterCreate()
                        // where customer object is any way saved - against the given customer operation
                        customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
                        customer.setStatus_update_time(new Date());
                    } else {
                        return BackendResponseCodes.BE_ERROR_ACC_LOCKED;
                    }
                }
                break;
        }
        return null;
    }

    public static String checkAgentStatus(Agents agent) {
        switch (agent.getAdmin_status()) {
            case DbConstants.USER_STATUS_DISABLED:
                return BackendResponseCodes.BE_ERROR_ACC_DISABLED;

            case DbConstants.USER_STATUS_LOCKED:
                 return BackendResponseCodes.BE_ERROR_ACC_LOCKED;
        }
        return null;
    }

    public static String getCardStatusForUse(CustomerCards card) {
        switch(card.getStatus()) {

            case DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED:
                return null;

            /*
            case DbConstants.CUSTOMER_CARD_STATUS_BLOCKED:
                return BackendResponseCodes.BE_ERROR_CARD_BLOCKED;*/

            case DbConstants.CUSTOMER_CARD_STATUS_WITH_MERCHANT:
            case DbConstants.CUSTOMER_CARD_STATUS_REMOVED:
            case DbConstants.CUSTOMER_CARD_STATUS_NEW:
                return BackendResponseCodes.BE_ERROR_WRONG_CARD;
        }
        return BackendResponseCodes.BE_ERROR_WRONG_CARD;
    }

    public static String getCardStatusForAllocation(CustomerCards card) {
        switch(card.getStatus()) {
            case DbConstants.CUSTOMER_CARD_STATUS_WITH_MERCHANT:
                return null;

            case DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED:
                return BackendResponseCodes.BE_ERROR_CARD_INUSE;

            /*
            case DbConstants.CUSTOMER_CARD_STATUS_BLOCKED:
                return BackendResponseCodes.BE_ERROR_CARD_BLOCKED;*/

            case DbConstants.CUSTOMER_CARD_STATUS_REMOVED:
            case DbConstants.CUSTOMER_CARD_STATUS_NEW:
                return BackendResponseCodes.BE_ERROR_WRONG_CARD;
        }
        return BackendResponseCodes.BE_ERROR_WRONG_CARD;
    }

    // returns true if max attempt limit reached
    public static boolean handleCustomerWrongAttempt(BackendOps backendOps, Customers customer, String attemptType) {
        // fetch or create related wrong attempt row
        WrongAttempts attempt = backendOps.fetchOrCreateWrongAttempt(customer.getMobile_num(), attemptType, DbConstants.USER_TYPE_CUSTOMER);
        if(attempt != null) {
            // Lock account, if max wrong attempt limit reached
            if( attempt.getAttempt_cnt() >= GlobalSettingsConstants.CUSTOMER_WRONG_ATTEMPT_LIMIT) {
                // lock customer account
                customer.setAdmin_status(DbConstants.USER_STATUS_LOCKED);
                customer.setStatus_reason(getAccLockedReason(attemptType));
                customer.setStatus_update_time(new Date());
                if( backendOps.updateCustomer(customer)==null ) {
                    //TODO: generate alarm
                }
                // Generate SMS to inform the same
                String smsText = getAccLockSmsText(customer.getMobile_num(), GlobalSettingsConstants.CUSTOMER_ACCOUNT_BLOCKED_HOURS, attemptType, DbConstants.USER_TYPE_CUSTOMER);
                if(smsText != null) {
                    if( !SmsHelper.sendSMS(smsText, customer.getMobile_num()) )
                    {
                        //TODO: generate alarm
                    }
                }
                return true;
            }
            // increase attempt count
            attempt.setAttempt_cnt(attempt.getAttempt_cnt()+1);
            if( backendOps.saveWrongAttempt(attempt) == null ) {
                //TODO: generate alarm
            }
        } else {
            //TODO: generate alarm
        }

        return false;
    }

    // returns true if max attempt limit reached
    public static boolean handleMerchantWrongAttempt(BackendOps backendOps, Merchants merchant, String attemptType) {
        // fetch or create related wrong attempt row
        WrongAttempts attempt = backendOps.fetchOrCreateWrongAttempt(merchant.getAuto_id(), attemptType, DbConstants.USER_TYPE_MERCHANT);
        if(attempt != null) {
            // Lock account, if max wrong attempt limit reached
            if( attempt.getAttempt_cnt() >= GlobalSettingsConstants.MERCHANT_WRONG_ATTEMPT_LIMIT) {
                // lock customer account
                merchant.setAdmin_status(DbConstants.USER_STATUS_LOCKED);
                merchant.setStatus_reason(getAccLockedReason(attemptType));
                merchant.setStatus_update_time(new Date());
                if( backendOps.updateMerchant(merchant)==null ) {
                    //TODO: generate alarm
                }
                // Generate SMS to inform the same
                String smsText = getAccLockSmsText(merchant.getAuto_id(), GlobalSettingsConstants.MERCHANT_ACCOUNT_BLOCKED_HOURS, attemptType, DbConstants.USER_TYPE_MERCHANT);
                if(smsText != null) {
                    if( !SmsHelper.sendSMS(smsText, merchant.getMobile_num()) )
                    {
                        //TODO: generate alarm
                    }
                }
                return true;
            }
            // increase attempt count
            attempt.setAttempt_cnt(attempt.getAttempt_cnt()+1);
            if( backendOps.saveWrongAttempt(attempt) == null ) {
                //TODO: generate alarm
            }
        } else {
            //TODO: generate alarm
        }

        return false;
    }

    // returns true if max attempt limit reached
    public static boolean handleAgentWrongAttempt(BackendOps backendOps, Agents agent, String attemptType) {
        // fetch or create related wrong attempt row
        WrongAttempts attempt = backendOps.fetchOrCreateWrongAttempt(agent.getMobile_num(), attemptType, DbConstants.USER_TYPE_AGENT);
        if(attempt != null) {
            // Lock account, if max wrong attempt limit reached
            if( attempt.getAttempt_cnt() >= GlobalSettingsConstants.MERCHANT_WRONG_ATTEMPT_LIMIT) {
                // lock customer account
                agent.setAdmin_status(DbConstants.USER_STATUS_LOCKED);
                agent.setStatus_reason(getAccLockedReason(attemptType));
                //agent.setStatus_update_time(new Date());
                if( backendOps.updateAgent(agent)==null ) {
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
            if( backendOps.saveWrongAttempt(attempt) == null ) {
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
        }
        return 100;
    }

    private static String getAccLockSmsText(String userId, int hours, String wrongattemptType, int userType) {
        switch(wrongattemptType) {
            case DbConstants.ATTEMPT_TYPE_PASSWORD_RESET:
                if(userType==DbConstants.USER_TYPE_AGENT) {
                    return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PASSWD_RESET_AGENT, userId);
                }
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PASSWD_RESET, userId, hours);
            case DbConstants.ATTEMPT_TYPE_USER_LOGIN:
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PASSWORD, userId, hours);
            case DbConstants.ATTEMPT_TYPE_USER_PIN:
                return String.format(SmsConstants.SMS_ACCOUNT_LOCKED_PIN, userId, hours);
        }
        return null;
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

        Backendless.Data.mapTableToClass( "Transaction0", Transaction.class );
        Backendless.Data.mapTableToClass( "Cashback0", Cashback.class );

        Backendless.Data.mapTableToClass( "Transaction1", Transaction.class );
        Backendless.Data.mapTableToClass( "Cashback1", Cashback.class );
    }

    public static void throwException(Logger logger, String errorCode, String errorMsg, boolean isNormalResponse) {
        if(isNormalResponse) {
            logger.info("Sending response as exception: "+errorCode+", "+errorMsg);
        } else {
            logger.error("Raising exception: "+errorCode+", "+errorMsg);
        }

        // to be removed once issue is fixed on backendless side
        errorMsg = "ZZ"+errorCode;
        BackendlessFault fault = new BackendlessFault(errorCode,errorMsg);

        Backendless.Logging.flush();
        throw new BackendlessException(fault);
        //throw new BackendlessException( errorCode, errorMsg);
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


}
