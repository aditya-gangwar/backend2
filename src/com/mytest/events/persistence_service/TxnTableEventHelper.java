package com.mytest.events.persistence_service;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.InvocationContext;
import com.backendless.servercode.RunnerContext;
import com.mytest.constants.*;
import com.mytest.database.Customers;
import com.mytest.database.Merchants;
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

    private int cl_debit;
    private int cl_credit;
    private int cb_debit;
    private int cb_credit;
    private int cl_balance;
    private int cb_balance;
    private String merchantName;
    private String txnDate;

    public void handleBeforeCreate(RunnerContext context, Transaction transaction, String cbTable) throws Exception {
        initCommon();
        try {
            mLogger.debug("In Transaction handleBeforeCreate");
            HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, context.getUserToken() );

            // Fetch merchant
            BackendlessUser user = BackendOps.fetchUserByObjectId(context.getUserId(), DbConstants.USER_TYPE_MERCHANT);
            Merchants merchant = (Merchants) user.getProperty("merchant");
            // check if merchant is enabled
            CommonUtils.checkMerchantStatus(merchant);
            String merchantId = merchant.getAuto_id();

            // Fetch customer
            String customerId = CommonUtils.addMobileCC(transaction.getCustomer_id());
            Customers customer = BackendOps.getCustomer(customerId, BackendConstants.CUSTOMER_ID_MOBILE);
            // check if customer is enabled
            CommonUtils.checkCustomerStatus(customer);
            // check that card is not blocked
            // TODO: check this if this is really required - if not, avoid fetching card object also
            CommonUtils.checkCardForUse(customer.getMembership_card());

            // verify PIN
            if(CommonUtils.customerPinRequired(merchant, transaction)) {
                if (transaction.getCpin() != null) {
                    if (!transaction.getCpin().equals(customer.getTxn_pin())) {
                        CommonUtils.handleWrongAttempt(customer, DbConstants.USER_TYPE_CUSTOMER, DbConstantsBackend.ATTEMPT_TYPE_USER_PIN);
                        throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_PIN, "Wrong PIN attempt: " + customerId);
                    }
                } else {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_PIN, "PIN Missing: " + customerId);
                }
            }

            // Fetch cashback record
            String whereClause = "rowid = '" + customer.getPrivate_id() + merchantId + "'";
            Cashback cashback = null;
            ArrayList<Cashback> data = BackendOps.fetchCashback(whereClause, cbTable);
            if (data != null) {
                cashback = data.get(0);

                // update amounts in cashback oject
                cashback.setCl_credit(cashback.getCl_credit() + transaction.getCl_credit());
                cashback.setCl_debit(cashback.getCl_debit() + transaction.getCl_debit());
                // check for cash account limit
                if ((cashback.getCl_credit() - cashback.getCl_debit()) > GlobalSettingsConstants.CUSTOMER_CASH_MAX_LIMIT) {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_CASH_ACCOUNT_LIMIT_RCHD, "Cash account limit reached: " + customerId);
                }
                cashback.setCb_credit(cashback.getCb_credit() + transaction.getCb_credit());
                cashback.setCb_debit(cashback.getCb_debit() + transaction.getCb_debit());
                cashback.setTotal_billed(cashback.getTotal_billed() + transaction.getTotal_billed());
                cashback.setCb_billed(cashback.getCb_billed() + transaction.getCb_billed());

                // add/update transaction fields
                transaction.setCustomer_id(customer.getMobile_num());
                transaction.setCust_private_id(customer.getPrivate_id());
                transaction.setMerchant_id(merchantId);
                transaction.setMerchant_name(merchant.getName());
                transaction.setTrans_id(CommonUtils.generateTxnId(merchantId));
                transaction.setCreate_time(new Date());
                transaction.setArchived(false);
                transaction.setCpin(null);
                // following are uses to adding cashback object to txn:
                // 1) both txn and cashback, will get updated in one go - thus saving rollback scenarios
                // 2) updated cashback object will be automatically returned,
                // along with updated transaction object to calling app,
                // which can thus use it to display updated balance - instead of fetching cashback again.
                // 3) cashback object in transaction will be used in afterCreate() of txn table too - to get updated balance
                transaction.setCashback(cashback);

            } else {
                //TODO: add in alarms
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, "Txn commit: No cashback object found: "+merchantId+","+customerId);
            }

        } catch (Exception e) {
            mLogger.error("Exception in TxnEventHelper:handleBeforeCreate: "+e.toString());
            Backendless.Logging.flush();
            if(e instanceof BackendlessException) {
                throw CommonUtils.getNewException((BackendlessException) e);
            }
            throw e;
        }
    }

    public void handleAfterCreate(RunnerContext context, Transaction transaction, ExecutionResult<Transaction> result) throws Exception {
        initCommon();
        try {
            mLogger.debug("In Transaction handleAfterCreate");
            // If transaction creation successful send SMS to customer
            if(result.getException()==null) {
                buildAndSendTxnSMS(transaction);
            }
        } catch (Exception e) {
            mLogger.error("Exception in TxnEventHelper:handleAfterCreate: "+e.toString());
            Backendless.Logging.flush();
            throw e;
        }
    }

    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.TxnTableEventHelper");
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
            //|| cb_debit > BackendConstants.SEND_TXN_SMS_MIN_AMOUNT
                ) {
            Cashback cashback = transaction.getCashback();
            if(cashback==null) {
                mLogger.error("Cashback object is not available.");
            } else {
                merchantName = transaction.getMerchant_name().toUpperCase(Locale.ENGLISH);
                cb_balance = cashback.getCb_credit() - cashback.getCb_debit();
                cl_balance = cashback.getCl_credit() - cashback.getCl_debit();

                SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_BACKEND, CommonConstants.DATE_LOCALE);
                sdf.setTimeZone(TimeZone.getTimeZone(BackendConstants.TIMEZONE));
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
