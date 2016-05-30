package com.mytest.database;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class DbConstants {
    // Users table - 'user_type' column values
    public static final int USER_TYPE_MERCHANT = 0;
    public static final int USER_TYPE_CUSTOMER = 1;

    // Merchant table - 'admin_status' column values
    public static final int USER_STATUS_DEFAULT = 0;
    public static final int USER_STATUS_NEW_REGISTERED = 1;
    public static final int USER_STATUS_ACTIVE = 2;
    public static final int USER_STATUS_DISABLED = 3;
    public static final int USER_STATUS_DISABLED_WRONG_PIN = 4;

    // CustomerCards table - 'status' column values
    public static final int CUSTOMER_CARD_STATUS_NEW = 0;
    public static final int CUSTOMER_CARD_STATUS_WITH_MERCHANT = 1;
    public static final int CUSTOMER_CARD_STATUS_ALLOTTED = 2;
    public static final int CUSTOMER_CARD_STATUS_REMOVED = 3;

    // CustomerOps table - 'opcode' column values
    public static final String CUSTOMER_OP_NEW_CARD = "New Card";
    public static final String CUSTOMER_OP_RESET_PIN = "Reset PIN";
    public static final String CUSTOMER_OP_CHANGE_MOBILE = "Change Mobile";

    // CustomerOps table - 'status' column values
    public static final String CUSTOMER_OP_STATUS_OTP_GENERATED = "statusOtpGenerated";

    // MerchantOps table - 'status' column values
    public static final String MERCHANT_OP_STATUS_PENDING = "pending";
    public static final String MERCHANT_OP_STATUS_LOCKED = "locked";
    public static final String MERCHANT_OP_STATUS_COMPLETE = "complete";
    // MerchantOps table - 'opcode' column values
    public static final String MERCHANT_OP_RESET_PASSWD = "reset_password";

    // AllOtp table - 'opcode' column values - apart from ones from 'MerchantOps' and 'CustomerOps' tables
    public static final String MERCHANT_OP_NEW_DEVICE_LOGIN = "new_device_login";

    // Counters table - 'name' column values
    public static final String CUSTOMER_ID_COUNTER = "customer_id";
    public static final String MERCHANT_ID_COUNTER = "merchant_id";

    // GlobalSettings table - should be exactly same as 'names' in DB
    public static final String merchant_passwd_reset_mins = "merchant_passwd_reset_mins";
    public static final String merchant_wrong_password_attempts = "merchant_wrong_password_attempts";
    public static final String otp_valid_mins = "otp_valid_mins";
    public static final String cl_limit_for_pin_card = "cl_limit_for_pin_card";
    public static final String cb_redeem_limit = "cb_redeem_limit";
    public static final String cb_limit_for_pin_card = "cb_limit_for_pin_card";
    public static final String reports_history_days = "reports_history_days";
    public static final String reports_blackout_end = "reports_blackout_end";
    public static final String reports_blackout_start = "reports_blackout_start";
    public static final String service_disabled_until = "service_disabled_until";
    public static final String customer_account_block_hrs = "customer_account_block_hrs";
    public static final String customer_wrong_pin_attempts = "customer_wrong_pin_attempts";

    // GlobalSettings table - 'value_datatype' column values
    public static final int DATATYPE_INT = 0;
    public static final int DATATYPE_STRING = 1;
    public static final int DATATYPE_DATE = 2;

    // Table names
    public static String CASHBACK_TABLE_NAME = "Cashback";
    public static String TRANSACTION_TABLE_NAME = "Transaction";

}
