package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import com.mytest.constants.*;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by adgangwa on 15-05-2016.
 */
public class MerchantServices implements IBackendlessService {

    private Logger mLogger;
    private BackendOps mBackendOps;

    /*
     * Public methods: Backend REST APIs
     */
    public void changeMobile(String currentMobile, String newMobile, String otp) {
        initCommon();
        mLogger.debug("In changeMobile: "+currentMobile+","+newMobile);

        // this will ensure that backend operations are executed, as logged-in user who called this api using generated SDK
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // Fetch merchant
        BackendlessUser user = mBackendOps.getCurrentMerchantUser();
        if(user == null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        if(otp==null || otp.isEmpty()) {
            // First run, generate OTP if all fine

            // check if merchant is enabled
            String status = CommonUtils.checkMerchantStatus(merchant);
            if( status != null) {
                CommonUtils.throwException(mLogger,status, "Merchant account is not active", false);
            }

            // Validate based on given current number
            if(!merchant.getMobile_num().equals(currentMobile)) {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "Wrong old mobile number", false);
            }

            // Generate OTP to verify new mobile number
            AllOtp newOtp = new AllOtp();
            newOtp.setUser_id(merchant.getAuto_id());
            newOtp.setMobile_num(newMobile);
            newOtp.setOpcode(DbConstants.MERCHANT_OP_CHANGE_MOBILE);
            newOtp = mBackendOps.generateOtp(newOtp);
            if(newOtp == null) {
                // failed to generate otp
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_OTP_GENERATE_FAILED, "OTP generate failed", false);
            }

            // OTP generated successfully - return exception to indicate so
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "OTP generated successfully", true);

        } else {
            // Second run, as OTP available
            AllOtp fetchedOtp = mBackendOps.fetchOtp(merchant.getAuto_id());
            if( fetchedOtp == null ||
                    !mBackendOps.validateOtp(fetchedOtp, otp) ) {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_WRONG_OTP, "Wrong OTP provided: "+otp, false);
            }

            mLogger.debug("OTP matched for given merchant operation: "+merchant.getAuto_id());

            // Update with new mobile number
            String oldMobile = merchant.getMobile_num();
            merchant.setMobile_num(newMobile);
            merchant = mBackendOps.updateMerchant(merchant);
            if(merchant == null) {
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            }

            // save in merchant ops table
            MerchantOps merchantops = new MerchantOps();
            merchantops.setMerchant_id(merchant.getAuto_id());
            merchantops.setOp_code(DbConstants.MERCHANT_OP_CHANGE_MOBILE);
            merchantops.setMobile_num(oldMobile);
            merchantops.setExtra_op_params(newMobile);
            merchantops.setOp_status(DbConstants.MERCHANT_OP_STATUS_COMPLETE);
            if( mBackendOps.addMerchantOp(merchantops) == null )
            {
                //TODO: raise alarm - but dont return error
            } else {
                mLogger.debug("Processed mobile change for: "+merchant.getAuto_id());
            }

            // Send SMS on old and new mobile - ignore sent status
            String smsText = buildMobileChangeSMS(oldMobile, newMobile);
            SmsHelper.sendSMS(smsText, oldMobile+","+newMobile);
        }
    }


    public void updateSettings(String cbRate, boolean addClEnabled, String email) {
        initCommon();
        mLogger.debug("In updateSettings: "+cbRate+": "+addClEnabled);

        BackendlessUser user = mBackendOps.getCurrentMerchantUser();
        if(user == null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        // check if merchant is enabled
        String status = CommonUtils.checkMerchantStatus(merchant);
        if( status != null) {
            CommonUtils.throwException(mLogger,status, "Merchant account is not active", false);
        }

        // update settings
        merchant.setCb_rate(cbRate);
        merchant.setCl_add_enable(addClEnabled);
        merchant.setEmail(email);
        if( mBackendOps.updateMerchant(merchant)==null ) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
    }

    public Cashback registerCustomer(String merchantId, String merchantCbTable, String customerMobile, String name, String cardId) {
        initCommon();
        mLogger.debug("In registerCustomer: "+customerMobile+": "+cardId);

        mLogger.debug("Before: "+InvocationContext.asString());
        mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

        // assume role of calling user
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        //mLogger.debug("After: "+InvocationContext.asString());
        //mLogger.debug("After: "+HeadersManager.getInstance().getHeaders().toString());

        // fetch customer card object
        CustomerCards card = mBackendOps.getCustomerCard(cardId);
        if(card == null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        } else {
            String status = CommonUtils.getCardStatusForAllocation(card);
            if(status != null) {
                CommonUtils.throwException(mLogger,status, "Invalid customer card", false);
            }
            // TODO: enable in production
            /*
            if(!card.getMerchant_id().equals(merchantId)) {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_CARD_WRONG_MERCHANT, "");
            }*/
        }

        // Create customer object and register
        Customers customer = createCustomer(customerMobile, name, merchantCbTable);
        if(customer==null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        // set membership card
        card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
        card.setStatus_update_time(new Date());
        customer.setMembership_card(card);

        BackendlessUser customerUser = new BackendlessUser();
        customerUser.setProperty("user_id", customerMobile);
        customerUser.setPassword(customer.getTxn_pin());
        customerUser.setProperty("user_type", DbConstants.USER_TYPE_CUSTOMER);
        customerUser.setProperty("customer",customer);

        customerUser = mBackendOps.registerUser(customerUser);
        if(customerUser == null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        customer = (Customers) customerUser.getProperty("customer");

        // Send sms to the customer with PIN
        String pin = customer.getTxn_pin();
        String smsText = String.format(SmsConstants.SMS_FIRST_PIN, customerMobile, pin);
        // Send SMS through HTTP
        if (!SmsHelper.sendSMS(smsText, customerMobile)) {
            // TODO: write to alarm table for retry later
        }

        // create cashback also - to avoid another call to 'getCashback' from merchant
        Cashback cashback = createCbObject(merchantId, customer);
        // save cashback object
        if(mBackendOps.saveCashback(cashback) == null) {
            //TODO: add as major alarm
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_REGISTER_SUCCESS_CREATE_CB_FAILED, "", false);
            //throw new BackendlessException( "", mBackendOps.mLastOpStatus );
        }

        // Add 'customer details' in the cashback object to be returned
        // these details are not stored in DB along with cashback object
        String customerDetails = buildCustomerDetails(customer);
        cashback.setCustomer_details(customerDetails);
        // remove 'not needed sensitive' fields from cashback object
        stripCashback(cashback);

        //Backendless.Logging.flush();
        return cashback;
    }


    public Cashback getCashback(String merchantId, String merchantCbTable, String customerId, boolean mobileIsCustomerId) {
        initCommon();
        mLogger.debug("In getCashback: "+merchantId+": "+customerId);

        mLogger.debug("Before: "+InvocationContext.asString());
        mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

        // also assume role of calling user
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        //mLogger.debug("After: "+InvocationContext.asString());
        //mLogger.debug("After: "+HeadersManager.getInstance().getHeaders().toString());

        // Fetch customer
        Customers customer = mBackendOps.getCustomer(customerId, mobileIsCustomerId);
        if(customer==null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            //throw new BackendlessException( "No such customer", BackendResponseCodes.BE_ERROR_NO_SUCH_USER );
        }

        // Cashback details to be returned - even if customer account/card is disabled/locked
        // so not checking for customer account/card status

        // Create where clause to fetch cashback
        String whereClause = null;
        String rowid = customerId + merchantId;
        if(mobileIsCustomerId) {
            whereClause = "rowid = '"+rowid+"'";
        } else {
            whereClause = "rowid_card = '"+rowid+"'";
        }

        // Fetch cashback record
        Cashback cashback = null;
        ArrayList<Cashback> data = mBackendOps.fetchCashback(whereClause, merchantCbTable);
        if(data!=null) {
            cashback = data.get(0);
        } else {
            // create new cashback object
            cashback = handleCashbackCreate(merchantId, merchantCbTable, customer);
            if(cashback==null) {
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            }
        }

        // Add 'customer details' in the cashback object to be returned
        // these details are not stored in DB along with cashback object
        String customerDetails = buildCustomerDetails(customer);
        cashback.setCustomer_details(customerDetails);
        // remove 'not needed sensitive' fields from cashback object
        stripCashback(cashback);

        //Backendless.Logging.flush();
        return cashback;
    }

    public MerchantStats getMerchantStats(String merchantId) {
        initCommon();
        mLogger.debug("In getMerchantStats: "+merchantId);

        // assume role of calling user
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // Fetch merchant
        Merchants merchant = mBackendOps.getMerchant(merchantId, false);
        if(merchant==null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        // check if merchant is enabled
        String status = CommonUtils.checkMerchantStatus(merchant);
        if( status != null) {
            CommonUtils.throwException(mLogger,status, "Merchant account is not active", false);
        }

        // create new stats object
        // fetch merchant stat object, if exists
        MerchantStats stats = mBackendOps.fetchMerchantStats(merchantId);
        if(stats==null) {
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
        ArrayList<Cashback> data = mBackendOps.fetchCashback("merchant_id = '"+merchantId+"'", merchant.getCashback_table());
        if(data!=null) {
            // loop on all cashback objects and calculate stats
            mLogger.debug("Fetched cashback records: "+merchantId+", "+data.size());
            for (int k = 0; k < data.size(); k++) {
                Cashback cb = data.get(k);

                // update customer counts
                // no need to check for 'debit' amount - as 'credit' amount is total amount and includes debit amount too
                if(cb.getCb_credit()>0 && cb.getCl_credit()>0) {
                    stats.cust_cnt_cb_and_cash++;
                } else if(cb.getCb_credit()>0) {
                    stats.cust_cnt_cb++;
                } else if(cb.getCl_credit()>0) {
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

                stats.setUpdate_time(new Date());
            }

            // save stats object - don't bother about return status
            mBackendOps.saveMerchantStats(stats);
        } else {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }

        //Backendless.Logging.flush();
        return stats;
    }

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


    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.MerchantServices");
        mBackendOps = new BackendOps(mLogger);
        CommonUtils.initTableToClassMappings();
    }

    private Customers createCustomer(String mobileNum, String name, String merchantCbTable) {
        Customers customer = new Customers();
        customer.setMobile_num(mobileNum);
        customer.setName(name);
        customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
        customer.setStatus_reason(DbConstants.ENABLED_ACTIVE);
        customer.setStatus_update_time(new Date());
        // new record - so set it directly
        customer.setCashback_table(merchantCbTable);

        // get customer counter value and encode the same to get customer private id
        Double customerCnt =  mBackendOps.fetchCounterValue(DbConstants.CUSTOMER_ID_COUNTER);
        if(customerCnt == null) {
            return null;
        }
        mLogger.debug("Fetched customer cnt: "+customerCnt.longValue());
        String private_id = Base61.fromBase10(customerCnt.longValue());
        mLogger.debug("Generated private id: "+private_id);
        customer.setPrivate_id(private_id);

        // generate and set PIN
        String pin = CommonUtils.generateCustomerPIN();
        mLogger.debug("Generated PIN: "+pin);
        customer.setTxn_pin(pin);

        return customer;
    }

    private Cashback handleCashbackCreate(String merchantId, String merchantCbTable, Customers customer) {
        // create new cashback object
        Cashback cashback = createCbObject(merchantId, customer);
        // save cashback object
        if(mBackendOps.saveCashback(cashback) == null) {
            return null;
            //CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg);
            //throw new BackendlessException( "", mBackendOps.mLastOpStatus );
        }

        // Add cashback table name (of this merchant's) in customer record, if not already added (due to some other merchant)
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
            if(mBackendOps.updateCustomer(customer) == null) {
                return null;
                //CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg);
                //throw new BackendlessException( "", mBackendOps.mLastOpStatus );
            }
        }
        return cashback;
    }

    private Cashback createCbObject(String merchantId, Customers customer) {
        Cashback cashback = new Cashback();

        // rowid_card - "customer card id"+"merchant id"+"terminal id"
        String cardId = customer.getMembership_card().getCard_id();
        if(cardId != null) {
            cashback.setRowid_card(cardId + merchantId);
        } else {
            mLogger.error("customer card object is not available");
            return null;
        }

        // rowid - "customer mobile no"+"merchant id"+"terminal id"
        cashback.setRowid(customer.getMobile_num() + merchantId);

        cashback.setMerchant_id(merchantId);
        cashback.setCust_private_id(customer.getPrivate_id());

        cashback.setCb_credit(0);
        cashback.setCb_debit(0);
        cashback.setCl_credit(0);
        cashback.setCl_debit(0);
        cashback.setTotal_billed(0);
        cashback.setCb_billed(0);

        // not setting 'merchant' or 'customer'
        return cashback;
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
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        sb.append(sdf.format(customer.getStatus_update_time())).append(CommonConstants.CSV_DELIMETER);

        CustomerCards card = customer.getMembership_card();
        sb.append(card.getCard_id()).append(CommonConstants.CSV_DELIMETER)
                .append(card.getStatus()).append(CommonConstants.CSV_DELIMETER)
                .append(sdf.format(card.getStatus_update_time()));

        /*
        if(customer.getAdmin_status() == DbConstants.USER_STATUS_ACTIVE) {
            // next 2 fields empty
            sb.append(CommonConstants.CSV_DELIMETER)
                    .append(CommonConstants.CSV_DELIMETER);
        } else {
            sb.append(customer.getStatus_reason()).append(CommonConstants.CSV_DELIMETER);

            if(customer.getStatus_update_time() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                sb.append(sdf.format(customer.getStatus_update_time())).append(CommonConstants.CSV_DELIMETER);
            } else {
                sb.append(CommonConstants.CSV_DELIMETER);
            }
        }

        CustomerCards card = customer.getMembership_card();
        sb.append(card.getCard_id()).append(CommonConstants.CSV_DELIMETER)
                .append(card.getStatus());

        if(card.getStatus() == DbConstants.CUSTOMER_CARD_STATUS_BLOCKED &&
                card.getStatus_update_time() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
            sb.append(sdf.format(card.getStatus_update_time())).append(CommonConstants.CSV_DELIMETER);
        } else {
            sb.append(CommonConstants.CSV_DELIMETER);
        }*/

        mLogger.debug("Generated customer details: "+sb.toString());
        return sb.toString();
    }

    // Strip cashback object for information not needed by merchant app
    private void stripCashback(Cashback cashback) {
        cashback.setCust_private_id(null);
        //cashback.setCustomer(null);
        //cashback.setMerchant(null);
        cashback.setRowid(null);
        cashback.setRowid_card(null);
    }

    private String buildPwdChangeSMS(String userId) {
        return String.format(SmsConstants.SMS_PASSWD_CHANGED, CommonUtils.getHalfVisibleId(userId));
    }

    private String buildMobileChangeSMS(String userId, String mobile_num) {
        return String.format(SmsConstants.SMS_MOBILE_CHANGE_MERCHANT, CommonUtils.getHalfVisibleId(userId), CommonUtils.getHalfVisibleId(mobile_num));
    }

}
