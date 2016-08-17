package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import com.mytest.constants.*;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.timers.TxnArchiver;
import com.mytest.utilities.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by adgangwa on 15-05-2016.
 */
public class MerchantServices implements IBackendlessService {

    private Logger mLogger;

    /*
     * Public methods: Backend REST APIs
     */
    public void changeMobile(String currentMobile, String newMobile, String otp) {
        initCommon();
        boolean positiveException = false;

        try {
            mLogger.debug("In changeMobile: " + currentMobile + "," + newMobile);
            newMobile = CommonUtils.addMobileCC(newMobile);
            currentMobile = CommonUtils.addMobileCC(currentMobile);

            // Fetch merchant
            BackendlessUser user = BackendOps.fetchUserByObjectId(InvocationContext.getUserId(), DbConstants.USER_TYPE_MERCHANT);
            Merchants merchant = (Merchants) user.getProperty("merchant");
            // check if merchant is enabled
            CommonUtils.checkMerchantStatus(merchant);

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
                BackendOps.generateOtp(newOtp);

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

                // record in merchant ops table
                MerchantOps merchantops = new MerchantOps();
                merchantops.setMerchant_id(merchant.getAuto_id());
                merchantops.setOp_code(DbConstantsBackend.MERCHANT_OP_CHANGE_MOBILE);
                merchantops.setMobile_num(oldMobile);
                merchantops.setExtra_op_params(newMobile);
                merchantops.setOp_status(DbConstantsBackend.MERCHANT_OP_STATUS_COMPLETE);
                try {
                    BackendOps.addMerchantOp(merchantops);
                } catch(Exception e) {
                    // ignore error
                    mLogger.error("changeMobile: Exception while adding merchant operation: "+e.toString());
                }

                mLogger.debug("Processed mobile change for: " + merchant.getAuto_id());

                // Send SMS on old and new mobile - ignore sent status
                String smsText = buildMobileChangeSMS(oldMobile, newMobile);
                // not checking the status
                SmsHelper.sendSMS(smsText, oldMobile + "," + newMobile);
            }
        } catch(Exception e) {
            if(!positiveException) {
                mLogger.error("Exception in MerchantServices:changeMobile: "+e.toString());
            }
            throw e;
        }
    }


    public void updateSettings(String cbRate, boolean addClEnabled, String email) {
        initCommon();
        try {
            mLogger.debug("In updateSettings: "+cbRate+": "+addClEnabled+": "+email);
            mLogger.debug("Before context: "+InvocationContext.asString());
            mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

            BackendlessUser user = BackendOps.fetchUserByObjectId(InvocationContext.getUserId(), DbConstants.USER_TYPE_MERCHANT);
            Merchants merchant = (Merchants) user.getProperty("merchant");

            // check if merchant is enabled
            CommonUtils.checkMerchantStatus(merchant);

            // update settings
            merchant.setCb_rate(cbRate);
            merchant.setCl_add_enable(addClEnabled);
            merchant.setEmail(email);
            BackendOps.updateMerchant(merchant);

        } catch (Exception e) {
            mLogger.error("Exception in updateSettings: "+e.toString());
            Backendless.Logging.flush();
            throw e;
        }
    }

    public Cashback registerCustomer(String customerMobile, String name, String cardId) {
        initCommon();
        Customers customer = null;
        CustomerCards card = null;
        BackendlessUser customerUser = null;

        try {
            mLogger.debug("In registerCustomer: " + customerMobile + ": " + cardId);
            customerMobile = CommonUtils.addMobileCC(customerMobile);

            // Fetch merchant
            BackendlessUser user = BackendOps.fetchUserByObjectId(InvocationContext.getUserId(), DbConstants.USER_TYPE_MERCHANT);
            Merchants merchant = (Merchants) user.getProperty("merchant");
            // check if merchant is enabled
            CommonUtils.checkMerchantStatus(merchant);

            // fetch customer card object
            card = BackendOps.getCustomerCard(cardId);
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
            customerUser.setProperty("customer", customer);

            customerUser = BackendOps.registerUser(customerUser);
            try {
                customer = (Customers) customerUser.getProperty("customer");
                // assign custom role to it
                BackendOps.assignRole(customerMobile, BackendConstants.ROLE_CUSTOMER);
            } catch(Exception e) {
                // TODO: add as 'Major' alarm - user to be removed later manually
                // rollback to not-usable state
                customer.setAdmin_status(DbConstants.USER_STATUS_REG_ERROR);
                customer.setStatus_reason(DbConstants.REG_ERROR_ROLE_ASSIGN_FAILED);
                customer.setMembership_card(null);
                customerUser.setProperty("customer", customer);
                try {
                    BackendOps.updateUser(customerUser);
                } catch(Exception ex) {
                    mLogger.fatal("registerCustomer: User Rollback failed: "+ex.toString());
                    // TODO: raise critical alarm for manual correction
                }

                // free up the card for next allocation
                card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_NEW);
                // TODO: set any other fields required
                try {
                    BackendOps.saveCustomerCard(card);
                } catch(Exception ex) {
                    mLogger.fatal("registerCustomer: Customefr card Rollback failed: "+ex.toString());
                    // TODO: raise critical alarm for manual correction
                }

                throw e;
            }

            // Send sms to the customer with PIN
            String smsText = String.format(SmsConstants.SMS_FIRST_PIN_CUSTOMER, customerMobile, customer.getTxn_pin());
            if (!SmsHelper.sendSMS(smsText, customerMobile)) {
                // Don't consider the reg operation as failed
                // TODO: write to alarm table for retry later
            }

            try {
                // create cashback also - to avoid another call to 'getCashback' from merchant
                Cashback cashback = createCbObject(merchant.getAuto_id(), customer);
                // Add 'customer details' in the cashback object to be returned
                // these details are not stored in DB along with cashback object
                cashback.setCustomer_details(buildCustomerDetails(customer));
                // remove 'not needed sensitive' fields from cashback object
                stripCashback(cashback);
                return cashback;

            } catch(Exception e) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_REGISTER_SUCCESS_CREATE_CB_FAILED, "");
            }

            //Backendless.Logging.flush();

        } catch (Exception e) {
            mLogger.error("Exception in registerCustomer: "+e.toString());
            Backendless.Logging.flush();
            throw e;
        }
    }


    // Taking 'merchantId' and 'merchantCbTable' values from caller is a bit of security risk
    // as it will allow any logged-in merchant - to read (not update) cb details of other merchants too
    // but ignoring this for now - to keep this API as fast as possible - as this will be most called API
    public Cashback getCashback(String merchantId, String merchantCbTable, String customerId) {
        initCommon();
        try {
            mLogger.debug("In getCashback: " + merchantId + ": " + customerId);

            int customerIdType = CommonUtils.customerIdMobile(customerId) ? BackendConstants.CUSTOMER_ID_MOBILE : BackendConstants.CUSTOMER_ID_CARD;

            // TODO: see how to avoid fetching customer in common scenario - i.e. when cb object is available and customer status is active
            // one way is to put all mandatory info about customer in cb object only

            // Fetch customer using mobile or cardId
            Customers customer = BackendOps.getCustomer(customerId, customerIdType);

            // Fetch cashback record
            // Create where clause to fetch cashback
            //String colName = (customerIdType==BackendConstants.CUSTOMER_ID_MOBILE) ? "rowid" : "rowid_card";
            String whereClause = "rowid = '" + customer.getPrivate_id()+merchantId + "'";
            ArrayList<Cashback> data = BackendOps.fetchCashback(whereClause, merchantCbTable);
            Cashback cashback = null;
            if(data == null) {
                cashback = handleCashbackCreate(merchantId, merchantCbTable, customer);
            } else {
                cashback = data.get(0);
            }

            // Cashback details to be returned - even if customer account/card is disabled/locked
            // so not checking for customer account/card status

            // Add 'customer details' in the cashback object to be returned
            // these details are not stored in DB along with cashback object
            cashback.setCustomer_details(buildCustomerDetails(customer));
            stripCashback(cashback);

            //Backendless.Logging.flush();
            return cashback;

        } catch (Exception e) {
            mLogger.error("Exception in getCashback: "+e.toString());
            Backendless.Logging.flush();
            throw e;
        }
    }

    public MerchantStats getMerchantStats() {
        initCommon();
        try {
            mLogger.debug("In getMerchantStats");

            // Fetch merchant
            BackendlessUser user = BackendOps.fetchUserByObjectId(InvocationContext.getUserId(), DbConstants.USER_TYPE_MERCHANT);
            Merchants merchant = (Merchants) user.getProperty("merchant");
            String merchantId = merchant.getAuto_id();

            // not checking for merchant account status

            // create new stats object
            // fetch merchant stat object, if exists
            MerchantStats stats = null;
            try {
                stats = BackendOps.fetchMerchantStats(merchantId);
            } catch(BackendlessException e) {
                if(!e.getCode().equals(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND)) {
                    throw e;
                }
            }
            // create object if not already available
            if (stats == null) {
                stats = new MerchantStats();
                stats.setMerchant_id(merchantId);
            } else {
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
            }

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

            return stats;
        } catch (Exception e) {
            mLogger.error("Exception in getMerchantStats: "+e.toString());
            Backendless.Logging.flush();
            throw e;
        }
    }

    public void archiveTxns() {
        initCommon();
        try {
            mLogger.debug("In archiveTxns");

            // Fetch merchant
            BackendlessUser user = BackendOps.fetchUserByObjectId(InvocationContext.getUserId(), DbConstants.USER_TYPE_MERCHANT);
            Merchants merchant = (Merchants) user.getProperty("merchant");
            // not checking for merchant account status

            // archive txns
            TxnArchiver archiver = new TxnArchiver(mLogger, merchant, InvocationContext.getUserToken());
            archiver.archiveMerchantTxns();
            Backendless.Logging.flush();

        } catch (Exception e) {
            mLogger.error("Exception in archiveTxns: "+e.toString());
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
        mLogger = Backendless.Logging.getLogger("com.mytest.services.MerchantServices");
        //mBackendOps = new BackendOps(mLogger);
        CommonUtils.initTableToClassMappings();
    }

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
        sb.append(CommonUtils.removeMobileCC(customer.getMobile_num())).append(CommonConstants.CSV_DELIMETER)
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

    private String buildMobileChangeSMS(String userId, String mobile_num) {
        return String.format(SmsConstants.SMS_MOBILE_CHANGE_MERCHANT, CommonUtils.getHalfVisibleId(userId), CommonUtils.getHalfVisibleId(mobile_num));
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

        //Backendless.Logging.flush();
        //return BackendResponseCodes.BE_RESPONSE_NO_ERROR;
    }
    private String buildPwdChangeSMS(String userId) {
        return String.format(SmsConstants.SMS_PASSWD_CHANGED, CommonUtils.getHalfVisibleId(userId));
    }

    */


