package com.mytest.utilities;

/**
 * This class provide constant values against the some of the
 * columns of GlobalSettings table, which are used in backend codes too.
 * This is to avoid fetching GlobalSettings table in backend code each time.
 * This means: These values should be manually kept in sync with those in GlobalSettings table.
 * User apps fetch values from 'GlobalSettings' table,
 * but backend code use below constants for the same.
 */
public class GlobalSettingsConstants {
    public static final int OTP_VALID_MINS = 30;
    // This should be same here, in GlobalSettings table and
    // in 'Backendless' user settings
    public static int MERCHANT_WRONG_PASSWORD_ATTEMPT_LIMIT = 4;
    // Keep it 2-5 minutes less than what is configured in GlobalSettings
    // to allow for the time between 'message request submitted' and 'message actually delivered'
    public static int MERCHANT_PASSWORD_RESET_COOL_OFF_MINS = 5;
}
