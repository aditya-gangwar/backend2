package com.mytest.messaging;

/**
 * Created by adgangwa on 12-05-2016.
 */
public class SmsConstants {

    public static String SMSGW_URL_ENCODING = "UTF-8";
    public static String SMSGW_SENDER_ID = "MYCASH";
    public static String COUNTRY_CODE = "91";

    public static String SMSGW_TXTGURU_BASE_URL = "https://www.txtguru.in/imobile/api.php?";
    public static String SMSGW_TXTGURU_USERNAME = "aditya_gang";
    public static String SMSGW_TXTGURU_PASSWORD = "50375135";

    public static String SMSGW_MSG91_BASE_URL = "https://control.msg91.com/api/sendhttp.php?";
    public static String SMSGW_MSG91_AUTHKEY = "115853A9qGXSHBf575aaeb1";
    public static String SMSGW_MSG91_ROUTE_TXN = "4";
    public static String SMSGW_MSG91_ROUTE_PROMOTIONAL = "1";
    /*
     * SMS templates
     */

    public static String SMS_TXN_DEBIT_CL_CB = "Mycash: %s debited Rs %d from your Account and Rs %d from Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_CREDIT_CL_DEBIT_CB = "Mycash: %s added Rs %d to your Account and debited Rs %d from Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";

    public static String SMS_TXN_CREDIT_CL = "Mycash: %s added Rs %d to your Account on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CL = "Mycash: %s debited Rs %d from your Account on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CB = "Mycash: %s debited Rs %d from your Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d";

    public static String SMS_FIRST_PASSWD = "Dear Merchant - Welcome to MyCash family !! Your User ID is %s, and your first password is '%s'. PLZ CHANGE PASSWORD AFTER FIRST LOGIN.";
    public static String SMS_PASSWD = "MyCash new password for user %s is '%s'. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_FIRST_PIN = "Dear Customer - Welcome to MyCash family !! Your User ID is %s, and your first PIN is '%s'. PLS DO NOT SHARE WITH ANYONE.";
    public static String SMS_REG_AGENT = "Dear Agent - Welcome to MyCash family!! Your User ID is %s, and your password is your DOB in DDMMYYYY format. PLS CHANGE YOUR PASSWORD IMMEDIATELY AFTER LOGIN.";
    public static String SMS_PIN = "MyCash transaction PIN for user %s is '%s'. PLS DO NOT SHARE WITH ANYONE.";
    public static String SMS_PASSWD_CHANGED = "MyCash password changed successfully for user %s. PLS CALL CUSTOMER CARE IF NOT DONE BY YOU.";
    public static String SMS_MERCHANT_ID = "Your MyCash Merchant ID is %s. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_MERCHANT_ID_FIRST = "Dear Merchant - Welcome to MyeCash family !! Your Merchant ID for login is %s. Happy Customers to you.";

    public static String SMS_OTP = "You have initiated '%s' txn for user %s. OTP is '%s' and valid for %d mins only. PLS CALL US IF NOT DONE BY YOU.";

    public static String SMS_MOBILE_CHANGE = "Dear Customer - Registered mobile number of your account '%s' is changed successfully to '%s'. PLS CALL US IMMEDIATELY IF NOT DONE BY YOU.";
    public static String SMS_MOBILE_CHANGE_MERCHANT = "Dear Merchant - Registered mobile number of your account '%s' is changed successfully to '%s'. PLS CALL US IMMEDIATELY IF NOT DONE BY YOU.";
    public static String SMS_CUSTOMER_NEW_CARD = "You have registered new card with number %s to your account %s. PLS CALL US IMMEDIATELY IF NOT DONE BY YOU.";

    public static String SMS_ACCOUNT_LOCKED_PASSWORD = "Your MyCash account '%s' is locked for next %d hours, due to more than allowed wrong password attempts.";
    public static String SMS_ACCOUNT_LOCKED_PIN = "Your MyCash account '%s' is locked for next %d hours, due to more than allowed wrong PIN attempts.";
    public static String SMS_ACCOUNT_LOCKED_PASSWD_RESET = "Your MyCash account '%s' is locked for next %d hours, due to more than allowed wrong 'password reset' attempts.";

    public static String SMS_ACCOUNT_LOCKED_PASSWD_RESET_AGENT = "Your MyCash agent account '%s' is locked, due to more than allowed wrong 'password reset' attempts.";


}
