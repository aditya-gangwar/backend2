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
    public Cashback registerCustomer(String merchantId, String merchantCbTable, String customerMobile, String name, String cardId) {
        initCommon();
        mLogger.debug("In registerCustomer: "+customerMobile+": "+cardId);

        // assume role of calling user
        HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // fetch customer card object
        CustomerCards card = mBackendOps.getCustomerCard(cardId);
        if(card == null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        } else {
            String status = CommonUtils.checkCustomerCardStatus(card);
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

        Backendless.Logging.flush();
        return cashback;
    }


    public Cashback getCashback(String merchantId, String merchantCbTable, String customerId, boolean mobileIsCustomerId) {
        initCommon();
        mLogger.debug("In getCashback: "+merchantId+": "+customerId);

        // assume role of calling user
        HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // Fetch customer
        Customers customer = mBackendOps.getCustomer(customerId, mobileIsCustomerId);
        if(customer==null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            //throw new BackendlessException( "No such customer", BackendResponseCodes.BE_ERROR_NO_SUCH_USER );
        }

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
        ArrayList<Cashback> data = mBackendOps.fetchCashback(whereClause, false, merchantCbTable);
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

        Backendless.Logging.flush();
        return cashback;
    }


    public void setDeviceForLogin(String loginId, String deviceInfo, String rcvdOtp) {
        initCommon();

        if(deviceInfo==null || deviceInfo.isEmpty()) {
            //return BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA;
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA, "Invalid input data: deviceInfo", false);
        }

        mLogger.debug("In setDeviceForLogin: "+loginId+": "+deviceInfo);

        // deviceInfo format: <device id>,<manufacturer>,<model>,<os version>
        String[] csvFields = deviceInfo.split(CommonConstants.CSV_DELIMETER);
        String deviceId = csvFields[0];

        BackendlessUser user = mBackendOps.fetchUser(loginId, DbConstants.USER_TYPE_MERCHANT);
        if(user==null) {
            //return BackendResponseCodes.BE_ERROR_NO_SUCH_USER;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        // check admin status
        String status = CommonUtils.checkMerchantStatus(merchant);
        if(status != null) {
            //return status;
            CommonUtils.throwException(mLogger,status, "Merchant status not active", false);
        }

        boolean matched = false;
        if(rcvdOtp==null || rcvdOtp.isEmpty()) {
            // first run - as did not rcv OTP

            // check if given device id matches trusted list
            List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
            int cnt = 0;
            if(trustedDevices != null) {
                cnt = trustedDevices.size();
                for (MerchantDevice device : trustedDevices) {
                    if(device.getDevice_id().equals(deviceId)) {
                        matched = true;
                        break;
                    }
                }
            }

            // generate OTP if device not matched
            if( !matched ) {
                // Check for max devices allowed per user
                if(cnt >= CommonConstants.MAX_DEVICES_PER_MERCHANT) {
                    //return BackendResponseCodes.BE_ERROR_TRUSTED_DEVICE_LIMIT_RCHD;
                    CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_TRUSTED_DEVICE_LIMIT_RCHD, "Trusted device limit reached", false);
                }
                // First login for this - generate OTP
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(loginId);
                newOtp.setMobile_num(merchant.getMobile_num());
                newOtp.setOpcode(DbConstants.MERCHANT_OP_NEW_DEVICE_LOGIN);
                newOtp = mBackendOps.generateOtp(newOtp);
                if(newOtp == null) {
                    //return BackendResponseCodes.BE_ERROR_OTP_GENERATE_FAILED;
                    CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_OTP_GENERATE_FAILED, mBackendOps.mLastOpStatus, false);
                } else {
                    //return BackendResponseCodes.BE_ERROR_OTP_GENERATED;
                    CommonUtils.throwException(mLogger,BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "OTP generated successfully", true);
                }
            }

        } else {
            // second run - as rcvd otp
            // update device only if OTP matches
            AllOtp fetchedOtp = mBackendOps.fetchOtp(loginId);
            if( fetchedOtp == null ||
                    !mBackendOps.validateOtp(fetchedOtp, rcvdOtp) ) {
                //return BackendResponseCodes.BE_ERROR_WRONG_OTP;
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_WRONG_OTP, "Wrong OTP value", false);
            }
        }

        // If here, means either 'device matched' or 'new device and otp matched'
        // Update device Info in merchant object
        merchant.setTempDevId(deviceInfo);
        user.setProperty("merchant",merchant);
        if( mBackendOps.updateUser(user)==null ) {
            //return BackendResponseCodes.BE_ERROR_GENERAL;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }

        Backendless.Logging.flush();
        //return BackendResponseCodes.BE_RESPONSE_NO_ERROR;
    }


    public void changePassword(String userId, String oldPasswd, String newPasswd, String mobileNum) {
        initCommon();
        mLogger.debug("In changePassword: "+userId+": "+mobileNum);

        // we are anyways gonna login next, so dont need this
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // Login to verify the old password
        // Note: afterLogin event handler will not get called - so 'trusted device' check will not happen
        // In normal situation, this is not an issue - as user can call 'change password' only after login
        // However, in hacked situation, 'trusted device' check wont happen - ignoring this for now.
        BackendlessUser user = mBackendOps.loginUser(userId,oldPasswd);
        if(user==null) {
            //return mBackendOps.mLastOpStatus;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

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

        Backendless.Logging.flush();
        //return BackendResponseCodes.BE_RESPONSE_NO_ERROR;
    }

    public void resetMerchantPwd(String userId, String deviceId, String brandName) {
        initCommon();
        mLogger.debug("In resetMerchantPwd: "+userId+": "+deviceId);

        // not required, as supposed to be called by user without logging in (forget password case)
        // this will ensure that backend operations are executed, as logged-in user who called this api using generated SDK
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // fetch user with the given id with related merchant object
        BackendlessUser user = mBackendOps.fetchUser(userId, DbConstants.USER_TYPE_MERCHANT);
        if(user==null) {
            //return mBackendOps.mLastOpStatus;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        // check admin status
        String status = CommonUtils.checkMerchantStatus(merchant);
        if(status != null) {
            //return status;
            CommonUtils.throwException(mLogger,status, "Merchant account not active", false);
        }

        // Check if from trusted device
        // don't check for first time after merchant is registered
        if(merchant.getAdmin_status() != DbConstants.USER_STATUS_NEW_REGISTERED) {
            boolean matched = false;
            List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
            if(trustedDevices != null &&
                    (deviceId != null && !deviceId.isEmpty())) {
                for (MerchantDevice device : trustedDevices) {
                    if(device.getDevice_id().equals(deviceId)) {
                        matched = true;
                        break;
                    }
                }
            }
            if(!matched) {
                //return BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE;
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "This is not trusted device", false);
            }
        }

        // check for 'extra verification'
        String name = merchant.getName();
        if(name==null || !name.equalsIgnoreCase(brandName)) {

            if( CommonUtils.handleMerchantWrongAttempt(mBackendOps, merchant, DbConstants.ATTEMPT_TYPE_PASSWORD_RESET) ) {

                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD,
                        "Merchant wrong password attempt limit reached"+merchant.getAuto_id(), false);
            } else {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED,
                        "Merchant password reset verification failed"+merchant.getAuto_id(), false);
            }
        }

        if(merchant.getAdmin_status() == DbConstants.USER_STATUS_NEW_REGISTERED) {
            String error = handlePasswdResetImmediate(user, merchant);
            if( error != null) {
                mLogger.error("Failed to process merchant reset password operation: "+merchant.getAuto_id()+", "+error);
                //return error;
                CommonUtils.throwException(mLogger,error, "Error in handlePasswdResetImmediate", false);
            } else {
                mLogger.debug("Processed passwd reset op for: "+merchant.getAuto_id());
            }
        } else {
            // create row in MerchantOps table
            if( mBackendOps.addMerchantOp(DbConstants.MERCHANT_OP_RESET_PASSWD, merchant) == null )
            {
                //return mBackendOps.mLastOpStatus;
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            } else {
                mLogger.debug("Processed passwd reset op for: "+merchant.getAuto_id());
                //return BackendResponseCodes.BE_RESPONSE_OP_SCHEDULED;
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_RESPONSE_OP_SCHEDULED, "Merchant reset scheduled", true);
            }
        }

        Backendless.Logging.flush();
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

    private String handlePasswdResetImmediate(BackendlessUser user, Merchants merchant) {

        // generate password
        String passwd = CommonUtils.generateMerchantPassword();
        mLogger.debug("Merchant Password: "+passwd);

        // update user account for the password
        user.setPassword(passwd);
        // delete any row, if exists, of earlier wrong attempts
        /*
        WrongAttempts attempt = mBackendOps.fetchWrongAttempts(merchant.getAuto_id(), DbConstants.ATTEMPT_TYPE_PASSWORD_RESET);
        if(attempt!=null) {
            mBackendOps.deleteWrongAttempt(attempt);
        }*/

        user = mBackendOps.updateUser(user);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }
        mLogger.debug("Updated merchant for password reset: "+merchant.getAuto_id());

        // Send SMS through HTTP
        String smsText = buildFirstPwdResetSMS(merchant.getAuto_id(), passwd);
        if( !SmsHelper.sendSMS(smsText, merchant.getMobile_num()) )
        {
            return BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED;
        }
        mLogger.debug("Sent first password reset SMS: "+merchant.getAuto_id());

        return null;
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

        cashback.setCust_private_id(customer.getPrivate_id());
        cashback.setCb_credit(0);
        cashback.setCb_debit(0);
        cashback.setCl_credit(0);
        cashback.setCl_debit(0);
        cashback.setTotal_billed(0);

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

    private String buildFirstPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_FIRST_PASSWD,userId,password);
    }
}
