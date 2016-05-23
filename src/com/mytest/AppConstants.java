package com.mytest;

import java.util.Locale;

/**
 * Created by adgangwa on 10-04-2016.
 */
public class AppConstants {

    /*
     * Below ones are taken from merchant app - should always be same there too
     */
    public static final Locale DATE_LOCALE = Locale.ENGLISH;
    // used where only date (without time) is to be shown
    public static final String DATE_FORMAT_ONLY_DATE = "dd MMM, yy";
    // used to specify 'date with no time' to the backend, like in where clause
    public static final String DATE_FORMAT_ONLY_DATE_BACKEND = "dd-MMM-yyyy";
    // used in reports etc where both date and time is to be shown
    public static final String DATE_FORMAT_WITH_TIME = "dd/MM/yyyy HH:mm:ss";
    // date format to be used in filename
    public static final String DATE_FORMAT_ONLY_DATE_FILENAME = "ddMMMyy";

    public static String FILE_PATH_SEPERATOR = "/";
    public static String MERCHANT_ROOT_DIR = "merchants"+AppConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_ROOT_DIR = MERCHANT_ROOT_DIR+"txn_files"+AppConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_FILE_PREFIX = "txns_";

    public static final String CSV_DELIMETER = ",";
    public static final String CSV_FILE_EXT = ".csv";
    public static final String CSV_NEWLINE = "\n";

    public static final int PASSWORD_LEN = 5;
    public static final int PIN_LEN = 5;
    public static final int OTP_LEN = 4;
    public static final int CUSTOMER_PRIVATE_ID_LEN = 4;
    public static final int TRANSACTION_ID_LEN = 10;
    public static int dbQueryMaxPageSize = 100;
    public static final int OTP_VALID_MINS = 15;

    // number to character mapping
    public static final String numToChar[] = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};

    // used in generating temporary passwords
    public final static char[] pwdChars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    public final static char[] pinAndOtpChars = "0123456789".toCharArray();

    // Backend error codes

    // these are my own defined
    public static final String BL_MYERROR_GENERAL = "500";
    public static final String BL_MYERROR_NO_SUCH_USER = "502";
    public static final String BL_MYERROR_CUSTOMER_ACC_DISABLED = "504";
    public static final String BL_MYERROR_SEND_SMS_FAILED = "508";
    public static final String BL_MYERROR_OTP_GENERATE_FAILED = "510";
    public static final String BL_MYERROR_OTP_GENERATED = "512";
    public static final String BL_MYERROR_WRONG_OTP = "514";
    public static final String BL_MYERROR_WRONG_PIN = "516";
    public static final String BL_MYERROR_NO_SUCH_QR_CARD = "520";
    public static final String BL_MYERROR_WRONG_QR_CARD = "522";
    public static final String BL_MYERROR_QR_CARD_INUSE = "524";
    public static final String BL_MYERROR_QR_CARD_WRONG_MERCHANT = "526";
    public static final String BL_MYERROR_SERVER_ERROR_ACC_DISABLED = "599";


    // these are defined by backendless
    public static final String BL_ERROR_NO_DATA_FOUND = "1009";

    public static final String BL_ERROR_REGISTER_DUPLICATE = "3033";
    public static final String BL_ERROR_LOGIN_DISABLED = "3000";
    public static final String BL_ERROR_ALREADY_LOGGOED_IN = "3002";
    public static final String BL_ERROR_INVALID_ID_PASSWD = "3003";
    public static final String BL_ERROR_EMPTY_ID_PASSWD = "3006";
    public static final String BL_ERROR_ACCOUNT_LOCKED = "3036";
    public static final String BL_ERROR_MULTIPLE_LOGIN_LIMIT = "3044";


    /*
     * Below ones are locally relevant to code in this backend project only
     */
    public static int SEND_TXN_SMS_MIN_AMOUNT = 10;
    public static String APP_ID = "09667F8B-98A7-E6B9-FFEB-B2B6EE831A00";
    public static String SECRET_KEY = "95971CBD-BADD-C61D-FF32-559664AE4F00";

    public static int LOG_POLICY_NUM_MSGS = 1;
    public static int LOG_POLICY_FREQ_SECS = 0;
}
