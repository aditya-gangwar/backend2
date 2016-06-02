package com.mytest.constants;

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
    public static int MERCHANT_WRONG_ATTEMPT_LIMIT = 4;
    public static int CUSTOMER_WRONG_ATTEMPT_LIMIT = 4;

    // Keep it 2-5 minutes less than what is configured in GlobalSettings
    // to allow for the time between 'message request submitted' and 'message actually delivered'
    public static int MERCHANT_PASSWORD_RESET_COOL_OFF_MINS = 55;

    // Corresponding 'customer_account_block_hrs' in GlobalSettings table
    public static int CUSTOMER_ACCOUNT_BLOCKED_HOURS = 24;

    // Corresponding 'merchant_account_block_hrs' in GlobalSettings table
    public static int MERCHANT_ACCOUNT_BLOCKED_HOURS = 2;
}
