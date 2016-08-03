package com.mytest.constants;

import java.util.Locale;

/**
 * This class contains constants relevant to both server code and app code.
 * This class should be exactly same everywhere
 */
public class CommonConstants {

    /*
     * Due to some issue in backendless - the errorCode is not correctly transmitted to app
     * from the Backend API - on raising an exception.
     * So, the errorCode is passed in 'errorMsg' field of the Backendless exception.
     * This prefix is added to the message to signal the same.
     */
    public static final String PREFIX_ERROR_CODE_AS_MSG = "ZZ_";
    /*
     * To use int as boolean
     */
    public static final int BOOLEAN_VALUE_FALSE = 0;
    public static final int BOOLEAN_VALUE_TRUE = 2;
    public static final int BOOLEAN_VALUE_INVALID = -1;

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
    public static final int MERCHANT_ID_LEN = 6;
    public static final int AGENT_ID_LEN = 8;

    // DOB in format 'DDMMYYYY'
    public static final int DOB_SECRET_LEN = 8; //mobile number

    public static final int PIN_OTP_LEN = 5;
    public static final int TRANSACTION_ID_LEN = 10;
    public static final int MAX_DEVICES_PER_MERCHANT = 3;
    public static final int MOBILE_NUM_LENGTH = 10;
    public static final int dbQueryMaxPageSize = 100;
    public static final int CUSTOMER_QRCODE_LENGTH = 7;

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
     * Index of various parameters in Txn CSV records
     * Per day file containing these records is created by backend,
     * but used by app to show txns to user.
     * Format:
     * trans_id,time,merchant_id,merchant_name,customer_id,cust_private_id,
     * total_billed,cb_billed,cl_debit,cl_credit,cb_debit,cb_credit,cb_percent\n
     */
    public static int TXN_CSV_IDX_ID = 0;
    public static int TXN_CSV_IDX_TIME = 1;
    public static int TXN_CSV_IDX_MERCHANT_ID = 2;
    public static int TXN_CSV_IDX_MERCHANT_NAME = 3;
    public static int TXN_CSV_IDX_CUSTOMER_ID = 4;
    public static int TXN_CSV_IDX_CUSTOMER_PVT_ID = 5;
    public static int TXN_CSV_IDX_TOTAL_BILLED = 6;
    public static int TXN_CSV_IDX_CB_BILLED = 7;
    public static int TXN_CSV_IDX_ACC_DEBIT = 8;
    public static int TXN_CSV_IDX_ACC_CREDIT = 9;
    public static int TXN_CSV_IDX_CB_REDEEM = 10;
    public static int TXN_CSV_IDX_CB_AWARD = 11;
    public static int TXN_CSV_IDX_CB_RATE = 12;

    /*
     * Other common constants
     */
    public static String TRANSACTION_ID_PREFIX = "TX";
    // number to character mapping
    public static final String numToChar[] = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
}
