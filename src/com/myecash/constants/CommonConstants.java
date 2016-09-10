package com.myecash.constants;

import java.util.Locale;

/**
 * This class contains constants relevant to both server code and app code.
 * This class should be exactly same everywhere
 */
public class CommonConstants {

    public static final String INDIA_MOBILE_COUNTRY_CODE = "91";
    /*
     * Due to some issue in backendless - the errorCode is not correctly transmitted to app
     * from the Backend API - on raising an exception.
     * So, the errorCode is passed in 'errorMsg' field of the Backendless exception.
     * This prefix is added to the message to signal the same.
     */
    public static final String PREFIX_ERROR_CODE_AS_MSG = "ZZ/";
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
    // used to specify 'date with no time' in the CSV report generated
    public static final String DATE_FORMAT_ONLY_DATE_CSV = "dd/MM/yyyy";
    // used in reports etc where both date and time is to be shown
    public static final String DATE_FORMAT_WITH_TIME = "dd/MM/yyyy HH:mm:ss";
    // date format to be used in filename
    public static final String DATE_FORMAT_ONLY_DATE_FILENAME = "ddMMMyy";
    // format to show only time in 12 hr format
    public static final String DATE_FORMAT_ONLY_TIME_12 = "hh:mm a";
    // format to show only time in CSV file
    public static final String DATE_FORMAT_ONLY_TIME_24_CSV = "HH:mm:ss";

    /*
     * Size, Length and Limits
     */
    public static final int MOBILE_NUM_LENGTH = 10;
    public static final int MERCHANT_ID_LEN = 8;
    public static final int AGENT_ID_LEN = 7;
    public static final int CUSTOMER_INTERNAL_ID_LEN = 6;
    public static final int CUSTOMER_CARDID_LEN = 11;
    // DOB in format 'DDMMYYYY'
    public static final int DOB_SECRET_LEN = 8; //mobile number
    public static final int TRANSACTION_ID_LEN = 10;
    public static final int PIN_OTP_LEN = 5;

    public static final int MAX_DEVICES_PER_MERCHANT = 3;
    public static final int dbQueryMaxPageSize = 100;

    /*
     * Backend path constants
     */
    public static String FILE_PATH_SEPERATOR = "/";
    public static String MERCHANT_ROOT_DIR = "merchants"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_DISPLAY_IMAGES_DIR = MERCHANT_ROOT_DIR+"displayImages"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_LOGGING_ROOT_DIR = "logging"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TEMP_DISPLAY_IMAGES_DIR = MERCHANT_ROOT_DIR+"displayImages"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_ROOT_DIR = MERCHANT_ROOT_DIR+"txnCsvFiles"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_CUST_DATA_ROOT_DIR = MERCHANT_ROOT_DIR+"customerData"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_IMAGE_ROOT_DIR = MERCHANT_ROOT_DIR+"txnImages"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_FILE_PREFIX = "txns_";
    public static String MERCHANT_CUST_DATA_FILE_PREFIX = "customers_";

    public static final String CSV_DELIMETER = ",";
    public static final String CSV_SUB_DELIMETER = ":";
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
    public static int TXN_CSV_IDX_CUST_PIN = 13;

    /*
     * Index of various parameters in Cashback CSV records (stored in CustData CSV files)
     * Format:
     * <Total Account Credit>,<Total Account Debit>,
     * <Total Cashback Credit>,<Total Cashback Debit>,
     * <Total Billed>,<Total Cashback Billed>,
     * <create time>,<update time>
     * Records with double bracket '<<>>' are only sent to 'customer care' users
     */
    public static int CB_CSV_CUST_PVT_ID = 0;
    public static int CB_CSV_MCHNT_ID = 1;
    public static int CB_CSV_ACC_CR = 2;
    public static int CB_CSV_ACC_DB = 3;
    public static int CB_CSV_CR = 4;
    public static int CB_CSV_DB = 5;
    public static int CB_CSV_TOTAL_BILL = 6;
    public static int CB_CSV_BILL = 7;
    public static int CB_CSV_CREATE_TIME = 8;
    public static int CB_CSV_UPDATE_TIME = 9;
    public static int CB_CSV_CUST_DETAILS = 10;
    public static int CB_CSV_TOTAL_FIELDS = 11;

    /*
     * Index of various parameters in CustomerDetails CSV records (rcvd as part of cashback object)
     * Format:
     * <private id>,<mobile_num>,<<name>>,<<first login ok>>,<<cust create time>>
     * <acc_status>,<acc_status_reason>,<acc_status_update_time>,<<admin remarks>>
     *     -- membership card data follows --
     * <card_id>,<card_status>,<card_status_update_time>
     * Records with double bracket '<<>>' are only sent to 'customer care' users
     */
    public static int CUST_CSV_PRIVATE_ID = 0;
    public static int CUST_CSV_MOBILE_NUM = 1;
    public static int CUST_CSV_NAME = 2;
    public static int CUST_CSV_FIRST_LOGIN_OK = 3;
    public static int CUST_CSV_CUST_CREATE_TIME = 4;
    public static int CUST_CSV_ACC_STATUS = 5;
    public static int CUST_CSV_STATUS_REASON = 6;
    public static int CUST_CSV_STATUS_UPDATE_TIME = 7;
    public static int CUST_CSV_ADMIN_REMARKS = 8;
    public static int CUST_CSV_CARD_ID = 9;
    public static int CUST_CSV_CARD_STATUS = 10;
    public static int CUST_CSV_CARD_STATUS_UPDATE_TIME = 11;
    public static int CUST_CSV_TOTAL_FIELDS = 12;

    /*
     * Other common constants
     */
    public static String TRANSACTION_ID_PREFIX = "TX";
    // number to character mapping
    public static final String numToChar[] = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
}
