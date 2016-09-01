package com.myecash.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adgangwa on 10-08-2016.
 */
public class DbConstantsBackend {

    // Table names - for which class name is not same
    public static String CASHBACK_TABLE_NAME = "Cashback";
    public static String TRANSACTION_TABLE_NAME = "Transaction";
    public static String MERCHANT_ID_BATCH_TABLE_NAME = "MerchantIdBatches";
    public static String CARD_ID_BATCH_TABLE_NAME = "CardIdBatches";

    // MerchantOps table - 'opcode' column values
    public static final String MERCHANT_OP_RESET_PASSWD = "Reset Password";
    public static final String MERCHANT_OP_CHANGE_MOBILE = "Change Mobile";
    // MerchantOps table - 'status' column values
    public static final String MERCHANT_OP_STATUS_PENDING = "pending";
    public static final String MERCHANT_OP_STATUS_LOCKED = "locked";
    public static final String MERCHANT_OP_STATUS_COMPLETE = "complete";

    // Otp table - 'opcode' column values - apart from ones from 'MerchantOps' and 'CustomerOps' tables
    public static final String MERCHANT_OP_NEW_DEVICE_LOGIN = "new_device_login";

    // Counters table - 'name' column values
    public static final String CUSTOMER_ID_COUNTER = "customer_id";
    public static final String MERCHANT_ID_COUNTER = "merchant_id";

    // WrongAttempts table - 'attempt_type' column values
    public static final String ATTEMPT_TYPE_FORGOT_USERID = "forgotUserId";
    public static final String ATTEMPT_TYPE_PASSWORD_RESET = "passwordReset";
    public static final String ATTEMPT_TYPE_USER_LOGIN = "userLogin";
    public static final String ATTEMPT_TYPE_USER_PIN = "userPin";

    public static final Map<String, Integer> attemptTypeToAccLockedReason;
    static {
        Map<String, Integer> aMap = new HashMap<>(10);

        // my own backend response codes
        aMap.put(ATTEMPT_TYPE_FORGOT_USERID, DbConstants.LOCKED_FORGOT_USERID_ATTEMPT_LIMIT_RCHD);
        aMap.put(ATTEMPT_TYPE_PASSWORD_RESET, DbConstants.LOCKED_FORGOT_PASSWORD_ATTEMPT_LIMIT_RCHD);
        aMap.put(ATTEMPT_TYPE_USER_LOGIN, DbConstants.LOCKED_WRONG_PASSWORD_LIMIT_RCHD);
        aMap.put(ATTEMPT_TYPE_USER_PIN, DbConstants.LOCKED_WRONG_PIN_LIMIT_RCHD);

        attemptTypeToAccLockedReason = Collections.unmodifiableMap(aMap);
    }

    // Merchant Id Batch status
    public static final String MERCHANT_ID_BATCH_STATUS_AVAILABLE = "available";
    public static final String MERCHANT_ID_BATCH_STATUS_OPEN = "open";
    public static final String MERCHANT_ID_BATCH_STATUS_CLOSED = "closed";
    // Card Id Batch status
    public static final String CARD_ID_BATCH_STATUS_AVAILABLE = "available";
    public static final String CARD_ID_BATCH_STATUS_OPEN = "open";
    public static final String CARD_ID_BATCH_STATUS_CLOSED = "closed";

}
