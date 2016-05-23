package com.mytest.events.user_service;

import com.backendless.Backendless;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.logging.Logger;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.mytest.database.Merchants;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.Base61;
import com.mytest.utilities.CommonUtils;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.AppConstants;
import com.mytest.database.Customers;
import com.mytest.database.DbConstants;

import java.util.HashMap;
import java.util.Random;

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

    @Override
    public void beforeRegister( RunnerContext context, HashMap userValue ) throws Exception
    {
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.mytest.events.GenericUserEventHandler");
        BackendOps backendOps = new BackendOps(logger);

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
                String private_id = Base61.fromBase10(customerCnt.longValue());
                customer.setPrivate_id(private_id);

                // generate and set PIN
                String pin = CommonUtils.generateCustomerPIN();
                customer.setTxn_pin(pin);
                userValue.put("password",pin);
            } else {
                logger.error("Customer object is null: " + userId);
            }
        } else {
            Merchants merchant = (Merchants) userValue.get("merchant");
            if (merchant != null) {
                // get merchant counter value and use the same to generate merchant id
                Double merchantCnt =  backendOps.fetchCounterValue(DbConstants.CUSTOMER_ID_COUNTER);
                if(merchantCnt == null) {
                    BackendlessFault fault = new BackendlessFault(backendOps.mLastOpStatus,"Failed to fetch merchant id counter value");
                    throw new BackendlessException(fault);
                }
                // set merchant id
                String merchantId = generateMerchantId(merchantCnt.longValue());
                userValue.put("user_id", merchantId);
                merchant.setAuto_id(merchantId);

                // generate and set password
                String pwd = generatePassword();
                userValue.put("password",pwd);
            } else {
                logger.error("Merchant object is null: " + userId);
            }
        }
    }

    @Override
    public void afterRegister( RunnerContext context, HashMap userValue, ExecutionResult<HashMap> result ) throws Exception {
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.mytest.events.GenericUserEventHandler");

        // send password in SMS, if registration is successful
        if (result.getException() == null) {
            String userId = (String) userValue.get("user_id");
            Integer userType = (Integer) userValue.get("user_type");

            if (userType == DbConstants.USER_TYPE_CUSTOMER) {
                Customers customer = (Customers) userValue.get("customer");
                if (customer != null) {
                    // Send sms to the customer with PIN
                    String pin = customer.getTxn_pin();
                    String smsText = String.format(SmsConstants.SMS_TEMPLATE_PIN, userId, pin);
                    // Send SMS through HTTP
                    if (!SmsHelper.sendSMS(smsText, customer.getMobile_num())) {
                        // TODO: write to alarm table for retry later
                    }
                } else {
                    logger.error("Customer object is null: " + userId);
                }
            }
        } else {
            System.out.println("User registration was not successful: " + result.getException().toString());
        }
    }

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
        sb.append(AppConstants.numToChar[(int)(regCounter/(26*divisor))]);
        sb.append(AppConstants.numToChar[(int) (regCounter/divisor)]);
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
        char[] id = new char[AppConstants.PASSWORD_LEN];
        for (int i = 0;  i < AppConstants.PASSWORD_LEN;  i++) {
            id[i] = AppConstants.pwdChars[random.nextInt(AppConstants.pwdChars.length)];
        }
        String passwd = new String(id);
        //return passwd;
        return "adi";
    }

}
