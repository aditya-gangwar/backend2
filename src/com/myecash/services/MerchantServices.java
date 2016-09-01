package com.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import com.myecash.constants.*;
import com.myecash.database.*;
import com.myecash.messaging.SmsConstants;
import com.myecash.messaging.SmsHelper;
import com.myecash.utilities.TxnArchiver;
import com.myecash.utilities.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by adgangwa on 15-05-2016.
 */
public class MerchantServices implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.MerchantServices");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     * Merchant operations
     */
    public void changeMobile(String currentMobile, String newMobile, String otp) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "changeMobile";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = currentMobile+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                newMobile+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                otp;

        boolean positiveException = false;

        try {
            //mLogger.debug("In changeMobile: " + currentMobile + "," + newMobile);

            // Fetch merchant
            Merchants merchant = (Merchants) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger);

            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // Validate based on given current number
                if (!merchant.getMobile_num().equals(currentMobile)) {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
                }

                // Generate OTP to verify new mobile number
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(merchant.getAuto_id());
                newOtp.setMobile_num(newMobile);
                newOtp.setOpcode(DbConstantsBackend.MERCHANT_OP_CHANGE_MOBILE);
                BackendOps.generateOtp(newOtp,mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(merchant.getAuto_id(), otp);
                mLogger.debug("OTP matched for given merchant operation: " + merchant.getAuto_id());

                // Update with new mobile number
                String oldMobile = merchant.getMobile_num();
                merchant.setMobile_num(newMobile);
                merchant = BackendOps.updateMerchant(merchant);

                // add record in merchant ops table
                MerchantOps merchantops = new MerchantOps();
                merchantops.setMerchant_id(merchant.getAuto_id());
                merchantops.setOp_code(DbConstantsBackend.MERCHANT_OP_CHANGE_MOBILE);
                merchantops.setMobile_num(oldMobile);
                merchantops.setExtra_op_params(newMobile);
                merchantops.setOp_status(DbConstantsBackend.MERCHANT_OP_STATUS_COMPLETE);
                try {
                    BackendOps.saveMerchantOp(merchantops);
                } catch(Exception e) {
                    // ignore error
                    mLogger.error("changeMobile: Exception while adding merchant operation: "+e.toString());
                }

                mLogger.debug("Processed mobile change for: " + merchant.getAuto_id());

                // Send SMS on old and new mobile - ignore sent status
                String smsText = SmsHelper.buildMobileChangeSMS(oldMobile, newMobile);
                if(SmsHelper.sendSMS(smsText, oldMobile + "," + newMobile, mLogger)) {
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
                } else {
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }


    public void updateSettings(String cbRate, boolean addClEnabled, String email) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "updateSettings";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = cbRate+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                addClEnabled+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                email;

        try {
            //mLogger.debug("In updateSettings: "+cbRate+": "+addClEnabled+": "+email);
            //mLogger.debug("Before context: "+InvocationContext.asString());
            //mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

            Merchants merchant = (Merchants) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger);

            // update settings
            merchant.setCb_rate(cbRate);
            merchant.setCl_add_enable(addClEnabled);
            merchant.setEmail(email);
            BackendOps.updateMerchant(merchant);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    // Taking 'merchantId' and 'merchantCbTable' values from caller is a bit of security risk
    // as it will allow any logged-in merchant - to read (not update) cb details of other merchants too
    // but ignoring this for now - to keep this API as fast as possible - as this will be most called API
    public Cashback getCashback(String merchantId, String merchantCbTable, String customerId, boolean debugLogs) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getCashback";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                merchantCbTable+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                customerId;

        mLogger.setProperties(merchantId, DbConstants.USER_TYPE_MERCHANT, debugLogs);

        try {
            //mLogger.debug("In getCashback: " + merchantId + ": " + customerId);

            int customerIdType = CommonUtils.customerIdMobile(customerId) ? BackendConstants.CUSTOMER_ID_MOBILE : BackendConstants.CUSTOMER_ID_CARD;

            // TODO: see how to avoid fetching customer in common scenario - i.e. when cb object is available and customer status is active
            // one way is to put all mandatory info about customer in cb object only

            // Fetch customer using mobile or cardId
            Customers customer = BackendOps.getCustomer(customerId, customerIdType, true);
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getMobile_num();

            // Fetch cashback record
            // Create where clause to fetch cashback
            String whereClause = "rowid = '" + customer.getPrivate_id()+merchantId + "'";
            ArrayList<Cashback> data = BackendOps.fetchCashback(whereClause, merchantCbTable);
            Cashback cashback = null;
            if(data == null) {
                cashback = handleCashbackCreate(merchantId, merchantCbTable, customer);
            } else {
                cashback = data.get(0);
                mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = cashback.getMerchant_id();
            }

            // Cashback details to be returned - even if customer account/card is disabled/locked
            // so not checking for customer account/card status

            // Add 'customer details' in the cashback object to be returned
            // these details are not stored in DB along with cashback object
            cashback.setCustomer_details(buildCustomerDetails(customer));
            stripCashback(cashback);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return cashback;

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public MerchantStats getMerchantStats() {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getMerchantStats";

        try {
            //mLogger.debug("In getMerchantStats");

            // Fetch merchant
            Merchants merchant = (Merchants) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger);
            String merchantId = merchant.getAuto_id();

            // not checking for merchant account status

            // create new stats object
            // fetch merchant stat object, if exists
            MerchantStats stats = BackendOps.fetchMerchantStats(merchantId);
            // create object if not already available
            if (stats == null) {
                mLogger.debug("Creating new stats object");
                stats = new MerchantStats();
                stats.setMerchant_id(merchantId);
            }
            //reset all stats to 0
            stats.setBill_amt_no_cb(0);
            stats.setBill_amt_total(0);
            stats.setCash_credit(0);
            stats.setCb_credit(0);
            stats.setCash_debit(0);
            stats.setCb_debit(0);
            stats.setCust_cnt_cash(0);
            stats.setCust_cnt_cb(0);
            stats.setCust_cnt_cb_and_cash(0);
            stats.setCust_cnt_no_balance(0);

            // fetch all CB records for this merchant
            ArrayList<Cashback> data = BackendOps.fetchCashback("merchant_id = '" + merchantId + "'", merchant.getCashback_table());
            if(data != null) {
                // loop on all cashback objects and calculate stats
                mLogger.debug("Fetched cashback records: " + merchantId + ", " + data.size());
                for (int k = 0; k < data.size(); k++) {
                    Cashback cb = data.get(k);
                    // update customer counts
                    // no need to check for 'debit' amount - as 'credit' amount is total amount and includes debit amount too
                    if (cb.getCb_credit() > 0 && cb.getCl_credit() > 0) {
                        stats.cust_cnt_cb_and_cash++;
                    } else if (cb.getCb_credit() > 0) {
                        stats.cust_cnt_cb++;
                    } else if (cb.getCl_credit() > 0) {
                        stats.cust_cnt_cash++;
                    } else {
                        stats.cust_cnt_no_balance++;
                    }

                    // update amounts
                    stats.cb_credit = stats.cb_credit + cb.getCb_credit();
                    stats.cb_debit = stats.cb_debit + cb.getCb_debit();
                    stats.cash_credit = stats.cash_credit + cb.getCl_credit();
                    stats.cash_debit = stats.cash_debit + cb.getCl_debit();
                    stats.bill_amt_total = stats.bill_amt_total + cb.getTotal_billed();
                    stats.bill_amt_no_cb = stats.bill_amt_no_cb + (cb.getTotal_billed() - cb.getCb_billed());
                }
            }

            // save stats object - don't bother about return status
            // This is just for our own reporting purpose,
            // as for merchant stats anyways are calculated fresh each time from cashback objects
            try {
                stats.setUpdate_time(new Date());
                BackendOps.saveMerchantStats(stats);
            } catch (Exception e) {
                // ignore the exception
                mLogger.error("Exception while saving merchantStats object: "+e.toString());
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return stats;

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void archiveTxns() {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getMerchantStats";

        try {
            //mLogger.debug("In archiveTxns");

            // Fetch merchant
            Merchants merchant = (Merchants) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger);
            // not checking for merchant account status

            // archive txns
            TxnArchiver archiver = new TxnArchiver(mLogger, merchant, InvocationContext.getUserToken());
            archiver.archiveMerchantTxns(mEdr);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
     * Public methods: Backend REST APIs
     * Customer operations by merchant
     */
    public Cashback registerCustomer(String customerMobile, String name, String cardId) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "registerCustomer";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = customerMobile+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                name+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                cardId;

        Customers customer = null;
        CustomerCards card = null;
        BackendlessUser customerUser = null;

        try {
            //mLogger.debug("In registerCustomer: " + customerMobile + ": " + cardId);

            // Fetch merchant
            Merchants merchant = (Merchants) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger);

            // fetch customer card object
            card = BackendOps.getCustomerCard(cardId);
            mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = card.getCard_id();
            CommonUtils.checkCardForAllocation(card);
            // TODO: enable in production
            /*
            if(!card.getMerchant_id().equals(merchantId)) {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_CARD_WRONG_MERCHANT, "");
            }*/

            // Create customer object
            customer = createCustomer();
            // set fields
            // new record - so set it directly
            customer.setCashback_table(merchant.getCashback_table());
            customer.setMobile_num(customerMobile);
            customer.setName(name);
            customer.setCardId(cardId);
            // set membership card
            card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
            card.setStatus_update_time(new Date());
            customer.setMembership_card(card);

            // Create customer user
            customerUser = new BackendlessUser();
            customerUser.setProperty("user_id", customerMobile);
            // use generated PIN as password
            customerUser.setPassword(customer.getTxn_pin());
            customerUser.setProperty("user_type", DbConstants.USER_TYPE_CUSTOMER);
            // Both 'user' and 'customer' objects get created in single go
            // This also ensures that 'customer' object's 'ownerId' remains null
            // This helps to avoid direct update from app by the merchant who created this customer object
            customerUser.setProperty("customer", customer);

            customerUser = BackendOps.registerUser(customerUser);
            try {
                customer = (Customers) customerUser.getProperty("customer");
                mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getMobile_num();
                // assign custom role to it
                BackendOps.assignRole(customerMobile, BackendConstants.ROLE_CUSTOMER);
            } catch(Exception e) {
                // TODO: add as 'Major' alarm - user to be removed later manually
                // rollback to not-usable state
                rollbackRegister(customerMobile, card);
                throw e;
            }

            // Send sms to the customer with PIN
            String smsText = String.format(SmsConstants.SMS_FIRST_PIN_CUSTOMER, customerMobile, customer.getTxn_pin());
            if (SmsHelper.sendSMS(smsText, customerMobile, mLogger)) {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
            } else {
                // Don't consider the reg operation as failed
                // TODO: write to alarm table for retry later
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
            }

            try {
                // create cashback also - to avoid another call to 'getCashback' from merchant
                Cashback cashback = createCbObject(merchant.getAuto_id(), customer);
                // Add 'customer details' in the cashback object to be returned
                // these details are not stored in DB along with cashback object
                cashback.setCustomer_details(buildCustomerDetails(customer));
                // remove 'not needed sensitive' fields from cashback object
                stripCashback(cashback);

                // no exception - means function execution success
                mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
                return cashback;

            } catch(Exception e) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_REGISTER_SUCCESS_CREATE_CB_FAILED, "");
            }

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void execCustomerOp(String opCode, String customerId, String scannedCardId, String otp, String pin, String opParam) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "execCustomerOp";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = opCode+BackendConstants.BACKEND_EDR_SUB_DELIMETER +
                customerId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                scannedCardId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                otp+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                pin+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                opParam;

        boolean positiveException = false;

        try {
            // Fetch merchant
            Merchants merchant = (Merchants) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger);

            // Fetch customer user
            BackendlessUser custUser = BackendOps.fetchUser(customerId, DbConstants.USER_TYPE_CUSTOMER);
            Customers customer = (Customers) custUser.getProperty("customer");
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getMobile_num();
            String cardId = customer.getMembership_card().getCard_id();
            mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = cardId;
            // check if customer is enabled
            CommonUtils.checkCustomerStatus(customer, mLogger);

            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // Don't verify QR card# for 'new card' operation
                if (!opCode.equals(DbConstants.CUSTOMER_OP_NEW_CARD) &&
                        !cardId.equals(scannedCardId)) {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_CARD, "Wrong membership card");
                }

                // Don't verify PIN for 'reset PIN' operation
                if (!opCode.equals(DbConstants.CUSTOMER_OP_RESET_PIN) &&
                        !customer.getTxn_pin().equals(pin)) {

                    CommonUtils.handleWrongAttempt(customerId, customer, DbConstants.USER_TYPE_CUSTOMER, DbConstantsBackend.ATTEMPT_TYPE_USER_PIN, mLogger);
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_PIN, "Wrong PIN attempt: " + customer.getMobile_num());
                }

                // Generate OTP and send SMS
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(customerId);
                if (opCode.equals(DbConstants.CUSTOMER_OP_CHANGE_MOBILE)) {
                    newOtp.setMobile_num(opParam);
                } else {
                    newOtp.setMobile_num(customer.getMobile_num());
                }
                newOtp.setOpcode(opCode);
                BackendOps.generateOtp(newOtp,mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(customerId, otp);

                switch (opCode) {
                    case DbConstants.CUSTOMER_OP_NEW_CARD:
                        changeCustomerCard(customer, scannedCardId, opParam);
                        break;
                    case DbConstants.CUSTOMER_OP_CHANGE_MOBILE:
                        changeCustomerMobile(custUser, opParam);
                        break;
                    case DbConstants.CUSTOMER_OP_RESET_PIN:
                        resetCustomerPin(custUser);
                        break;
                }

                // add to customer ops table - for records purpose
                try {
                    CustomerOps op = new CustomerOps();
                    op.setOp_code(opCode);
                    op.setRequestor_id(merchant.getAuto_id());
                    op.setQr_card(scannedCardId);
                    op.setExtra_op_params(opParam);
                    BackendOps.saveCustomerOp(op);
                } catch(Exception e) {
                    // ignore error
                    mLogger.error("Customer change card op record save failed: "+e.toString());
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
     * Private helper methods
     */
    private Customers createCustomer() {
        Customers customer = new Customers();
        customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
        customer.setStatus_reason(DbConstants.ENABLED_ACTIVE);
        customer.setStatus_update_time(new Date());

        // get customer counter value and encode the same to get customer private id
        Long customerCnt =  BackendOps.fetchCounterValue(DbConstantsBackend.CUSTOMER_ID_COUNTER);
        String private_id = Base61.fromBase10(customerCnt);
        mLogger.debug("Generated private id: "+private_id);
        customer.setPrivate_id(private_id);

        // generate and set PIN
        String pin = CommonUtils.generateCustomerPIN();
        mLogger.debug("Generated PIN: "+pin);
        customer.setTxn_pin(pin);

        return customer;
    }

    private Cashback handleCashbackCreate(String merchantId, String merchantCbTable, Customers customer) {
        // Add cashback table name (of this merchant's) in customer record, if not already added (by some other merchant)
        boolean cbTableUpdated = false;
        String currCbTables = customer.getCashback_table();
        if(currCbTables==null || currCbTables.isEmpty()) {
            mLogger.debug("Setting new CB tables for customer: "+merchantCbTable+","+currCbTables);
            customer.setCashback_table(merchantCbTable);
            cbTableUpdated = true;

        } else if(!currCbTables.contains(merchantCbTable)) {
            String newCbTables = currCbTables+","+merchantCbTable;
            mLogger.debug("Setting new CB tables for customer: "+newCbTables+","+currCbTables);
            customer.setCashback_table(newCbTables);
            cbTableUpdated = true;
        }

        // update customer
        if(cbTableUpdated) {
            BackendOps.updateCustomer(customer);
        }

        // create new cashback object
        // intentionally doing it after updating customer for cashback table name
        return createCbObject(merchantId, customer);
    }

    private Cashback createCbObject(String merchantId, Customers customer) {
        Cashback cashback = new Cashback();

        // rowid_card - "customer card id"+"merchant id"+"terminal id"
        String cardId = customer.getMembership_card().getCard_id();
        //cashback.setRowid_card(cardId + merchantId);
        // rowid - "customer mobile no"+"merchant id"+"terminal id"
        cashback.setRowid(customer.getPrivate_id() + merchantId);

        cashback.setMerchant_id(merchantId);
        cashback.setCust_private_id(customer.getPrivate_id());

        cashback.setCb_credit(0);
        cashback.setCb_debit(0);
        cashback.setCl_credit(0);
        cashback.setCl_debit(0);
        cashback.setTotal_billed(0);
        cashback.setCb_billed(0);

        // not setting 'merchant' or 'customer'
        return BackendOps.saveCashback(cashback);
    }

    private String buildCustomerDetails(Customers customer) {
        // Build customer detail in below CSV format
        // size = 10 + 16 + 1 + 1 + 20 + 1 + 20 = 70 (round off to 128)
        // <mobile_num>,<acc_status>,<acc_status_reason>,<acc_status_update_time>,<card_id>,<card_status>,<card_status_update_time>

        StringBuilder sb = new StringBuilder(128);
        sb.append(customer.getMobile_num()).append(CommonConstants.CSV_DELIMETER)
                .append(customer.getAdmin_status()).append(CommonConstants.CSV_DELIMETER)
                .append(customer.getStatus_reason()).append(CommonConstants.CSV_DELIMETER);

        SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
        sdf.setTimeZone(TimeZone.getTimeZone(BackendConstants.TIMEZONE));
        sb.append(sdf.format(customer.getStatus_update_time())).append(CommonConstants.CSV_DELIMETER);

        CustomerCards card = customer.getMembership_card();
        sb.append(card.getCard_id()).append(CommonConstants.CSV_DELIMETER)
                .append(card.getStatus()).append(CommonConstants.CSV_DELIMETER)
                .append(sdf.format(card.getStatus_update_time()));

        mLogger.debug("Generated customer details: "+sb.toString());
        return sb.toString();
    }

    // Strip cashback object for information not needed by merchant app
    private void stripCashback(Cashback cashback) {
        cashback.setCust_private_id(null);
        //cashback.setCustomer(null);
        //cashback.setMerchant(null);
        cashback.setRowid(null);
        //cashback.setRowid_card(null);
    }

    private void rollbackRegister(String custId, CustomerCards card) {
        // TODO: add as 'Major' alarm - user to be removed later manually
        // rollback to not-usable state
        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
        try {
            //mLogger.debug("rollbackRegister: Before: "+ InvocationContext.asString());
            //mLogger.debug("rollbackRegister: Before: "+HeadersManager.getInstance().getHeaders().toString());
            //mLogger.flush();

            BackendOps.decrementCounterValue(DbConstantsBackend.CUSTOMER_ID_COUNTER);

            Customers customer = BackendOps.getCustomer(custId, BackendConstants.CUSTOMER_ID_MOBILE, false);
            customer.setAdmin_status(DbConstants.USER_STATUS_REG_ERROR);
            customer.setStatus_reason(DbConstants.REG_ERROR_ROLE_ASSIGN_FAILED);
            customer.setAdmin_remarks("Registration failed");
            customer.setMembership_card(null);
            BackendOps.updateCustomer(customer);

            // free up the card for next allocation
            card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_NEW);
            // TODO: set any other fields required
            BackendOps.saveCustomerCard(card);

        } catch(Exception ex) {
            mLogger.fatal("registerCustomer: Customer register Rollback failed: "+ex.toString());
            //TODO: raise critical alarm
            throw ex;
        }
    }

    private void changeCustomerCard(Customers customer, String newCardId, String reason) {
        // fetch new card record
        CustomerCards newCard = BackendOps.getCustomerCard(newCardId);
        CommonUtils.checkCardForAllocation(newCard);
        //TODO: enable this in final testing
        //if(!newCard.getMerchantId().equals(merchantId)) {
        //  return ResponseCodes.RESPONSE_CODE_QR_WRONG_MERCHANT;
        //}

        CustomerCards oldCard = customer.getMembership_card();
        mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = oldCard.getCard_id();

        // update 'customer' and 'CustomerCard' objects for new card
        newCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
        newCard.setStatus_update_time(new Date());
        customer.setCardId(newCard.getCard_id());
        customer.setMembership_card(newCard);

        // save updated customer object
        BackendOps.updateCustomer(customer);

        // update old card status
        try {
            oldCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_REMOVED);
            oldCard.setStatus_reason(reason);
            oldCard.setStatus_update_time(new Date());
            BackendOps.saveCustomerCard(oldCard);

        } catch (BackendlessException e) {
            // ignore as not considered as failure for whole 'changeCustomerCard' operation
            // but log as alarm for manual correction
            // TODO: raise alarm
            mLogger.error("Exception while updating old card status: "+e.toString());
            mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX]=BackendConstants.BACKEND_ERROR_OLDCARD_SAVE_FAILED;
        }

        // Send message to customer informing the same - ignore sent status
        String smsText = SmsHelper.buildNewCardSMS(customer.getMobile_num(), newCardId);
        if(SmsHelper.sendSMS(smsText, customer.getMobile_num(), mLogger)) {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
        }
    }

    private void changeCustomerMobile(BackendlessUser custUser, String newMobile) {
        Customers customer = (Customers) custUser.getProperty("customer");
        String oldMobile = customer.getMobile_num();

        // update mobile number
        custUser.setProperty("user_id", newMobile);
        customer.setMobile_num(newMobile);
        custUser.setProperty("customer", customer);
        BackendOps.updateUser(custUser);

        // Send message to customer informing the same - ignore sent status
        String smsText = SmsHelper.buildCustMobileChangeSMS( oldMobile, newMobile );
        if(SmsHelper.sendSMS(smsText, oldMobile+","+newMobile, mLogger)){
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
        };
    }

    private void resetCustomerPin(BackendlessUser custUser) {
        Customers customer = (Customers) custUser.getProperty("customer");
        // generate pin
        String newPin = CommonUtils.generateCustomerPIN();

        // update user account for the PIN
        custUser.setPassword(newPin);
        //TODO: encode PIN
        customer.setTxn_pin(newPin);
        custUser.setProperty("customer",customer);

        BackendOps.updateUser(custUser);

        // Send SMS through HTTP
        String smsText = SmsHelper.buildCustPwdResetSMS(customer.getMobile_num(), newPin);
        if( !SmsHelper.sendSMS(smsText, customer.getMobile_num(), mLogger)) {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
        }
    }
}

    /*
    public void changePassword(String userId, String oldPasswd, String newPasswd, String mobileNum) {
        initCommon();
        mLogger.debug("In changePassword: "+userId+": "+mobileNum);

        mLogger.debug("Before: "+InvocationContext.asString());
        mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

        // we are anyways gonna login next, so dont need this
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // Login to verify the old password
        // Note: afterLogin event handler will not get called - so 'trusted device' check will not happen
        // As event handlers are not called - for API calls made from server code.
        // In normal situation, this is not an issue - as user can call 'change password' only after login
        // However, in hacked situation, 'trusted device' check wont happen - ignoring this for now.
        BackendlessUser user = mBackendOps.loginUser(userId,oldPasswd);
        if(user==null) {
            //return mBackendOps.mLastOpStatus;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        mLogger.debug("After: "+InvocationContext.asString());
        mLogger.debug("After: "+HeadersManager.getInstance().getHeaders().toString());

        // check admin status
        String status = CommonUtils.checkMerchantStatus(merchant);
        if(status != null) {
            mBackendOps.logoutUser();
            CommonUtils.throwException(mLogger,status, "Merchant account is inactive", false);
        }

        // Change password
        user.setPassword(newPasswd);
        user = mBackendOps.updateUser(user);
        if(user==null) {
            //return mBackendOps.mLastOpStatus;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }

        // Send SMS through HTTP
        String smsText = buildPwdChangeSMS(userId);
        if( !SmsHelper.sendSMS(smsText, mobileNum) )
        {
            //TODO: add in alarms table
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "Failed to send password reset SMS: "+userId, false);
        }

        //mLogger.flush();
        //return BackendResponseCodes.BE_RESPONSE_NO_ERROR;
    }
    private String buildPwdChangeSMS(String userId) {
        return String.format(SmsConstants.SMS_PASSWD_CHANGED, CommonUtils.getHalfVisibleId(userId));
    }

    */


    /*
    public void changeCustomerCard(String customerId, String newCardId, String reason, String otp, String pin) {
        initCommon();

        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "changeCustomerCard";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = customerId + BackendConstants.BACKEND_EDR_SUB_DELIMETER +
                newCardId + BackendConstants.BACKEND_EDR_SUB_DELIMETER +
                reason;

        boolean positiveException = false;

        try {
            // Fetch merchant
            Merchants merchant = (Merchants) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    true, DbConstants.USER_TYPE_MERCHANT, mEdr);

            // Fetch customer
            Customers customer = BackendOps.getCustomer(customerId, BackendConstants.CUSTOMER_ID_MOBILE);
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getMobile_num();
            String cardId = customer.getMembership_card().getCard_id();
            mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = cardId;
            // check if customer is enabled
            CommonUtils.checkCustomerStatus(customer);

            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // Verify PIN
                if (!customer.getTxn_pin().equals(pin)) {
                    CommonUtils.handleWrongAttempt(customerId, customer, DbConstants.USER_TYPE_CUSTOMER, DbConstantsBackend.ATTEMPT_TYPE_USER_PIN);
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_PIN, "Wrong PIN attempt: " + customer.getMobile_num());
                }

                // Generate OTP and send SMS
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(customer.getMobile_num());
                newOtp.setMobile_num(customer.getMobile_num());
                newOtp.setOpcode(DbConstants.CUSTOMER_OP_NEW_CARD);
                BackendOps.generateOtp(newOtp);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(customerId, otp);
                // fetch new card record
                CustomerCards newCard = BackendOps.getCustomerCard(newCardId);
                CommonUtils.checkCardForAllocation(newCard);
                //TODO: enable this in final testing
                //if(!newCard.getMerchantId().equals(merchantId)) {
                //  return ResponseCodes.RESPONSE_CODE_QR_WRONG_MERCHANT;
                //}

                CustomerCards oldCard = customer.getMembership_card();
                mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = oldCard.getCard_id();

                // update 'customer' and 'CustomerCard' objects for new card
                newCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
                newCard.setStatus_update_time(new Date());
                customer.setCardId(newCard.getCard_id());
                customer.setMembership_card(newCard);

                // save updated customer object
                BackendOps.updateCustomer(customer);

                // update old card status
                try {
                    oldCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_REMOVED);
                    oldCard.setStatus_reason(reason);
                    oldCard.setStatus_update_time(new Date());
                    BackendOps.saveCustomerCard(oldCard);

                } catch (BackendlessException e) {
                    // ignore as not considered as failure for whole 'changeCustomerCard' operation
                    // but log as alarm for manual correction
                    // TODO: raise alarm
                    mLogger.error("Exception while updating old card status: "+e.toString());
                    mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX]=BackendConstants.BACKEND_ERROR_OLDCARD_SAVE_FAILED;
                }

                // add to customer ops table - for records purpose
                try {
                    CustomerOps op = new CustomerOps();
                    op.setRequestor_id(merchant.getAuto_id());
                    op.setOp_status(DbConstants.CUSTOMER_OP_NEW_CARD);
                    op.setQr_card(oldCard.getCard_id());
                    op.setExtra_op_params(reason);
                    BackendOps.saveCustomerOp(op);
                } catch(Exception e) {
                    // ignore error
                    mLogger.error("Customer change card op record save failed: "+e.toString());
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch (Exception e) {
            handleException(e,positiveException);
            throw e;
        } finally {
            finalHandling(startTime);
        }
    }*/

