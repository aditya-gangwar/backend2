package com.mytest.models;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class DbConstants {
    public static final int USER_TYPE_MERCHANT = 0;
    public static final int USER_TYPE_CUSTOMER = 1;

    public static final int USER_STATUS_NEW = 0;
    public static final int USER_STATUS_ACTIVE = 1;
    public static final int USER_STATUS_DISABLED = 2;

    public static final int CUSTOMER_CARD_STATUS_NEW = 0;
    public static final int CUSTOMER_CARD_STATUS_WITH_MERCHANT = 1;
    public static final int CUSTOMER_CARD_STATUS_ALLOTTED = 2;
    public static final int CUSTOMER_CARD_STATUS_REMOVED = 3;

    public static String CASHBACK_TABLE_NAME = "Cashback";
    public static String TRANSACTION_TABLE_NAME = "Transaction";

}
