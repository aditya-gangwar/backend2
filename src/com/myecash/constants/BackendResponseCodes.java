package com.myecash.constants;

/**
 * Created by adgangwa on 28-05-2016.
 */
public class BackendResponseCodes {

    // these are my own defined
    public static final String BE_RESPONSE_NO_ERROR = "100";
    public static final String BE_RESPONSE_OP_SCHEDULED = "102";
    public static final String BE_RESPONSE_OTP_GENERATED = "103";

    // BE_ERROR_GENERAL should not be thrown to the client apps
    public static final String BE_ERROR_GENERAL = "500";
    public static final String BE_ERROR_NO_SUCH_USER = "501";
    public static final String BE_ERROR_ACC_DISABLED = "502";
    public static final String BE_ERROR_ACC_LOCKED = "503";
    public static final String BE_ERROR_OPERATION_NOT_ALLOWED = "504";
    public static final String BE_ERROR_DUPLICATE_REQUEST = "505";
    public static final String BE_ERROR_FIRST_LOGIN_PENDING = "506";
    public static final String BE_ERROR_NOT_LOGGED_IN = "507";

    public static final String BE_ERROR_SEND_SMS_FAILED = "510";
    public static final String BE_ERROR_WRONG_INPUT_DATA = "511";
    public static final String BE_ERROR_DUPLICATE_USER = "512";

    public static final String BE_ERROR_OTP_GENERATE_FAILED = "520";
    public static final String BE_ERROR_WRONG_OTP = "521";
    public static final String BE_ERROR_WRONG_PIN = "522";

    public static final String BE_ERROR_NO_SUCH_CARD = "530";
    public static final String BE_ERROR_WRONG_CARD = "531";
    public static final String BE_ERROR_CARD_INUSE = "532";
    public static final String BE_ERROR_CARD_WRONG_MERCHANT = "533";
    public static final String BE_ERROR_CARD_BLOCKED = "534";

    public static final String BE_ERROR_VERIFICATION_FAILED = "540";
    public static final String BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD = "541";
    public static final String BE_ERROR_NOT_TRUSTED_DEVICE = "542";
    public static final String BE_ERROR_TRUSTED_DEVICE_LIMIT_RCHD = "543";
    public static final String BE_ERROR_CASH_ACCOUNT_LIMIT_RCHD = "544";
    public static final String BE_ERROR_DEVICE_ALREADY_REGISTERED = "545";

    public static final String BE_ERROR_NO_OPEN_MERCHANT_ID_BATCH = "550";


    // these are defined by backendless
    public static final String BL_ERROR_NO_DATA_FOUND = "1009";
    public static final String BL_ERROR_NO_PERMISSIONS = "1012";
    public static final String BL_ERROR_DUPLICATE_ENTRY = "1155";

    public static final String BL_ERROR_REGISTER_DUPLICATE = "3033";
    public static final String BL_ERROR_LOGIN_DISABLED = "3000";
    public static final String BL_ERROR_ALREADY_LOGGOED_IN = "3002";
    public static final String BL_ERROR_INVALID_ID_PASSWD = "3003";
    public static final String BL_ERROR_EMPTY_ID_PASSWD = "3006";
    public static final String BL_ERROR_ACCOUNT_LOCKED = "3036";
    public static final String BL_ERROR_MULTIPLE_LOGIN_LIMIT = "3044";
}
