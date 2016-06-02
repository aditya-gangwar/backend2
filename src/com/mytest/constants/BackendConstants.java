package com.mytest.constants;

/**
 * This class defines constants that are only relevant for backend code
 * and not for the user apps.
 */
public class BackendConstants {

    // Cashback table pool values
    public static final int CASHBACK_TABLE_POOL_START = 0;
    public static final int CASHBACK_TABLE_POOL_SIZE = 1;

    public static final int PASSWORD_LEN = 5;
    public static final int CUSTOMER_PRIVATE_ID_LEN = 4;

    // used in generating temporary passwords
    public final static char[] pwdChars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    public final static char[] pinAndOtpChars = "0123456789".toCharArray();
    // used in generating random transaction ids, passwords and PINs
    public final static char[] txnChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();


    public static int SEND_TXN_SMS_MIN_AMOUNT = 10;
    public static String APP_ID = "09667F8B-98A7-E6B9-FFEB-B2B6EE831A00";
    public static String SECRET_KEY = "95971CBD-BADD-C61D-FF32-559664AE4F00";

    public static int LOG_POLICY_NUM_MSGS = 1;
    public static int LOG_POLICY_FREQ_SECS = 0;

}
