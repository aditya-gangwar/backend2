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

    public static int dbQueryMaxPageSize = 100;


    /*
     * Below ones are locally relevant to code in this backend project only
     */
    public static int SEND_TXN_SMS_MIN_AMOUNT = 10;
    public static String APP_ID = "09667F8B-98A7-E6B9-FFEB-B2B6EE831A00";
    public static String SECRET_KEY = "95971CBD-BADD-C61D-FF32-559664AE4F00";

    public static int LOG_POLICY_NUM_MSGS = 1;
    public static int LOG_POLICY_FREQ_SECS = 0;
}
