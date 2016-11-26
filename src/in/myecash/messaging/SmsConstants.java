package in.myecash.messaging;

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

    // SMS against Manual Requests by Merchant
    public static String SMS_ADMIN_MCHNT_SEND_PSWD_RESET_HINT = "Dear MyeCash Merchant - As per request, your password reset hint is '%s'. You can use the same to generate new password. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_ADMIN_MCHNT_MOBILE_CHANGE = "Dear MyeCash Merchant - As per request, registered mobile number of your account '%s' is changed successfully to '%s'. Don't forget to remove any Lost Mobile device from your Trusted Device List. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_ADMIN_MCHNT_RESET_TRUSTED_DEVICES = "Dear MyeCash Merchant - As per request, all devices are removed from your 'Trusted Device List'. You can now try login from new device. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_ADMIN_MCHNT_ACC_CLOSURE = "Dear MyeCash Merchant - As per request, we have put your account '%s' under %s days of Account Closure Notice period. No 'Credit' transactions are allowed now. Your account will be automatically removed on %s. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_ADMIN_MCHNT_CANCEL_ACC_CLOSURE = "Dear MyeCash Merchant - As per request, we have cancelled your earlier Account closure request. You can do 'Credit' transactions also now. Thanks. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_ADMIN_MCHNT_ACC_ENABLE = "Dear MyeCash Merchant - As per request, we have enabled your account '%s'. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";

    // SMS against Manual Requests by Customer
    public static String SMS_ADMIN_CUST_RESET_PIN = "Dear MyeCash Customer - As per request, your new PIN after reset is '%s'. You can change it to your choice from App Menu. ALWAYS KEEP YOUR PIN SECRET. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_ADMIN_CUST_MOBILE_CHANGE = "Dear MyeCash Customer - As per request, registered mobile number of your account is changed successfully to '%s'. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_ADMIN_CUST_ACC_ENABLE = "Dear MyeCash Customer - As per request, we have enabled your account '%s'. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";


    // Account Status change SMS
    public static String SMS_ACCOUNT_LOCKED_PASSWORD = "Your MyeCash account '%s' is locked due to multiple wrong password attempts. It will be unlocked automatically after %s hours.";
    public static String SMS_ACCOUNT_LOCKED_PIN = "Your MyeCash account '%s' is locked due to multiple wrong PIN attempts. It will be unlocked automatically after %s hours.";
    public static String SMS_ACCOUNT_LOCKED_VERIFY_FAILED = "Your MyeCash account '%s' is locked due to multiple wrong verifications. It will be unlocked automatically after %s hours.";
    public static String SMS_ACCOUNT_DISABLE = "Your MyeCash account number '%s' is Disabled now. You can call us for further help.";
    public static String SMS_ACCOUNT_ENABLE = "Your MyeCash account number '%s' is Enabled now. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    //public static String SMS_ACCOUNT_LOCKED_FORGOT_USERID = "Your MyCash account '%s' is locked for next %d hours, due to more than allowed wrong 'forgot userId' attempts.";
    //public static String SMS_ACCOUNT_LOCKED_PASSWD_RESET_AGENT = "Your MyCash agent account '%s' is locked, due to more than allowed wrong 'password reset' attempts.";

    // MyeCash transaction SMS
    public static String SMS_TXN_DEBIT_CL_CB = "Mycash: %s debited Rs %d from your Account and Rs %d from Cashback on %s. Balance- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_CREDIT_CL_DEBIT_CB = "Mycash: %s added Rs %d to your Account and redeemed Rs %d from Cashback on %s. Balance- Account:Rs %d, Cashback:Rs %d.";

    public static String SMS_TXN_CREDIT_CL = "Mycash: %s added Rs %d to your Account on %s. Balance: Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CL = "Mycash: %s debited Rs %d from your Account on %s. Balance: Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CB = "Mycash: %s redeemed Rs %d from your Cashback on %s. Balance: Account:Rs %d, Cashback:Rs %d";

    public static String SMS_TXN_CANCEL = "Mycash: %s cancelled transaction with ID %s. Balance: Account:Rs %d, Cashback:Rs %d";

    // Password/PIN messages
    public static String SMS_FIRST_PASSWD = "Dear User - Welcome to MyCash family !! Your User ID is %s, and your password is '%s'. PLZ DO CHANGE PASSWORD AFTER FIRST LOGIN.";
    public static String SMS_PASSWD = "MyeCash new password for user %s is '%s'. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_PASSWD_CHANGED = "MyeCash password changed successfully for user %s. PLS CALL CUSTOMER CARE IF NOT DONE BY YOU.";

    // OTP messages
    //public static String SMS_OTP = "You have initiated '%s' for user %s. OTP is '%s' and valid for %d mins only. PLS CALL US IF NOT DONE BY YOU.";
    public static String SMS_REG_CUST_OTP = "Use OTP %s to register as MyeCash Customer. Valid for next %d minutes. Always enter OTP yourself in Merchant device. PLS CALL US IF NOT REQUESTED BY YOU.";
    public static String SMS_LOGIN_OTP = "Login attempt detected from New Device. Use OTP %s to add it as 'Trusted Device' for account %s. PLS CALL US IF NOT REQUESTED BY YOU.";
    public static String SMS_CHANGE_MOB_OTP = "Use OTP %s to verify this Mobile number. Valid for next %d minutes. PLS CALL US IF NOT REQUESTED BY YOU.";
    public static String SMS_NEW_CARD_OTP = "Use OTP %s to authenticate new membership card for account %s. Valid for next %d minutes. PLS CALL US IF NOT REQUESTED BY YOU.";
    public static String SMS_PIN_RESET_OTP = "Use OTP %s to authenticate PIN Reset for customer account %s. Valid for next %d minutes. PLS CALL US IF NOT REQUESTED BY YOU.";

    public static String SMS_PIN = "MyeCash transaction PIN for user %s is '%s'. KEEP YOUR PIN SECRET AND NEVER SHARE WITH ANYONE";
    public static String SMS_PIN_CHANGED = "MyeCash PIN changed successfully for user %s. PLS CALL CUSTOMER CARE IF NOT DONE BY YOU.";

    // Registration / User ID messages
    public static String SMS_MERCHANT_ID = "Your MyCash Merchant ID is %s. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_MERCHANT_ID_FIRST = "Dear Merchant - Welcome to MyeCash family. Your Merchant ID for login is %s. Happy Customers to you.";
    public static String SMS_CUSTOMER_REGISTER = "Welcome to MyeCash family. Your registered name is '%s'. Your Mobile number '$s' is customer ID. To manage your account, download MyeCash Customer App from Android store.";
    public static String SMS_REG_INTERNAL_USER = "Dear User - Welcome to MyCash family!! Your User ID is %s, and your password is your DOB in DDMMYYYY format. PLS CHANGE YOUR PASSWORD IMMEDIATELY AFTER LOGIN.";

    // Mobile/Card change messages
    public static String SMS_MOBILE_CHANGE = "Registered Mobile number of your MyeCash account '%s' is changed successfully to '%s'. PLS CALL US IMMEDIATELY IF NOT DONE BY YOU.";
    public static String SMS_CUSTOMER_NEW_CARD = "You have registered new card with number %s to your account %s. PLS CALL US IMMEDIATELY IF NOT DONE BY YOU.";

}
