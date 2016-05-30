package com.mytest.events.user_service;

import com.backendless.Backendless;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.logging.Logger;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.mytest.database.*;
import com.mytest.utilities.*;

import java.util.*;

/**
* GenericUserEventHandler handles the User Service events.
* The event handlers are the individual methods implemented in the class.
* The "before" and "after" prefix determines if the handler is executed before
* or after the default handling logic provided by Backendless.
* The part after the prefix identifies the actual event.
* For example, the "beforeLogin" method is the "Login" event handler and will
* be called before Backendless applies the default login logic. The event
* handling pipeline looks like this:

* Client Request ---> Before Handler ---> Default Logic ---> After Handler --->
* Return Response
*/

public class GenericUserEventHandler extends com.backendless.servercode.extension.UserExtender
{
    private Logger mLogger;

    @Override
    public void afterLogin( RunnerContext context, String login, String password, ExecutionResult<HashMap> result ) throws Exception
    {
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.GenericUserEventHandler");
        BackendOps backendOps = new BackendOps(mLogger);

        mLogger.debug("In GenericUserEventHandler: afterLogin");

        if(result.getException()==null) {
            String userId = (String) result.getResult().get("user_id");
            Integer userType = (Integer) result.getResult().get("user_type");

            if (userType == DbConstants.USER_TYPE_MERCHANT) {
                Merchants merchant = backendOps.getMerchant(userId);
                if(merchant==null) {
                    backendOps.logoutUser();
                    BackendlessFault fault = new BackendlessFault(backendOps.mLastOpStatus,"Failed to fetch merchant");
                    throw new BackendlessException(fault);
                }

                // check admin status
                String status = CommonUtils.checkMerchantStatus(merchant.getAdmin_status());
                if(status != null) {
                    backendOps.logoutUser();
                    BackendlessFault fault = new BackendlessFault(status,"Account not active");
                    throw new BackendlessException(fault);
                }

                // Check if 'device id' not set
                String deviceInfo = merchant.getTempDevId();
                if(deviceInfo==null || deviceInfo.isEmpty()) {
                    backendOps.logoutUser();
                    BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_MYERROR_NOT_TRUSTED_DEVICE,"Untrusted device");
                    throw new BackendlessException(fault);
                }

                // Add to 'trusted list', if not already there
                // deviceInfo format: <device id>,<manufacturer>,<model>,<os version>
                String[] csvFields = deviceInfo.split(CommonConstants.CSV_DELIMETER);
                String deviceId = csvFields[0];

                // Match device id
                boolean matched = false;
                List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
                if(trustedDevices != null) {
                    for (MerchantDevice device : trustedDevices) {
                        if(device.getDevice_id().equals(deviceId)) {
                            matched = true;
                            device.setLast_login(new Date());
                            break;
                        }
                    }
                } else {
                    trustedDevices = new ArrayList<>();
                }

                if(!matched) {
                    // New device - add as trusted device
                    MerchantDevice device = new MerchantDevice();
                    device.setMerchant_id(merchant.getAuto_id());
                    device.setDevice_id(csvFields[0]);
                    device.setManufacturer(csvFields[1]);
                    device.setModel(csvFields[2]);
                    device.setOs_type("Android");
                    device.setOs_version(csvFields[3]);
                    device.setLast_login(new Date());

                    trustedDevices.add(device);
                }

                // Update merchant
                merchant.setTempDevId(null);
                merchant.setTrusted_devices(trustedDevices);
                Merchants merchant2 = backendOps.updateMerchant(merchant);
                if(merchant2==null) {
                    backendOps.logoutUser();
                    BackendlessFault fault = new BackendlessFault(backendOps.mLastOpStatus,"Failed to add trusted device");
                    throw new BackendlessException(fault);
                }
            }
        }
    }

    @Override
    public void beforeRegister( RunnerContext context, HashMap userValue ) throws Exception
    {
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.GenericUserEventHandler");
        BackendOps backendOps = new BackendOps(mLogger);

        mLogger.debug("In GenericUserEventHandler: beforeRegister");

        // If merchant, generate login id and password
        // If customer, generate private id and PIN
        String userId = (String) userValue.get("user_id");
        Integer userType = (Integer) userValue.get("user_type");

        if (userType == DbConstants.USER_TYPE_CUSTOMER) {
            Customers customer = (Customers) userValue.get("customer");
            if (customer != null) {
                // get customer counter value and encode the same to get customer private id
                Double customerCnt =  backendOps.fetchCounterValue(DbConstants.CUSTOMER_ID_COUNTER);
                if(customerCnt == null) {
                    BackendlessFault fault = new BackendlessFault(backendOps.mLastOpStatus,"Failed to fetch customer id counter value");
                    throw new BackendlessException(fault);
                }
                mLogger.debug("Fetched customer cnt: "+customerCnt.longValue());
                String private_id = Base61.fromBase10(customerCnt.longValue());
                mLogger.debug("Generated private id: "+private_id);
                customer.setPrivate_id(private_id);

                // generate and set PIN
                String pin = CommonUtils.generateCustomerPIN();
                mLogger.debug("Generated PIN: "+pin);
                customer.setTxn_pin(pin);
                userValue.put("password",pin);
                mLogger.debug("Updated all");
            } else {
                mLogger.error("Customer object is null: " + userId);
            }
        } else {
            Merchants merchant = (Merchants) userValue.get("merchant");
            if (merchant != null) {
                // get merchant counter value and use the same to generate merchant id
                Double merchantCnt =  backendOps.fetchCounterValue(DbConstants.MERCHANT_ID_COUNTER);
                if(merchantCnt == null) {
                    BackendlessFault fault = new BackendlessFault(backendOps.mLastOpStatus,"Failed to fetch merchant id counter value");
                    throw new BackendlessException(fault);
                }
                mLogger.debug("Fetched merchant cnt: "+merchantCnt.longValue());
                // set merchant id
                String merchantId = generateMerchantId(merchantCnt.longValue());
                mLogger.debug("Generated merchant id: "+merchantId);
                userValue.put("user_id", merchantId);
                merchant.setAuto_id(merchantId);

                // generate and set password
                String pwd = generatePassword();
                mLogger.debug("Generated passwd: "+pwd);
                userValue.put("password",pwd);

                // set cashback and transaction table names
                /*
                GlobalSettings gSettings = backendOps.fetchGlobalSettings();
                if(gSettings==null) {
                    BackendlessFault fault = new BackendlessFault(backendOps.mLastOpStatus,"Failed to fetch global settings");
                    throw new BackendlessException(fault);
                }*/
                setCbAndTransTables(merchant, merchantCnt.longValue());
            } else {
                mLogger.error("Merchant object is null: " + userId);
            }
        }
        Backendless.Logging.flush();
    }

    /*
    @Override
    public void afterRegister( RunnerContext context, HashMap userValue, ExecutionResult<HashMap> result ) throws Exception {
    }*/
        /*
    @Override
    public void afterRegister( RunnerContext context, HashMap userValue, ExecutionResult<HashMap> result ) throws Exception {
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.GenericUserEventHandler");

        mLogger.debug("In GenericUserEventHandler: afterRegister");
        Backendless.Logging.flush();

        // send password in SMS, if registration is successful
        if (result.getException() == null) {
            mLogger.debug("A2");
            Backendless.Logging.flush();

            HashMap user = result.getResult();
            mLogger.debug("A3");
            Backendless.Logging.flush();
            String userId = (String) user.get("user_id");
            Integer userType = (Integer) user.get("user_type");

            if (userType == DbConstants.USER_TYPE_CUSTOMER) {
                Customers customer = (Customers) user.get("customer");
                if (customer != null) {
                    // Send sms to the customer with PIN
                    String pin = customer.getTxn_pin();
                    String smsText = String.format(SmsConstants.SMS_TEMPLATE_PIN, userId, pin);
                    // Send SMS through HTTP
                    if (!SmsHelper.sendSMS(smsText, customer.getMobile_num())) {
                        // TODO: write to alarm table for retry later
                    }
                } else {
                    mLogger.error("Customer object is null: " + userId);
                }
            }
        } else {
            mLogger.error("In afterRegister, received exception: " + result.getException().toString());
            Backendless.Logging.flush();
        }
    }
    */

    private String generateMerchantId(long regCounter) {
        // Generate unique merchant id based on merchant reg counter value
        // Id is alphanumeric with first 2 alphabets and then 4 digits

        // 9999 is max for 4 digits
        // not using 10,000 as divisor to avoid using 0000 in user id
        int divisor = 9999;
        int rem = (int)(regCounter%divisor);

        // first alphabet = counter / 26*9999
        // second alphabet = counter / 9999
        // digits = counter % 9999
        StringBuilder sb = new StringBuilder();
        sb.append(CommonConstants.numToChar[(int)(regCounter/(26*divisor))]);
        sb.append(CommonConstants.numToChar[(int) (regCounter/divisor)]);
        if(rem==0) {
            sb.append(divisor);
        } else {
            sb.append(String.format("%04d",rem));
        }

        return sb.toString();
    }

    private String generatePassword() {
        // random alphanumeric string
        Random random = new Random();
        char[] id = new char[BackendConstants.PASSWORD_LEN];
        for (int i = 0; i < BackendConstants.PASSWORD_LEN; i++) {
            id[i] = BackendConstants.pwdChars[random.nextInt(BackendConstants.pwdChars.length)];
        }
        String passwd = new String(id);
        //return passwd;
        return "adi";
    }

    private void setCbAndTransTables(Merchants merchant, long regCounter) {
        // decide on the cashback table using round robin
        //int pool_size = gSettings.getCb_table_pool_size();
        //int pool_start = gSettings.getCb_table_pool_start();
        int pool_size = BackendConstants.CASHBACK_TABLE_POOL_SIZE;
        int pool_start = BackendConstants.CASHBACK_TABLE_POOL_START;

        // use last 4 numeric digits for round-robin
        //int num = Integer.parseInt(getUser_id().substring(2));
        int table_suffix = pool_start + ((int)(regCounter % pool_size));
        //int table_suffix = pool_start + (num % pool_size);

        String cbTableName = DbConstants.CASHBACK_TABLE_NAME + String.valueOf(table_suffix);
        merchant.setCashback_table(cbTableName);
        mLogger.debug("Generated cashback table name:" + cbTableName);

        // use the same prefix for cashback and transaction tables
        // as there is 1-to-1 mapping in the table schema - transaction0 maps to cashback0 only
        //pool_size = MyGlobalSettings.getGlobalSettings().getTxn_table_pool_size();
        //pool_start = MyGlobalSettings.getGlobalSettings().getTxn_table_pool_start();
        //table_suffix = pool_start + ((int)(mRegCounter % pool_size));

        String transTableName = DbConstants.TRANSACTION_TABLE_NAME + String.valueOf(table_suffix);
        merchant.setTxn_table(transTableName);
        mLogger.debug("Generated transaction table name:" + transTableName);
    }
}

    /*
    private static int MAX_DEVICES_PER_MERCHANT = 3;

    @Override
    public void beforeLogin( RunnerContext context, String login, String password ) throws Exception
    {
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.GenericUserEventHandler");
        BackendOps backendOps = new BackendOps(mLogger);

        mLogger.debug("In beforeLogin: "+login);

        // Login id contains device id too - seperate them out
        String[] csvFields = login.split(AppConstants.CSV_DELIMETER);
        String loginId = csvFields[0];
        String deviceId = csvFields[1];

        BackendlessUser user = backendOps.fetchUser(loginId, DbConstants.USER_TYPE_MERCHANT);
        if(user==null) {
            BackendlessFault fault = new BackendlessFault(backendOps.mLastOpStatus,"Failed to fetch global settings");
            throw new BackendlessException(fault);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        // Check current admin status
        int status = merchant.getAdmin_status();
        switch(status) {
            case DbConstants.USER_STATUS_REGISTERED:
                logoutSync();
                return ErrorCodes.USER_NEW;
            case DbConstants.USER_STATUS_DISABLED:
                logoutSync();
                return ErrorCodes.USER_ACC_DISABLED;
        }

        List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
        int cnt = 0;
        boolean matched = false;
        if(trustedDevices != null) {
            cnt = trustedDevices.size();
            for (MerchantDevice device : trustedDevices) {
                if(device.getDevice_id().equals(deviceId)) {
                    matched = true;
                    break;
                }
            }
        }

        // if no matching device id found - means new device for this user
        // generate OTP if limit not reached
        if(!matched) {
            // Check for max devices allowed per user
            if(cnt >= MAX_DEVICES_PER_MERCHANT) {
                BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_MERCHANT_DEVICE_LIMIT_RCHD,"");
                throw new BackendlessException(fault);
            }
            // First login for this  - generate OTP and generate exception
            AllOtp newOtp = new AllOtp();
            newOtp.setUser_id(loginId);
            newOtp.setMobile_num(merchant.getMobile_num());
            newOtp.setOpcode(DbConstants.MERCHANT_OP_NEW_DEVICE_LOGIN);
            newOtp = backendOps.generateOtp(newOtp);
            if(newOtp == null) {
                // failed to generate otp
                BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_OTP_GENERATE_FAILED,"Failed to generate OTP");
                throw new BackendlessException(fault);
            } else {
                // OTP generated successfully - return exception to indicate so
                BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_OTP_GENERATED,"OTP generated");
                throw new BackendlessException(fault);
            }
        }
    }
    */

