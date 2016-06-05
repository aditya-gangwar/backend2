package com.mytest.constants;

import java.util.Locale;

/**
 * This class contains constants relevant to both server code and app code.
 * This class should be exactly same everywhere
 */
public class CommonConstants {

    /*
     * Date formats
     */
    public static final Locale DATE_LOCALE = Locale.ENGLISH;
    // used where only date (without time) is to be shown
    public static final String DATE_FORMAT_ONLY_DATE_DISPLAY = "dd MMM, yy";
    // used to specify 'date with no time' to the backend, like in where clause
    public static final String DATE_FORMAT_ONLY_DATE_BACKEND = "dd-MMM-yyyy";
    // used in reports etc where both date and time is to be shown
    public static final String DATE_FORMAT_WITH_TIME = "dd/MM/yyyy HH:mm:ss";
    // date format to be used in filename
    public static final String DATE_FORMAT_ONLY_DATE_FILENAME = "ddMMMyy";
    // format to show only time in 12 hr format
    public static final String DATE_FORMAT_ONLY_TIME_12 = "h:mm a";

    /*
     * Size, Length and Limits
     */
    public static final int PIN_OTP_LEN = 5;
    public static final int TRANSACTION_ID_LEN = 10;
    public static int MAX_DEVICES_PER_MERCHANT = 3;
    public static int MERCHANT_ID_LEN = 6;
    public static int MOBILE_NUM_LENGTH = 10;
    public static int dbQueryMaxPageSize = 100;
    public static int CUSTOMER_QRCODE_LENGTH = 7;

    /*
     * Backend path constants
     */
    public static String FILE_PATH_SEPERATOR = "/";
    public static String MERCHANT_ROOT_DIR = "merchants"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_USERDATA_ROOT_DIR = MERCHANT_ROOT_DIR+"userdata"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_ROOT_DIR = MERCHANT_ROOT_DIR+"txn_files"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_IMAGE_ROOT_DIR = MERCHANT_ROOT_DIR+"txn_images"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_FILE_PREFIX = "txns_";

    public static final String CSV_DELIMETER = ",";
    public static final String CSV_FILE_EXT = ".csv";
    public static final String CSV_NEWLINE = "\n";

    /*
     * Other common constants
     */
    public static String TRANSACTION_ID_PREFIX = "TX";
    // number to character mapping
    public static final String numToChar[] = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
}
