package com.mytest.events.user_service;

import com.backendless.Backendless;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.mytest.constants.*;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
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
    private MyLogger mLogger;

    @Override
    public void afterLogin( RunnerContext context, String login, String password, ExecutionResult<HashMap> result ) throws Exception
    {
        initCommon();
        boolean positiveException = false;

        try {
            mLogger.debug("In GenericUserEventHandler: afterLogin: "+login);
            //mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());
            //mLogger.debug(context.toString());
            //List<String> roles = Backendless.UserService.getUserRoles();
            //mLogger.debug("Roles: "+roles.toString());
            /*
            if(BackendConstants.DEBUG_MODE) {
                System.out.println("Before: "+HeadersManager.getInstance().getHeaders().toString());
                System.out.println(context.toString());
                System.out.println("Roles: "+roles.toString());
            }*/

            if(result.getException()==null) {
                // Login is successful
                // add user token, so as correct roles are assumed
                HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, context.getUserToken() );

                String userId = (String) result.getResult().get("user_id");
                Integer userType = (Integer) result.getResult().get("user_type");

                if (userType == DbConstants.USER_TYPE_MERCHANT) {
                    // fetch merchant object
                    Merchants merchant = BackendOps.getMerchant(userId, true);
                    // check admin status
                    CommonUtils.checkMerchantStatus(merchant);

                    // Check if 'device id' not set
                    // This is set in setDeviceId() backend API
                    String deviceInfo = merchant.getTempDevId();
                    if(deviceInfo==null || deviceInfo.isEmpty()) {
                        // TODO : Raise critical alarm
                        throw new BackendlessException(BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "");
                    }

                    // deviceInfo format: <device id>,<manufacturer>,<model>,<os version>,<time>,<otp>
                    String[] csvFields = deviceInfo.split(CommonConstants.CSV_DELIMETER);
                    String deviceId = csvFields[0];
                    long entryTime = Long.parseLong(csvFields[4]);
                    String rcvdOtp = (csvFields.length==6) ? csvFields[5] : null;

                    // 'deviceInfo' is valid only if from last DEVICE_INFO_VALID_SECS
                    // This logic helps us to avoid resetting 'tempDevId' to NULL on each login call
                    long timeDiff = System.currentTimeMillis() - entryTime;
                    if( timeDiff > (BackendConstants.DEVICE_INFO_VALID_SECS*1000) &&
                            !BackendConstants.TESTING_MODE) {
                        // deviceInfo is old than 5 mins = 300 secs
                        // most probably from last login call - setDeviceInfo not called before this login
                        // can indicate sabotage
                        // TODO : Raise critical alarm
                        throw new BackendlessException(BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "Device data is old");
                    }

                    // Check if device is in trusted list
                    if(!CommonUtils.isTrustedDevice(deviceId, merchant)) {
                        // Device not in trusted list

                        if (rcvdOtp == null || rcvdOtp.isEmpty()) {
                            // First run of un-trusted device - generate OTP

                            // Check for max devices allowed per user
                            int deviceCnt = (merchant.getTrusted_devices()!=null) ? merchant.getTrusted_devices().size() : 0;
                            if (deviceCnt >= CommonConstants.MAX_DEVICES_PER_MERCHANT) {
                                throw new BackendlessException(BackendResponseCodes.BE_ERROR_TRUSTED_DEVICE_LIMIT_RCHD, "Device data is old");
                            }
                            // Generate OTP
                            AllOtp newOtp = new AllOtp();
                            newOtp.setUser_id(userId);
                            newOtp.setMobile_num(merchant.getMobile_num());
                            newOtp.setOpcode(DbConstantsBackend.MERCHANT_OP_NEW_DEVICE_LOGIN);
                            BackendOps.generateOtp(newOtp);

                            // OTP generated successfully - return exception to indicate so
                            positiveException = true;
                            throw new BackendlessException(BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "");

                        } else {
                            // OTP available - validate the same
                            BackendOps.validateOtp(userId, rcvdOtp);

                            // OTP is valid - add this device to trusted list
                            // Trusted device may be null - create new if so
                            List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
                            if(trustedDevices == null) {
                                trustedDevices = new ArrayList<>();
                            }

                            // New device - add as trusted device
                            MerchantDevice device = new MerchantDevice();
                            device.setMerchant_id(merchant.getAuto_id());
                            device.setDevice_id(deviceId);
                            device.setManufacturer(csvFields[1]);
                            device.setModel(csvFields[2]);
                            device.setOs_type("Android");
                            device.setOs_version(csvFields[3]);
                            //device.setLast_login(new Date());

                            trustedDevices.add(device);

                            // Update merchant
                            merchant.setTrusted_devices(trustedDevices);
                            // when USER_STATUS_NEW_REGISTERED, the device will also be new
                            // so it is not required to update this outside this if block
                            if(merchant.getAdmin_status() == DbConstants.USER_STATUS_NEW_REGISTERED) {
                                // not using fx. CommonUtils.setMerchantStatus() - as the status update is internal and not relevant for end user
                                merchant.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
                                merchant.setStatus_reason(DbConstants.ENABLED_ACTIVE);
                                merchant.setStatus_update_time(new Date());
                                merchant.setAdmin_remarks("Last status was USER_STATUS_NEW_REGISTERED");
                            }

                            merchant.setTempDevId(null);
                            BackendOps.updateMerchant(merchant);
                        }
                    }

                    // device is in trusted list - nothing to be done

                } else if (userType == DbConstants.USER_TYPE_AGENT) {
                    Agents agent = BackendOps.getAgent(userId);
                    // check admin status
                    CommonUtils.checkAgentStatus(agent);

                    // fetch device data
                    InternalUserDevice deviceData = BackendOps.fetchInternalUserDevice(userId);
                    if(deviceData==null || deviceData.getTempId()==null || deviceData.getTempId().isEmpty()) {
                        // TODO : Raise critical alarm
                        mLogger.fatal("In afterLogin for agent: Temp instance id not available: "+userId);
                        throw new BackendlessException(BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "SubCode1");
                    }
                    // If first login after register - store the provided 'instanceId' as trusted
                    if(agent.getAdmin_status()==DbConstants.USER_STATUS_NEW_REGISTERED) {
                        mLogger.debug("First login case for agent user: "+userId);
                        if(deviceData.getInstanceId()==null || deviceData.getInstanceId().isEmpty()) {
                            deviceData.setInstanceId(deviceData.getTempId());
                            // update agent state
                            agent.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
                            agent.setStatus_reason(DbConstants.ENABLED_ACTIVE);
                            BackendOps.updateAgent(agent);
                        } else {
                            // invalid state
                            mLogger.fatal("In afterLogin for agent: Invalid state: "+userId);
                            throw new BackendlessException(BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "SubCode2");
                        }
                    } else {
                        // compare instanceIds
                        if(!deviceData.getInstanceId().equals(deviceData.getTempId())) {
                            throw new BackendlessException(BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "SubCode3");
                        }
                    }
                    // update device data
                    deviceData.setTempId(null);
                    BackendOps.saveInternalUserDevice(deviceData);
                }

            } else {
                // login failed - increase count if failed due to wrong password
                if(result.getException().getCode() == Integer.parseInt(BackendResponseCodes.BL_ERROR_INVALID_ID_PASSWD)) {
                    mLogger.debug("Login failed for user: "+login+" due to wrong id/passwd");
                    switch(CommonUtils.getUserType(login)) {
                        case DbConstants.USER_TYPE_MERCHANT:
                            // fetch merchant
                            Merchants merchant = BackendOps.getMerchant(login, false);
                            CommonUtils.handleWrongAttempt(merchant, DbConstants.USER_TYPE_MERCHANT, DbConstantsBackend.ATTEMPT_TYPE_USER_LOGIN);
                            break;

                        case DbConstants.USER_TYPE_AGENT:
                            // fetch agent
                            Agents agent = BackendOps.getAgent(login);
                            CommonUtils.handleWrongAttempt(agent, DbConstants.USER_TYPE_AGENT, DbConstantsBackend.ATTEMPT_TYPE_USER_LOGIN);
                            break;
                    }
                } else {
                    mLogger.debug("Login failed for user: "+login+": "+result.getException().getCode()+": "+result.getException().getExceptionMessage());
                }
            }
        } catch (Exception e) {
            if(result.getException()==null) {
                BackendOps.logoutUser();
            }
            if(!positiveException) {
                mLogger.error("Exception in afterLogin: "+e.toString());
            }
            Backendless.Logging.flush();
            if(e instanceof BackendlessException) {
                throw CommonUtils.getNewException((BackendlessException) e);
            }
            throw e;
        }
    }

    /*
    @Override
    public void beforeRegister( RunnerContext context, HashMap userValue ) throws Exception {
        Backendless.Logging.setLogReportingPolicy(1,0);
        Logger logger = Backendless.Logging.getLogger("com.mytest.services.GenericUserEventHandler");

        logger.debug("In GenericUserEventHandler: beforeRegister");
        logger.debug("Before: beforeRegister: "+HeadersManager.getInstance().getHeaders().toString());
        logger.debug("beforeRegister:"+context.toString());
        Backendless.Logging.flush();
    }

    @Override
    public void afterRegister( RunnerContext context, HashMap userValue, ExecutionResult<HashMap> result ) throws Exception
    {
        Backendless.Logging.setLogReportingPolicy(1,0);
        Logger logger = Backendless.Logging.getLogger("com.mytest.services.GenericUserEventHandler");

        logger.debug("In GenericUserEventHandler: afterRegister");
        logger.debug("Before: afterRegister: "+HeadersManager.getInstance().getHeaders().toString());
        logger.debug("afterRegister:"+context.toString());
        Backendless.Logging.flush();
    }
    */
    /*
    @Override
    public void beforeRegister( RunnerContext context, HashMap userValue ) throws Exception
    {
        initCommon();
        try {
            mLogger.debug("In GenericUserEventHandler: beforeRegister");
            mLogger.debug("Before: beforeRegister: "+HeadersManager.getInstance().getHeaders().toString());
            mLogger.debug("beforeRegister:"+context.toString());
            // Print roles for debugging
            List<String> roles = Backendless.UserService.getUserRoles();
            mLogger.debug("beforeRegister: Roles: "+roles.toString());
            Backendless.Logging.flush();

            // If merchant, generate login id and password
            // If customer, generate private id and PIN
            //String userId = (String) userValue.get("user_id");
            Integer userType = (Integer) userValue.get("user_type");

            if (userType == DbConstants.USER_TYPE_MERCHANT) {
                Merchants merchant = (Merchants) userValue.get("merchant");
                if (merchant != null) {

                    userValue.put("user_id", "123435678");
                    userValue.put("password","1234");
                    merchant.setAuto_id("123435678");
                    merchant.setAdmin_status(DbConstants.USER_STATUS_NEW_REGISTERED);
                    merchant.setStatus_reason(DbConstants.ENABLED_NEW_USER);
                    merchant.setStatus_update_time(new Date());
                    merchant.setAdmin_remarks("New registered merchant");
                    merchant.setMobile_num(merchant.getMobile_num());
                    merchant.setCashback_table("cashback0");
                    merchant.setTxn_table("transactions0");


                    // get open merchant id batch
                    String countryCode = merchant.getAddress().getCity().getCountryCode();
                    String batchTableName = DbConstantsBackend.MERCHANT_ID_BATCH_TABLE_NAME+countryCode;
                    String whereClause = "status = '"+DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_OPEN+"'";
                    MerchantIdBatches batch = BackendOps.fetchMerchantIdBatch(batchTableName,whereClause);
                    if(batch == null) {
                        throw new BackendlessException(BackendResponseCodes.BE_ERROR_NO_OPEN_MERCHANT_ID_BATCH,
                                "No open merchant id batch available: "+batchTableName+","+whereClause);
                    }

                    // get merchant counter value and use the same to generate merchant id
                    Long merchantCnt =  BackendOps.fetchCounterValue(DbConstantsBackend.MERCHANT_ID_COUNTER);
                    mLogger.debug("Fetched merchant cnt: "+merchantCnt);
                    // set merchant id
                    String merchantId = CommonUtils.generateMerchantId(batch, countryCode, merchantCnt);
                    mLogger.debug("Generated merchant id: "+merchantId);

                    userValue.put("user_id", merchantId);
                    merchant.setAuto_id(merchantId);
                    merchant.setAdmin_status(DbConstants.USER_STATUS_NEW_REGISTERED);
                    merchant.setStatus_reason(DbConstants.ENABLED_NEW_USER);
                    merchant.setStatus_update_time(new Date());
                    merchant.setAdmin_remarks("New registered merchant");
                    merchant.setMobile_num(CommonUtils.addMobileCC(merchant.getMobile_num()));

                    // generate and set password
                    String pwd = CommonUtils.generateTempPassword();
                    mLogger.debug("Generated passwd: "+pwd);
                    userValue.put("password",pwd);

                    // set cashback and transaction table names
                    setCbAndTransTables(merchant, merchantCnt);
                }
            }
            //Backendless.Logging.flush();
        } catch (Exception e) {
            mLogger.error("Exception in beforeRegister: "+e.toString());
            Backendless.Logging.flush();
            if(e instanceof BackendlessException) {
                throw CommonUtils.getNewException((BackendlessException) e);
            }
            throw e;
        }
    }

    @Override
    public void afterRegister( RunnerContext context, HashMap userValue, ExecutionResult<HashMap> result ) throws Exception
    {
        initCommon();
        try {
            mLogger.debug("In GenericUserEventHandler: afterRegister");
            mLogger.debug("Before: afterRegister: "+HeadersManager.getInstance().getHeaders().toString());
            mLogger.debug("afterRegister:"+context.toString());
            Backendless.Logging.flush();

            // send password in SMS, if registration is successful
            if (result.getException() == null) {
                String userId = (String) userValue.get("user_id");
                Integer userType = (Integer) userValue.get("user_type");

                if (userType == DbConstants.USER_TYPE_MERCHANT) {
                    Merchants merchant = (Merchants) userValue.get("merchant");
                    if (merchant != null) {
                        // assign merchant role
                        try {
                            BackendOps.assignRole(userId, BackendConstants.ROLE_MERCHANT);
                        } catch(Exception e) {
                            // TODO: add as 'Major' alarm - user to be removed later manually
                            // rollback to not-usable state
                            merchant.setAdmin_status(DbConstants.USER_STATUS_REG_ERROR);
                            merchant.setStatus_reason(DbConstants.REG_ERROR_ROLE_ASSIGN_FAILED);
                            try {
                                BackendOps.updateMerchant(merchant);
                            } catch(Exception ex) {
                                mLogger.fatal("afterRegister: Merchant Rollback failed: "+ex.toString());
                                //TODO: raise critical alarm
                            }
                            throw e;
                        }
                        // send SMS with user id
                        String smsText = String.format(SmsConstants.SMS_MERCHANT_ID_FIRST, userId);
                        SmsHelper.sendSMS(smsText, merchant.getMobile_num());
                    }
                }
            } else {
                mLogger.error("In afterRegister, received exception: " + result.getException().toString());
            }
        } catch (Exception e) {
            mLogger.error("Exception in beforeRegister: "+e.toString());
            //Backendless.Logging.flush();
            if(e instanceof BackendlessException) {
                throw CommonUtils.getNewException((BackendlessException) e);
            }
            throw e;
        }
    }*/

    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.mytest.events.GenericUserEventHandler");
        mLogger = new MyLogger(logger);
    }

    /*
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

        String cbTableName = DbConstantsBackend.CASHBACK_TABLE_NAME + String.valueOf(table_suffix);
        merchant.setCashback_table(cbTableName);
        mLogger.debug("Generated cashback table name:" + cbTableName);

        // use the same prefix for cashback and transaction tables
        // as there is 1-to-1 mapping in the table schema - transaction0 maps to cashback0 only
        //pool_size = MyGlobalSettings.getGlobalSettings().getTxn_table_pool_size();
        //pool_start = MyGlobalSettings.getGlobalSettings().getTxn_table_pool_start();
        //table_suffix = pool_start + ((int)(mRegCounter % pool_size));

        String transTableName = DbConstantsBackend.TRANSACTION_TABLE_NAME + String.valueOf(table_suffix);
        merchant.setTxn_table(transTableName);
        mLogger.debug("Generated transaction table name:" + transTableName);
    }
    */
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
                BackendlessFault fault = new BackendlessFault(AppConstants.BE_ERROR_MERCHANT_DEVICE_LIMIT_RCHD,"");
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
                BackendlessFault fault = new BackendlessFault(AppConstants.BE_ERROR_OTP_GENERATE_FAILED,"Failed to generate OTP");
                throw new BackendlessException(fault);
            } else {
                // OTP generated successfully - return exception to indicate so
                BackendlessFault fault = new BackendlessFault(AppConstants.BE_ERROR_OTP_GENERATED,"OTP generated");
                throw new BackendlessException(fault);
            }
        }
    }
    */

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
                    String smsText = String.format(SmsConstants., userId, pin);
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
    }*/

    /*
    @Override
    public void beforeUpdate( RunnerContext context, HashMap userValue ) throws Exception
    {
        initCommon();
        mLogger.debug("In GenericUserEventHandler: beforeUpdate");

        //mLogger.debug("Before: "+ InvocationContext.asString());
        //mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());
        //mLogger.debug(context.toString());
        HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, context.getUserToken() );

        // Fetch to be updated user and check for admin status
        String userId = (String) userValue.get("user_id");
        Integer userType = (Integer) userValue.get("user_type");

        if (userType == DbConstants.USER_TYPE_MERCHANT) {
            Merchants merchant = (Merchants) userValue.get("merchant");
            if(merchant==null) {
                // fetch merchant
                merchant = BackendOps.getMerchant(userId, false);
                if(merchant==null) {
                    CommonUtils.throwException(mLogger, BackendResponseCodes.BE_ERROR_NO_SUCH_USER, "No merchant object with id "+userId, false);
                }
            }

            String status = CommonUtils.checkMerchantStatus(merchant);
            if(status != null) {
                CommonUtils.throwException(mLogger, status, "Merchant account not active: "+userId, false);
            }

        } else if (userType == DbConstants.USER_TYPE_CUSTOMER) {
            Customers customer = (Customers) userValue.get("customer");
            if(customer==null) {
                // fetch merchant
                customer = BackendOps.getCustomer(userId, true);
                if(customer==null) {
                    CommonUtils.throwException(mLogger, BackendResponseCodes.BE_ERROR_NO_SUCH_USER, "No customer object with id "+userId, false);
                }
            }

            String status = CommonUtils.checkCustomerStatus(customer);
            if(status != null) {
                CommonUtils.throwException(mLogger, status, "Customer account not active: "+userId, false);
            }
        }
    }*/

