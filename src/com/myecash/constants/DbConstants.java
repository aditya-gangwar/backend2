package com.myecash.constants;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class DbConstants {

    // Users table - 'user_type' column values
    public static final int USER_TYPE_MERCHANT = 0;
    public static final int USER_TYPE_CUSTOMER = 1;
    public static final int USER_TYPE_AGENT = 2;
    public static final int USER_TYPE_CC = 3;
    public static final int USER_TYPE_CNT = 4;
    public static final int USER_TYPE_ADMIN = 5;
    // user type code to text description
    public static String userTypeDesc[] = {
            "Merchant",
            "Customer",
            "Agent",
            "CustomerCare",
            "Controller",
            "Admin"
    };

    // Merchant table - 'admin_status' column values
    //public static final int USER_STATUS_NEW_REGISTERED = 1;
    public static final int USER_STATUS_ACTIVE = 1;
    // Disabled means permanent - until further action by the admin to explicitly enable the account
    public static final int USER_STATUS_DISABLED = 2;
    // Locked means temporary - and will be automatically unlocked after defined duration
    public static final int USER_STATUS_LOCKED = 3;
    // Error during registration - to be manually deleted
    public static final int USER_STATUS_REG_ERROR = 4;
    public static final int USER_STATUS_READY_TO_ACTIVE = 5;
    public static final int USER_STATUS_READY_TO_REMOVE = 6;
    // status code to text description
    public static String userStatusDesc[] = {
            "",
            "Active",
            "Disabled",
            "Locked",
            "Not Registered",
            "Ready to Enable",
            "In Expiry Period"
    };

    // CustomerCards table - 'status' column values
    public static final int CUSTOMER_CARD_STATUS_NEW = 0;
    public static final int CUSTOMER_CARD_STATUS_WITH_MERCHANT = 1;
    public static final int CUSTOMER_CARD_STATUS_ALLOTTED = 2;
    //public static final int CUSTOMER_CARD_STATUS_BLOCKED = 3;
    public static final int CUSTOMER_CARD_STATUS_REMOVED = 3;
    // Map int status values to corresponding descriptions
    public static String cardStatusDescriptions[] = {
            "Invalid",
            "Invalid",
            "Invalid",
            "Invalid"
    };


    // CustomerOps table - 'opcode' column values
    public static final String CUSTOMER_OP_RESET_PASSWORD = "Reset Password";
    public static final String CUSTOMER_OP_NEW_CARD = "New Card";
    public static final String CUSTOMER_OP_RESET_PIN = "Reset Pin";
    public static final String CUSTOMER_OP_CHANGE_MOBILE = "Change Mobile";

    // Transactions table values
    public static final String TXN_CUSTOMER_PIN_USED = "Yes";
    public static final String TXN_CUSTOMER_PIN_NOT_USED = "No";

    // GlobalSettings table - should be exactly same as 'names' in DB
    public static final String SETTINGS_MERCHANT_PASSWD_RESET_MINS = "merchant_passwd_reset_mins";
    public static final String SETTINGS_MERCHANT_PASSWD_RESET_REQUEST_GAP = "merchant_passwd_reset_request_gap_mins";
    public static final String SETTINGS_MERCHANT_WRONG_ATTEMPTS = "merchant_wrong_attempts";
    public static final String SETTINGS_MERCHANT_ACCOUNT_BLOCK_HRS = "merchant_account_block_hrs";
    public static final String SETTINGS_DASHBOARD_NO_REFRESH_HRS = "mchnt_stats_no_refresh_hours";

    public static final String SETTINGS_CL_CREDIT_LIMIT_FOR_PIN = "cl_credit_limit_for_pin";
    public static final String SETTINGS_CL_DEBIT_LIMIT_FOR_PIN = "cl_debit_limit_for_pin";
    public static final String SETTINGS_CB_DEBIT_LIMIT_FOR_PIN = "cb_debit_limit_for_pin";
    public static final String SETTINGS_CB_REDEEM_LIMIT = "cb_redeem_limit";
    public static final String SETTINGS_REPORTS_HISTORY_DAYS = "reports_history_days";
    public static final String SETTINGS_REPORTS_BLACKOUT_END = "reports_blackout_end";
    public static final String SETTINGS_REPORTS_BLACKOUT_START = "reports_blackout_start";

    public static final String SETTINGS_CUSTOMER_ACCOUNT_BLOCK_HRS = "customer_account_block_hrs";
    public static final String SETTINGS_CUSTOMER_WRONG_ATTEMPTS = "customer_wrong_attempts";
    public static final String SETTINGS_CUSTOMER_CASH_LIMIT = "cash_account_max_limit";
    public static final String SETTINGS_OTP_VALID_MINS = "otp_valid_mins";
    public static final String SETTINGS_SERVICE_DISABLED_UNTIL = "service_disabled_until";
    public static final String SETTINGS_TXN_IMAGE_CAPTURE_MODE = "txn_image_capture_mode";
    public static final String SETTINGS_CB_REDEEM_CARD_REQ = "cb_redeem_card_req";
    public static final String SETTINGS_ACC_DB_CARD_REQ = "acc_debit_card_req";
    public static final String SETTINGS_MCHNT_REMOVAL_EXPIRY_DAYS = "mchnt_removal_expiry_days";

    // GlobalSettings table - 'txn_image_capture_mode' column values
    public static final int TXN_IMAGE_CAPTURE_ALWAYS = 0;
    // only when 'card is mandatory' based on txn type and amounts
    public static final int TXN_IMAGE_CAPTURE_CARD_REQUIRED = 1;
    public static final int TXN_IMAGE_CAPTURE_NEVER = 2;

    // GlobalSettings table - 'value_datatype' column values
    public static final int DATATYPE_INT = 1;
    public static final int DATATYPE_BOOLEAN = 2;
    public static final int DATATYPE_STRING = 3;
    public static final int DATATYPE_DATE = 4;
}
