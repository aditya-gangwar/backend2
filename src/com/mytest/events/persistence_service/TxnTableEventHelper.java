package com.mytest.events.persistence_service;

import com.backendless.Backendless;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.logging.Logger;
import com.backendless.servercode.ExecutionResult;
import com.mytest.constants.*;
import com.mytest.database.Customers;
import com.mytest.database.WrongAttempts;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.database.Cashback;
import com.mytest.database.Transaction;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by adgangwa on 13-05-2016.
 */
public class TxnTableEventHelper {
    private Logger mLogger;
    private BackendOps mBackendOps;

    private int cl_debit;
    private int cl_credit;
    private int cb_debit;
    private int cb_credit;
    private int cl_balance;
    private int cb_balance;
    private String merchantName;
    private String txnDate;

    public void handleBeforeCreate(Transaction transaction, String cbTable) throws Exception {
        initCommon();
        mLogger.debug("In Transaction handleBeforeCreate");

        String merchantId = transaction.getMerchant_id();
        String customerId = transaction.getCustomer_id();

        // Fetch customer
        Customers customer = mBackendOps.getCustomer(customerId, true);
        if(customer==null) {
            //TODO: add in alarms
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_MYERROR_GENERAL,"");
            throw new BackendlessException(fault);
        }

        // verify PIN
        if( !transaction.getCpin().equals(customer.getTxn_pin()) ) {
            // fetch or create related wrong attempt row
            WrongAttempts attempt = mBackendOps.fetchOrCreateWrongAttempt(customerId, DbConstants.ATTEMPT_TYPE_USER_PIN);
            if(attempt != null) {
                // Lock account, if max wrong attempt limit reached
                if( attempt.getAttempt_cnt() >= GlobalSettingsConstants.CUSTOMER_WRONG_ATTEMPT_LIMIT) {
                    // lock customer account
                    customer.setAdmin_status(DbConstants.USER_STATUS_LOCKED);
                    customer.setStatus_reason(DbConstants.LOCKED_WRONG_PIN_LIMIT_RCHD);
                    customer.setStatus_update_time(new Date());
                    mBackendOps.updateCustomer(customer);

                    BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_MYERROR_FAILED_ATTEMPT_LIMIT_RCHD,"");
                    throw new BackendlessException(fault);
                }
                // increase attempt count
                attempt.setAttempt_cnt(attempt.getAttempt_cnt()+1);
                mBackendOps.saveWrongAttempt(attempt);
            }

            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_MYERROR_WRONG_PIN,"Wrong PIN");
            throw new BackendlessException(fault);
        }

        // Fetch cashback record
        String whereClause = "rowid = '"+customerId+merchantId+"'";
        Cashback cashback = null;
        ArrayList<Cashback> data = mBackendOps.fetchCashback(whereClause, false, cbTable);
        if(data!=null) {
            cashback = data.get(0);

            // update amounts in cashback oject
            cashback.setCl_credit(cashback.getCl_credit() + transaction.getCl_credit());
            cashback.setCl_debit(cashback.getCl_debit() + transaction.getCl_debit());
            cashback.setCb_credit(cashback.getCb_credit() + transaction.getCb_credit());
            cashback.setCb_debit(cashback.getCb_debit() + transaction.getCb_debit());
            cashback.setTotal_billed(cashback.getTotal_billed() + transaction.getTotal_billed());

            // add missing transaction fields
            transaction.setCust_private_id(cashback.getCust_private_id());
            transaction.setTrans_id(CommonUtils.generateTxnId(merchantId));
            transaction.setCreate_time(new Date());
            // the only use of adding cashback in txn (thus maintaining relationship in DB) is that
            // both will get updated in one go
            transaction.setCashback(cashback);

        } else {
            //TODO: add in alarms
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_MYERROR_GENERAL,"");
            throw new BackendlessException(fault);
        }

    }

    public void handleAfterCreate(Transaction transaction, ExecutionResult<Transaction> result) throws Exception {
        initCommon();
        mLogger.debug("In Transaction handleAfterCreate");

        // If transaction creation successful send SMS to customer
        if(result.getException()==null) {
            buildAndSendTxnSMS(transaction);
        }
    }

    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.TxnTableEventHelper");
        mBackendOps = new BackendOps(mLogger);
    }

    private void buildAndSendTxnSMS(Transaction transaction) throws Exception
    {
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
                merchantName = transaction.getMerchant_name().toUpperCase(Locale.ENGLISH);
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
