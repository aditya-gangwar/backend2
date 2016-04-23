package com.mytest.models;

import java.util.Locale;

/**
 * Created by adgangwa on 10-04-2016.
 */
public class AppConstants {

    public static final Locale DATE_LOCALE = Locale.ENGLISH;
    // used where only date (without time) is to be shown
    public static final String DATE_FORMAT_ONLY_DATE = "dd MMM, yy";
    // used to specify 'date with no time' to the backend, like in where clause
    public static final String DATE_FORMAT_ONLY_DATE_BACKEND = "dd-MMM-yyyy";
    // used in reports etc where both date and time is to be shown
    public static final String DATE_FORMAT_WITH_TIME = "dd/MM/yyyy HH:mm:ss";
    // date format to be used in filename
    public static final String DATE_FORMAT_ONLY_DATE_FILENAME = "ddMMMyy";

    public static final int USER_TYPE_MERCHANT = 0;
    public static final int USER_TYPE_CUSTOMER = 1;

    public static int SEND_TXN_SMS_MIN_AMOUNT = 10;

    public static String FILE_PATH_SEPERATOR = "/";
    public static String MERCHANT_ROOT_DIR = "merchants"+AppConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_DIR = "txns"+AppConstants.FILE_PATH_SEPERATOR;
    public static String MERCHANT_TXN_FILE_PREFIX = "txns_";

    public static String CASHBACK_TABLE_NAME = "Cashback";
    public static String TRANSACTION_TABLE_NAME = "Transaction";
    public static String TRANSACTION_ID_PREFIX = "TX";

    public static final int PASSWORD_LEN = 5;
    public static int merchant_id_length = 6;
    public static int dbQueryMaxPageSize = 100;
    public static final String CSV_DELIMETER = ",";
    public static final String CSV_FILE_EXT = ".csv";
    public static final String CSV_NEWLINE = "\n";


    // ERROR CODES
    public static final String ERROR_Y_M_I_Here = "I shouldn't be here !!";

    // Backendless error codes
    public static final String BL_ERROR_REGISTER_DUPLICATE = "3033";
    public static final String BL_ERROR_LOGIN_DISABLED = "3000";
    public static final String BL_ERROR_ALREADY_LOGGOED_IN = "3002";
    public static final String BL_ERROR_INVALID_ID_PASSWD = "3003";
    public static final String BL_ERROR_EMPTY_ID_PASSWD = "3006";
    public static final String BL_ERROR_ACCOUNT_LOCKED = "3036";
    public static final String BL_ERROR_MULTIPLE_LOGIN_LIMIT = "3044";

    // App common codes
    public static final int NO_ERROR = -1;
    public static final int GENERAL_ERROR = 0;

    // Merchant user operation error codes
    public static final int USER_ALREADY_REGISTERED = 1;
    public static final int USER_ALREADY_LOGGED_IN = 2;
    public static final int USER_NOT_LOGGED_IN = 3;
    public static final int USER_ACC_DISABLED = 4;
    public static final int USER_WRONG_ID_PASSWD = 5;
    public static final int USER_ACC_LOCKED = 6;
    public static final int FILE_UPLOAD_FAILED = 7;
    public static final int USER_NOT_REGISTERED = 8;
    public static final int NO_INTERNET_CONNECTION = 9;
    public static final int EMPTY_VALUE = 10;
    public static final int INVALID_FORMAT = 11;
    public static final int INVALID_LENGTH = 12;
    public static final int INVALID_VALUE = 13;
    public static final int NO_DATA_FOUND = 14;

    // used in generating random transaction ids and temporary passwords
    public final static char[] idchars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

}
