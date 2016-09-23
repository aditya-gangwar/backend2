package com.myecash.constants;

/**
 * This class provide constant values against the some of the
 * columns of GlobalSettings table, which are used in backend codes too.
 * This is to avoid fetching GlobalSettings table in backend code each time.
 * This means: These values should be manually kept in sync with those in GlobalSettings table.
 * User apps fetch values from 'GlobalSettings' table,
 * but backend code use below constants for the same.
 */
public class GlobalSettingsConstants {

    // TODO: cross verify all settings with that in the DB

    // Corresponding to 'otp_valid_mins' in GlobalSettings table
    public static final int OTP_VALID_MINS = 30;

    // Corresponding to 'merchant_wrong_password_attempts'  in GlobalSettings table
    public static final int MERCHANT_WRONG_ATTEMPT_LIMIT = 5;
    public static final int CUSTOMER_WRONG_ATTEMPT_LIMIT = 5;
    public static final int INTERNAL_USER_WRONG_ATTEMPT_LIMIT = 5;

    // Keep it 2-5 minutes less than what is configured in GlobalSettings
    // to allow for the time between 'message request submitted' and 'message actually delivered'
    public static final int MERCHANT_PASSWORD_RESET_COOL_OFF_MINS = 5;

    // Single password reset request allowed in 2 hours
    public static final int MERCHANT_PASSWORD_RESET_REQUEST_GAP_MINS = 120;

    // Corresponding 'customer_account_block_hrs' in GlobalSettings table
    public static final int CUSTOMER_ACCOUNT_BLOCKED_HOURS = 24;

    // Corresponding 'merchant_account_block_hrs' in GlobalSettings table
    public static final int MERCHANT_ACCOUNT_BLOCKED_HOURS = 1;

    // Corresponding 'cash_account_max_limit' in GlobalSettings table
    public static final int CUSTOMER_CASH_MAX_LIMIT = 500;

    // Corresponding 'cl_credit_limit_for_pin' in GlobalSettings table
    public static final int CL_CREDIT_LIMIT_FOR_PIN = 10;
    // Corresponding 'cl_debit_limit_for_pin' in GlobalSettings table
    public static final int CL_DEBIT_LIMIT_FOR_PIN = 10;
    // Corresponding 'cb_debit_limit_for_pin' in GlobalSettings table
    public static final int CB_DEBIT_LIMIT_FOR_PIN = 50;

    // Corresponding to 'mchnt_stats_no_refresh_hours' in GlobalSettings table
    public static final int MCHNT_STATS_NO_REFRESH_HOURS = 1;

    public static final boolean ACC_DEBIT_CARD_REQ = true;
    public static final boolean CB_REDEEM_CARD_REQ = true;

    // Corresponding to mchnt_removal_expiry_days
    public static final int MCHNT_REMOVAL_EXPIRY_DAYS = 30;
}
