package in.myecash.events.persistence_service;

import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import in.myecash.common.CommonUtils;
import in.myecash.common.MyGlobalSettings;
import in.myecash.messaging.SmsConstants;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import in.myecash.common.database.*;
import in.myecash.common.constants.*;
import in.myecash.constants.*;

/**
 * Created by adgangwa on 13-05-2016.
 */
public class TxnTableEventHelper {

    private MyLogger mLogger = new MyLogger("events.TxnTableEventHelper");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    private Merchants mMerchant;
    private Customers mCustomer;
    private Transaction mTransaction;
    private boolean mValidException;

    private String mMerchantId;
    private String mCustomerId;

    private int cl_debit;
    private int cl_credit;
    private int cb_debit;
    private int cb_credit;
    private int cl_balance;
    private int cb_balance;
    private String merchantName;
    private String txnDate;

    public void handleBeforeCreate(RunnerContext context, Transaction txn) throws Exception {
        BackendUtils.initAll();
        mTransaction = txn;
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "txn-beforeCreate";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = mTransaction.getCustomer_id();

        mValidException = false;
        try {
            mLogger.debug("In Transaction handleBeforeCreate");
            if(context==null) {
                mLogger.error("Context itself is null");
            } else if(context.getUserToken()==null) {
                mLogger.error("In handleBeforeCreate: RunnerContext: "+context.toString());
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.NOT_LOGGED_IN), "User not logged in");
            } else {
                HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, context.getUserToken() );
            }

            // Fetch mMerchant
            mMerchant = (Merchants) BackendUtils.fetchCurrentUser(context.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);
            mMerchantId = mMerchant.getAuto_id();
            mLogger.setProperties(mMerchantId,DbConstants.USER_TYPE_MERCHANT, mMerchant.getDebugLogs());

            //mLogger.debug("Invocation context: "+InvocationContext.asString());
            commonTxnProcessing();

            // update mCustomer for txn table
            updateTxnTables(mCustomer, mMerchant.getTxn_table());

            // Fetch cashback record
            String whereClause = "rowid = '" + mCustomer.getPrivate_id() + mMerchantId + "'";
            Cashback cashback = null;
            ArrayList<Cashback> data = BackendOps.fetchCashback(whereClause, mMerchant.getCashback_table(), false, false);
            if (data != null) {
                cashback = data.get(0);

                // update amounts in cashback object
                cashback.setCl_credit(cashback.getCl_credit() + mTransaction.getCl_credit());
                cashback.setCl_debit(cashback.getCl_debit() + mTransaction.getCl_debit());
                // check for cash account limit
                if ((cashback.getCl_credit() - cashback.getCl_debit()) > MyGlobalSettings.getCashAccLimit()) {
                    mValidException = true;
                    throw new BackendlessException(String.valueOf(ErrorCodes.CASH_ACCOUNT_LIMIT_RCHD), "Cash account limit reached: " + mCustomerId);
                }
                cashback.setCb_credit(cashback.getCb_credit() + mTransaction.getCb_credit());
                cashback.setCb_debit(cashback.getCb_debit() + mTransaction.getCb_debit());
                cashback.setTotal_billed(cashback.getTotal_billed() + mTransaction.getTotal_billed());
                cashback.setCb_billed(cashback.getCb_billed() + mTransaction.getCb_billed());

                // add/update transaction fields
                mTransaction.setCust_private_id(mCustomer.getPrivate_id());
                mTransaction.setMerchant_id(mMerchantId);
                mTransaction.setMerchant_name(mMerchant.getName());
                mTransaction.setTrans_id(BackendUtils.generateTxnId(mMerchantId));
                mEdr[BackendConstants.EDR_API_PARAMS_IDX]=mEdr[BackendConstants.EDR_API_PARAMS_IDX]+
                        BackendConstants.BACKEND_EDR_SUB_DELIMETER+mTransaction.getTrans_id();
                mTransaction.setCreate_time(new Date());
                mTransaction.setArchived(false);
                if(mTransaction.getImgFileName()!=null && !mTransaction.getImgFileName().isEmpty()) {
                    // calling app will need to upload 'card image file' with this name
                    mTransaction.setImgFileName(BackendUtils.getTxnImgFilename(mTransaction.getTrans_id()));
                }
                // following are uses to adding cashback object to txn:
                // 1) both txn and cashback, will get updated in one go - thus saving rollback scenarios
                // 2) updated cashback object will be automatically returned,
                // along with updated transaction object to calling app,
                // which can thus use it to display updated balance - instead of fetching cashback again.
                // 3) cashback object in transaction will be used in afterCreate() of txn table too - to get updated balance
                mTransaction.setCashback(cashback);

            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Txn commit: No cashback object found: "+ mMerchantId +","+ mCustomerId);
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e, mValidException,mLogger,mEdr);
            if(e instanceof BackendlessException) {
                throw BackendUtils.getNewException((BackendlessException) e);
            }
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
     * Transaction creation is not done as API i.e. from servercode, only due to this function.
     * As currently this fx. gets called in 'async' mode - while if we do it from API (i.e. sending SMS) it will be 'sync'
     * Thus it saves us few millisecs while creating txn.
     */
    public void handleAfterCreate(RunnerContext context, Transaction txn, ExecutionResult<Transaction> result) throws Exception {
        BackendUtils.initAll();
        mTransaction = txn;
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "txn-afterCreate";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = mTransaction.getMerchant_id()+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                mTransaction.getCustomer_id()+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                mTransaction.getTrans_id();

        try {
            //mLogger.debug("In Transaction handleAfterCreate");
            // If transaction creation successful send SMS to mCustomer
            if(result.getException()==null) {
                buildAndSendTxnSMS();
                mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            } else {
                mEdr[BackendConstants.EDR_EXP_CODE_IDX] = String.valueOf(result.getException().getCode());
                mEdr[BackendConstants.EDR_EXP_CODE_IDX] = result.getException().getExceptionMessage();
                mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_NOK;
            }

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public Transaction cancelTxn(String ctxtUserId, String txnId, String cardId, String pin) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "cancelTxn";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = txnId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+cardId;

        mValidException = false;
        try {
            mLogger.debug("In cancelTxn");

            // Fetch mMerchant
            mMerchant = (Merchants) BackendUtils.fetchCurrentUser(ctxtUserId,
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);
            mMerchantId = mMerchant.getAuto_id();
            mLogger.setProperties(mMerchantId,DbConstants.USER_TYPE_MERCHANT, mMerchant.getDebugLogs());

            // fetch txn
            mTransaction = BackendOps.fetchTxn(txnId, mMerchant.getTxn_table());
            if(!mTransaction.getMerchant_id().equals(mMerchantId) ) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Txn does not below to this Merchant: "+txnId);
            }

            if(mTransaction.getArchived()) {
                // I shudn't be here
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "This transaction cannot be cancelled.");
            }

            // set cancel time - to indicate cancellation of this txn in further processing
            mTransaction.setCancelTime(new Date());
            // set for verification purpose in commonTxnProcessing - values will be rolled back after that
            String oldCardId = mTransaction.getUsedCardId();
            mTransaction.setUsedCardId(cardId);
            String oldPin = mTransaction.getCpin();
            mTransaction.setCpin(pin);

            // do common processing
            commonTxnProcessing();
            // restore values
            mTransaction.setUsedCardId(oldCardId);
            mTransaction.setCpin(oldPin);

            // Fetch cashback record
            Cashback cashback = mTransaction.getCashback();
            if (cashback != null) {
                // update amounts in cashback object

                // 'cl credit' is not changed in txn cancellation
                //cashback.setCl_credit(cashback.getCl_credit() + transaction.getCl_credit());
                // any 'cl debit' in this txn, will be returned
                cashback.setCl_debit(cashback.getCl_debit() - mTransaction.getCl_debit());

                // any 'cb credit' will be deducted
                cashback.setCb_credit(cashback.getCb_credit() - mTransaction.getCb_credit());
                // any 'cb debit' will be returned
                cashback.setCb_debit(cashback.getCb_debit() - mTransaction.getCb_debit());

                // reverse bill amounts too
                cashback.setTotal_billed(cashback.getTotal_billed() - mTransaction.getTotal_billed());
                cashback.setCb_billed(cashback.getCb_billed() - mTransaction.getCb_billed());

                // cashback will be updated along with txn
                mTransaction.setCashback(cashback);
                BackendOps.updateTxn(mTransaction, mMerchant.getTxn_table());

            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Txn commit: No cashback object found: "+ mMerchantId +","+ mCustomerId);
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return mTransaction;

        } catch(Exception e) {
            BackendUtils.handleException(e, mValidException,mLogger,mEdr);
            if(e instanceof BackendlessException) {
                throw BackendUtils.getNewException((BackendlessException) e);
            }
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }

    }

    /*
     * Private helper methods
     */
    private void commonTxnProcessing() {

        // Fetch mCustomer
        mCustomer = BackendOps.getCustomer(mTransaction.getCustomer_id(), BackendConstants.ID_TYPE_MOBILE, true);
        mCustomerId = mCustomer.getMobile_num();
        mEdr[BackendConstants.EDR_CUST_ID_IDX] = mCustomerId;

        // credit txns not allowed under expiry duration
        // txn cancellation is allowed
        if(mMerchant.getAdmin_status()== DbConstants.USER_STATUS_READY_TO_REMOVE) {
            if( (mTransaction.getCb_credit() > 0 || mTransaction.getCl_credit() > 0) &&
                   mTransaction.getCancelTime()==null ) {
                // ideally it shudn't reach here - as both cl and cb settings are disabled and not allowed to be edited in app
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.ACC_UNDER_EXPIRY), "");
            }
        }

        // check if mCustomer is enabled
        BackendUtils.checkCustomerStatus(mCustomer, mEdr, mLogger);

        // if mCustomer in 'restricted access' mode - allow only credit txns
        // txn cancellation is also not allowed
        if(mCustomer.getAdmin_status()==DbConstants.USER_STATUS_MOB_CHANGE_RECENT &&
                ( mTransaction.getCl_debit()>0 || mTransaction.getCb_debit()>0 ||
                        mTransaction.getCancelTime()==null )) {
            mValidException = true; // to avoid logging of this exception
            throw new BackendlessException(String.valueOf(ErrorCodes.USER_MOB_CHANGE_RESTRICTED_ACCESS), "");
        }

        // verify PIN
        if(CommonUtils.customerPinRequired(mMerchant, mTransaction)) {
            if (mTransaction.getCpin() != null) {
                if (!mTransaction.getCpin().equals(mCustomer.getTxn_pin())) {
                    BackendUtils.handleWrongAttempt(mCustomerId, mCustomer, DbConstants.USER_TYPE_CUSTOMER,
                            DbConstantsBackend.WRONG_PARAM_TYPE_PIN, DbConstants.OP_TXN_COMMIT, mEdr, mLogger);
                    mValidException = true; // to avoid logging of this exception
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_PIN), "Wrong PIN attempt: " + mCustomerId);
                } else {
                    mTransaction.setCpin(DbConstants.TXN_CUSTOMER_PIN_USED);
                }
            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_PIN), "PIN Missing: " + mCustomerId);
            }
        } else {
            mTransaction.setCpin(DbConstants.TXN_CUSTOMER_PIN_NOT_USED);
        }

        // check if card required and provided and matches
        if(BackendUtils.customerCardRequired(mTransaction)) {
            if(mTransaction.getUsedCardId()!=null) {
                if(!mTransaction.getUsedCardId().equals(mCustomer.getCardId())) {
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_CARD), "Card Mismatch: " + mCustomerId);
                }
            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_CARD), "Card Missing: " + mCustomerId);
            }
        }

        // check that card is not blocked
        // TODO: check this if this is really required - if not, avoid fetching card object also
        mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = mCustomer.getCardId();
        BackendUtils.checkCardForUse(mCustomer.getMembership_card());

    }

    private Customers updateTxnTables(Customers customer, String mchntTable) {

        String currTables = customer.getTxn_tables();
        if(!currTables.contains(mchntTable)) {
            String newTables = currTables+CommonConstants.CSV_DELIMETER+mchntTable;
            mLogger.debug("Setting new Txn tables for mCustomer: "+newTables+","+currTables);
            customer.setTxn_tables(newTables);
            // update mCustomer object
            return BackendOps.updateCustomer(customer);
        }
        return null;
    }

    private void buildAndSendTxnSMS()
    {
        String custMobile = mTransaction.getCustomer_id();
        String txnId = mTransaction.getTrans_id();
        //mLogger.debug("Transaction update was successful: "+custMobile+", "+txnId);

        cl_debit = mTransaction.getCl_debit();
        cl_credit = mTransaction.getCl_credit();
        cb_debit = mTransaction.getCb_debit();
        cb_credit = mTransaction.getCb_credit();

        // Send SMS only in cases of 'redeem > INR 10' and 'add cash in account'
        if( cl_debit > BackendConstants.SEND_TXN_SMS_CL_MIN_AMOUNT
                || cl_credit > BackendConstants.SEND_TXN_SMS_CL_MIN_AMOUNT
            || cb_debit > BackendConstants.SEND_TXN_SMS_CB_MIN_AMOUNT
                ) {
            Cashback cashback = mTransaction.getCashback();
            if(cashback==null) {
                mLogger.error("Cashback object is not available.");
                throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR),"Txn afterCreate: No cashback object found: "+
                        mTransaction.getMerchant_id()+","+mTransaction.getCustomer_id());
            } else {
                merchantName = mTransaction.getMerchant_name().toUpperCase(Locale.ENGLISH);
                cb_balance = cashback.getCb_credit() - cashback.getCb_debit();
                cl_balance = cashback.getCl_credit() - cashback.getCl_debit();

                SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_BACKEND, CommonConstants.DATE_LOCALE);
                sdf.setTimeZone(TimeZone.getTimeZone(BackendConstants.TIMEZONE));
                txnDate = sdf.format(mTransaction.getCreate_time());

                // Build SMS
                String smsText = buildSMS();
                if(smsText!=null) {
                    // Send SMS through HTTP
                    SmsHelper.sendSMS(smsText,custMobile, mEdr, mLogger);
                }
            }
        }
    }

    private String buildSMS() {
        String sms=null;

        if(cl_debit>BackendConstants.SEND_TXN_SMS_CL_MIN_AMOUNT && cb_debit>BackendConstants.SEND_TXN_SMS_CB_MIN_AMOUNT) {
            sms = String.format(SmsConstants.SMS_TXN_DEBIT_CL_CB,merchantName,cl_debit,cb_debit,txnDate,cl_balance,cb_balance);

        } else if(cl_credit> BackendConstants.SEND_TXN_SMS_CL_MIN_AMOUNT && cb_debit>BackendConstants.SEND_TXN_SMS_CB_MIN_AMOUNT) {
            sms = String.format(SmsConstants.SMS_TXN_CREDIT_CL_DEBIT_CB,merchantName,cl_credit,cb_debit,txnDate,cl_balance,cb_balance);

        } else if(cl_credit>BackendConstants.SEND_TXN_SMS_CL_MIN_AMOUNT) {
            sms = String.format(SmsConstants.SMS_TXN_CREDIT_CL,merchantName,cl_credit,txnDate,cl_balance,cb_balance);

        } else if(cl_debit> BackendConstants.SEND_TXN_SMS_CL_MIN_AMOUNT) {
            sms = String.format(SmsConstants.SMS_TXN_DEBIT_CL,merchantName,cl_debit,txnDate,cl_balance,cb_balance);

        } else if(cb_debit> BackendConstants.SEND_TXN_SMS_CB_MIN_AMOUNT) {
            sms = String.format(SmsConstants.SMS_TXN_DEBIT_CB,merchantName,cb_debit,txnDate,cl_balance,cb_balance);
        }
        return sms;
    }

}
