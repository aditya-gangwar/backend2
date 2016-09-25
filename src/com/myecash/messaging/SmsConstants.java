package com.myecash.messaging;

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

    // TODO: Review all SMS text

    // Account Status change SMS
    public static String SMS_ACCOUNT_LOCKED_PASSWORD = "Your MyCash account '%s' is locked for next %d hours, due to more than allowed wrong password attempts.";
    public static String SMS_ACCOUNT_LOCKED_PIN = "Your MyCash account '%s' is locked for next %d hours, due to more than allowed wrong PIN attempts.";
    public static String SMS_ACCOUNT_LOCKED_FORGOT_USERID = "Your MyCash account '%s' is locked for next %d hours, due to more than allowed wrong 'forgot userId' attempts.";
    public static String SMS_ACCOUNT_LOCKED_PASSWD_RESET = "Your MyCash account '%s' is locked for next %d hours, due to more than allowed wrong 'forgot password' attempts.";
    public static String SMS_ACCOUNT_LOCKED_PASSWD_RESET_AGENT = "Your MyCash agent account '%s' is locked, due to more than allowed wrong 'password reset' attempts.";

    // MyeCash transaction SMS
    public static String SMS_TXN_DEBIT_CL_CB = "Mycash: %s debited Rs %d from your Account and Rs %d from Cashback on %s. Balance: Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_CREDIT_CL_DEBIT_CB = "Mycash: %s added Rs %d to your Account and debited Rs %d from Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";

    public static String SMS_TXN_CREDIT_CL = "Mycash: %s added Rs %d to your Account on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CL = "Mycash: %s debited Rs %d from your Account on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CB = "Mycash: %s debited Rs %d from your Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d";

    // Password/PIN/OTP messages
    public static String SMS_FIRST_PASSWD = "Dear User - Welcome to MyCash family !! Your User ID is %s, and your password is '%s'. PLZ DO CHANGE PASSWORD AFTER FIRST LOGIN.";
    public static String SMS_PASSWD = "MyeCash new password for user %s is '%s'. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_FIRST_PIN_CUSTOMER = "Dear Customer - Welcome to MyCash family !! Your User ID is %s, and your first PIN is '%s'. PLS DO NOT SHARE WITH ANYONE.";
    public static String SMS_PIN = "MyCash transaction PIN for user %s is '%s'. PLS DO NOT SHARE WITH ANYONE.";
    public static String SMS_PASSWD_CHANGED = "MyeCash password changed successfully for user %s. PLS CALL CUSTOMER CARE IF NOT DONE BY YOU.";
    public static String SMS_OTP = "You have initiated '%s' txn for user %s. OTP is '%s' and valid for %d mins only. PLS CALL US IF NOT DONE BY YOU.";

    // Registration / User ID messages
    public static String SMS_MERCHANT_ID = "Your MyCash Merchant ID is %s. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_MERCHANT_ID_FIRST = "Dear Merchant - Welcome to MyeCash family !! Your Merchant ID for login is %s. Happy Customers to you.";
    public static String SMS_REG_INTERNAL_USER = "Dear User - Welcome to MyCash family!! Your User ID is %s, and your password is your DOB in DDMMYYYY format. PLS CHANGE YOUR PASSWORD IMMEDIATELY AFTER LOGIN.";

    // Mobile/Card change messages
    public static String SMS_MOBILE_CHANGE = "Registered mobile number of your MyeCash account '%s' is changed successfully to '%s'. PLS CALL US IMMEDIATELY IF NOT DONE BY YOU.";
    public static String SMS_CUSTOMER_NEW_CARD = "You have registered new card with number %s to your account %s. PLS CALL US IMMEDIATELY IF NOT DONE BY YOU.";

    // Account enable/disable
    public static String SMS_USER_ACC_DISABLE = "Dear User - Your account number '%s' is Disabled now. You can call us for further help.";
    public static String SMS_MCHNT_LOGIN_RESET = "Dear Merchant - As per request, the login access for account '%s' is reset successfully. Pls use 'Forgot Password' link to restore your access.";
    public static String SMS_MCHNT_MOBILE_CHANGE_ADMIN = "Dear Merchant - As per request, registered mobile number of your account '%s' is changed successfully to '%s'. Pls use 'Forgot Password' link to restore your access.";

    public static String SMS_MERCHANT_REMOVE = "Dear Merchant - As per your request, your account '%s' is under %s days of expiry period now. 'Credit' transactions are not allowed from now. Your account will be automatically deleted on %s.";
}
