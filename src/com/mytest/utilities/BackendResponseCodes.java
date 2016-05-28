package com.mytest.utilities;

/**
 * Created by adgangwa on 28-05-2016.
 */
public class BackendResponseCodes {
    // Backend error codes

    // these are my own defined
    public static final String BL_MYRESPONSE_NO_ERROR = "100";

    public static final String BL_MYERROR_GENERAL = "500";
    public static final String BL_MYERROR_NO_SUCH_USER = "501";
    public static final String BL_MYERROR_CUSTOMER_ACC_DISABLED = "502";
    public static final String BL_MYERROR_SEND_SMS_FAILED = "503";
    public static final String BL_MYERROR_WRONG_INPUT_DATA = "504";

    public static final String BL_MYERROR_OTP_GENERATE_FAILED = "510";
    public static final String BL_MYERROR_OTP_GENERATED = "511";
    public static final String BL_MYERROR_WRONG_OTP = "512";
    public static final String BL_MYERROR_WRONG_PIN = "513";

    public static final String BL_MYERROR_NO_SUCH_QR_CARD = "520";
    public static final String BL_MYERROR_WRONG_QR_CARD = "521";
    public static final String BL_MYERROR_QR_CARD_INUSE = "522";
    public static final String BL_MYERROR_QR_CARD_WRONG_MERCHANT = "523";

    public static final String BL_MYERROR_VERIFICATION_FAILED = "530";
    public static final String BL_MYERROR_FAILED_ATTEMPT_LIMIT_RCHD = "531";

    public static final String BL_MYERROR_NOT_TRUSTED_DEVICE = "540";
    public static final String BL_MYERROR_TRUSTED_DEVICE_LIMIT_RCHD = "541";

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
}
