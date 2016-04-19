package com.mytest.models;

/**
 * Created by adgangwa on 19-04-2016.
 */
public class SmsTemplates {

    public static String SMS_TXN_DEBIT_CL_CB = "Mycash: %s debited Rs %d from your Account and Rs %d from Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_CREDIT_CL_DEBIT_CB = "Mycash: %s added Rs %d to your Account and debited Rs %d from Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";

    public static String SMS_TXN_CREDIT_CL = "Mycash: %s added Rs %d to your Account on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CL = "Mycash: %s debited Rs %d from your Account on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CB = "Mycash: %s debited Rs %d from your Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";

}
