package in.myecash.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adgangwa on 10-08-2016.
 */
public class DbConstantsBackend {

    // Table names - for which class name is not same
    public static String CASHBACK_TABLE_NAME = "Cashback";
    public static String TRANSACTION_TABLE_NAME = "Transaction";
    public static String MERCHANT_ID_BATCH_TABLE_NAME = "MerchantIdBatches";
    public static String CARD_ID_BATCH_TABLE_NAME = "CardIdBatches";

    // Merchant table - 'status_reason' column values
    // Same for 'reason' column in MerchantOps table too
    //public static final int ENABLED_NEW_USER = 1;
    public static final String ENABLED_ACTIVE = "enabled";
    public static final String DISABLED_AUTO_BY_SYSTEM = "";
    public static final String DISABLED_ON_USER_REQUEST = "";
    public static final String LOCKED_WRONG_PASSWORD_LIMIT_RCHD = "Wrong password limit reached";
    public static final String LOCKED_WRONG_PIN_LIMIT_RCHD = "Wrong PIN attempts limit reached";
    public static final String LOCKED_FORGOT_PASSWORD_ATTEMPT_LIMIT_RCHD = "Wrong forgot password attempts limit reached";
    public static final String LOCKED_FORGOT_USERID_ATTEMPT_LIMIT_RCHD = "Wrong forgot user id attempts limit reached";
    public static final String REG_ERROR_ROLE_ASSIGN_FAILED = "Error in registration";

    // Map int status values to corresponding descriptions
    /*
    public static String statusReasonDescriptions[] = {
            "",
            "User is active",
            "By system for security purpose. Will be re-activated after verification.",
            "On user request.",
            "Wrong password attempts limit reached.",
            "Wrong PIN attempts limit reached.",
            "Wrong 'password reset' attempts limit reached.",
            ""
    };*/

    /*
     * MerchantOps table
     */
    // 'opcode' column values
    public static final String MERCHANT_OP_RESET_PASSWD = "Reset Password";
    public static final String MERCHANT_OP_RESET_ACC_FOR_LOGIN = "Reset Account for Login";
    public static final String MERCHANT_OP_CHANGE_MOBILE = "Change Mobile";
    public static final String MERCHANT_OP_DISABLE_ACC = "Disable Account";
    public static final String MERCHANT_OP_REMOVE_ACC = "Remove Account";

    /*
     * Common to MerchantOps and CustomerOps table
     */
    // 'status' column values
    public static final String USER_OP_STATUS_PENDING = "Pending";
    //public static final String USER_OP_STATUS_LOCKED = "In progress";
    public static final String USER_OP_STATUS_COMPLETE = "Completed";
    public static final String USER_OP_STATUS_ERROR = "Failed";
    // 'initiatedBy' column values
    public static final String USER_OP_INITBY_MCHNT = "Merchant";
    public static final String USER_OP_INITBY_CUSTOMER = "Customer";
    public static final String USER_OP_INITBY_ADMIN = "MyeCash Admin";
    // 'initiatedVia' column values - valid when initiated by merchant
    public static final String USER_OP_INITVIA_APP = "App";
    public static final String USER_OP_INITVIA_CC = "Call to Customer Care";
    public static final String USER_OP_INITVIA_IVR = "Call to IVR";
    public static final String USER_OP_INITVIA_MANUAL = "Manual application";

    // Otp table - 'opcode' column values - apart from ones from 'MerchantOps' and 'CustomerOps' tables
    public static final String MERCHANT_OP_NEW_DEVICE_LOGIN = "new_device_login";

    // Counters table - 'name' column values
    public static final String CUSTOMER_ID_COUNTER = "customer_id";
    public static final String MERCHANT_ID_COUNTER = "merchant_id";

    // WrongAttempts table - 'attempt_type' column values
    public static final String ATTEMPT_TYPE_FORGOT_USERID = "forgotUserId";
    public static final String ATTEMPT_TYPE_PASSWORD_RESET = "passwordReset";
    public static final String ATTEMPT_TYPE_USER_LOGIN = "userLogin";
    public static final String ATTEMPT_TYPE_USER_PIN = "userPin";

    public static final Map<String, String> attemptTypeToAccLockedReason;
    static {
        Map<String, String> aMap = new HashMap<>(10);

        // my own backend response codes
        aMap.put(ATTEMPT_TYPE_FORGOT_USERID, LOCKED_FORGOT_USERID_ATTEMPT_LIMIT_RCHD);
        aMap.put(ATTEMPT_TYPE_PASSWORD_RESET, LOCKED_FORGOT_PASSWORD_ATTEMPT_LIMIT_RCHD);
        aMap.put(ATTEMPT_TYPE_USER_LOGIN, LOCKED_WRONG_PASSWORD_LIMIT_RCHD);
        aMap.put(ATTEMPT_TYPE_USER_PIN, LOCKED_WRONG_PIN_LIMIT_RCHD);

        attemptTypeToAccLockedReason = Collections.unmodifiableMap(aMap);
    }

    // Merchant Id Batch status
    public static final String MERCHANT_ID_BATCH_STATUS_AVAILABLE = "available";
    public static final String MERCHANT_ID_BATCH_STATUS_OPEN = "open";
    public static final String MERCHANT_ID_BATCH_STATUS_CLOSED = "closed";
    // Card Id Batch status
    public static final String CARD_ID_BATCH_STATUS_AVAILABLE = "available";
    public static final String CARD_ID_BATCH_STATUS_OPEN = "open";
    public static final String CARD_ID_BATCH_STATUS_CLOSED = "closed";

}
