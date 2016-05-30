package com.mytest.events.persistence_service;

import com.backendless.Backendless;
import com.backendless.logging.Logger;
import com.backendless.servercode.ExecutionResult;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendConstants;
import com.mytest.utilities.CommonConstants;
import com.mytest.database.Cashback;
import com.mytest.database.Transaction;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by adgangwa on 13-05-2016.
 */
public class TxnTableEventHelper {
    private Logger mLogger;

    int cl_debit;
    int cl_credit;
    int cb_debit;
    int cb_credit;
    int cl_balance;
    int cb_balance;
    String merchantName;
    String txnDate;

    public void buildAndSendTxnSMS(Transaction transaction, ExecutionResult<Transaction> result) throws Exception
    {
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.TxnTableEventHelper");

        mLogger.debug("In Transaction afterCreate");

        // If transaction creation successful send SMS to customer
        if(result.getException()==null) {
            String custMobile = transaction.getCustomer_id();
            String txnId = transaction.getTrans_id();
            mLogger.debug("Transaction update was successful: "+custMobile+", "+txnId);

            cl_debit = transaction.getCl_debit();
            cl_credit = transaction.getCl_credit();
            cb_debit = transaction.getCb_debit();
            cb_credit = transaction.getCb_credit();

            // Send SMS only in cases of 'redeem > INR 10' and 'add cash in account'
            if( cl_debit > BackendConstants.SEND_TXN_SMS_MIN_AMOUNT
                    || cl_credit > BackendConstants.SEND_TXN_SMS_MIN_AMOUNT
                //|| cb_debit > AppConstants.SEND_TXN_SMS_MIN_AMOUNT
                    ) {
                Cashback cashback = transaction.getCashback();
                if(cashback==null) {
                    mLogger.error("Cashback object is not available.");
                } else {
                    merchantName = cashback.getMerchant_name().toUpperCase(Locale.ENGLISH);
                    cb_balance = cashback.getCb_credit() - cashback.getCb_debit();
                    cl_balance = cashback.getCl_credit() - cashback.getCl_debit();

                    SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_BACKEND, CommonConstants.DATE_LOCALE);
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                    txnDate = sdf.format(transaction.getCreate_time());

                    // Build SMS
                    String smsText = buildSMS();
                    if(smsText==null) {
                        mLogger.error("Failed to build transaction SMS");
                    } else {
                        // Send SMS through HTTP
                        mLogger.debug("SMS to send: "+smsText+" : "+smsText.length());
                        if( !SmsHelper.sendSMS(smsText,custMobile) ) {
                            // TODO: write to alarm table for retry later
                        }
                    }
                }
            }
        }
    }

    private String buildSMS() {
        String sms=null;

        if(cl_debit>0 && cb_debit>0) {
            sms = String.format(SmsConstants.SMS_TXN_DEBIT_CL_CB,merchantName,cl_debit,cb_debit,txnDate,cl_balance,cb_balance);
        } else if(cl_credit> BackendConstants.SEND_TXN_SMS_MIN_AMOUNT && cb_debit>0) {
            sms = String.format(SmsConstants.SMS_TXN_CREDIT_CL_DEBIT_CB,merchantName,cl_credit,cb_debit,txnDate,cl_balance,cb_balance);
        } else if(cl_credit>0) {
            sms = String.format(SmsConstants.SMS_TXN_CREDIT_CL,merchantName,cl_credit,txnDate,cl_balance,cb_balance);
        } else if(cl_debit> BackendConstants.SEND_TXN_SMS_MIN_AMOUNT) {
            sms = String.format(SmsConstants.SMS_TXN_DEBIT_CL,merchantName,cl_debit,txnDate,cl_balance,cb_balance);
        } else if(cb_debit> BackendConstants.SEND_TXN_SMS_MIN_AMOUNT) {
            sms = String.format(SmsConstants.SMS_TXN_DEBIT_CB,merchantName,cb_debit,txnDate,cl_balance,cb_balance);
        }
        return sms;
    }

}
