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
    // Corresponding to 'otp_valid_mins' in GlobalSettings table
    public static final int OTP_VALID_MINS = 30;

    // Corresponding to 'merchant_wrong_password_attempts'  in GlobalSettings table
    public static int MERCHANT_WRONG_ATTEMPT_LIMIT = 5;
    public static int CUSTOMER_WRONG_ATTEMPT_LIMIT = 5;
    public static int INTERNAL_USER_WRONG_ATTEMPT_LIMIT = 5;

    // Keep it 2-5 minutes less than what is configured in GlobalSettings
    // to allow for the time between 'message request submitted' and 'message actually delivered'
    public static int MERCHANT_PASSWORD_RESET_COOL_OFF_MINS = 5;

    // Single password reset request allowed in 2 hours
    public static int MERCHANT_PASSWORD_RESET_REQUEST_GAP_MINS = 120;

    // Corresponding 'customer_account_block_hrs' in GlobalSettings table
    public static int CUSTOMER_ACCOUNT_BLOCKED_HOURS = 24;

    // Corresponding 'merchant_account_block_hrs' in GlobalSettings table
    public static int MERCHANT_ACCOUNT_BLOCKED_HOURS = 1;

    // Corresponding 'cash_account_max_limit' in GlobalSettings table
    public static int CUSTOMER_CASH_MAX_LIMIT = 500;

    // Corresponding 'cl_credit_limit_for_pin' in GlobalSettings table
    public static int CL_CREDIT_LIMIT_FOR_PIN = 0;
    // Corresponding 'cl_debit_limit_for_pin' in GlobalSettings table
    public static int CL_DEBIT_LIMIT_FOR_PIN = 0;
    // Corresponding 'cb_debit_limit_for_pin' in GlobalSettings table
    public static int CB_DEBIT_LIMIT_FOR_PIN = 0;

    // Corresponding to 'mchnt_stats_no_refresh_hours' in GlobalSettings table
    public static int MCHNT_STATS_NO_REFRESH_HOURS = 1;
}
