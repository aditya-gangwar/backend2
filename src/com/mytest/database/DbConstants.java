package com.mytest.database;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class DbConstants {
    public static final int USER_TYPE_MERCHANT = 0;
    public static final int USER_TYPE_CUSTOMER = 1;

    public static final int USER_STATUS_DEFAULT = 0;
    public static final int USER_STATUS_REGISTERED = 1;
    public static final int USER_STATUS_ACTIVE = 2;
    public static final int USER_STATUS_DISABLED = 3;
    public static final int USER_STATUS_DISABLED_WRONG_PIN = 4;

    public static final int CUSTOMER_CARD_STATUS_NEW = 0;
    public static final int CUSTOMER_CARD_STATUS_WITH_MERCHANT = 1;
    public static final int CUSTOMER_CARD_STATUS_ALLOTTED = 2;
    public static final int CUSTOMER_CARD_STATUS_REMOVED = 3;

    // Customer operation codes
    public static final String CUSTOMER_OP_NEW_CARD = "New Card";
    public static final String CUSTOMER_OP_RESET_PIN = "Reset PIN";
    public static final String CUSTOMER_OP_CHANGE_MOBILE = "Change Mobile";

    // Merchant operation codes
    public static final String MERCHANT_OP_NEW_DEVICE_LOGIN = "New Device Login"; // this operation is not stored in ops table
    public static final String MERCHANT_OP_RESET_PASSWD = "Reset Password";

    // Merchant Op Status
    public static final String MERCHANT_OP_STATUS_PENDING = "Pending";
    public static final String MERCHANT_OP_STATUS_LOCKED = "Locked";
    public static final String MERCHANT_OP_STATUS_COMPLETE = "Completed";

    public static String CASHBACK_TABLE_NAME = "Cashback";
    public static String TRANSACTION_TABLE_NAME = "Transaction";

    public static String CUSTOMER_ID_COUNTER = "customer_id";
    public static String MERCHANT_ID_COUNTER = "merchant_id";

}
