package com.myecash.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.FilePermission;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import com.myecash.constants.*;
import com.myecash.database.Agents;
import com.myecash.database.MerchantIdBatches;
import com.myecash.database.Merchants;
import com.myecash.messaging.SmsConstants;
import com.myecash.messaging.SmsHelper;
import com.myecash.utilities.BackendOps;
import com.myecash.utilities.CommonUtils;
import com.myecash.utilities.MyLogger;

import java.util.Date;

/**
 * Created by adgangwa on 12-08-2016.
 */
public class AgentServices  implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.AgentServicesNoLogin");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     */
    public void registerMerchant(Merchants merchant)
    {
        //initCommon();
        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "registerMerchant";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchant.getAuto_id()+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                merchant.getMobile_num()+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                merchant.getName();
        String merchantId = null;

        try {
            //mLogger.debug("In registerMerchant");
            //mLogger.debug("registerMerchant: Before: "+ InvocationContext.asString());
            //mLogger.debug("registerMerchant: Before: "+HeadersManager.getInstance().getHeaders().toString());
            //mLogger.flush();

            // Fetch agent
            Agents agent = (Agents) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_AGENT, mEdr, mLogger);
            mLogger.setProperties(agent.getId(), DbConstants.USER_TYPE_AGENT, agent.getDebugLogs());
            mEdr[BackendConstants.EDR_AGENT_ID_IDX] = agent.getId();

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
            merchantId = CommonUtils.generateMerchantId(batch, countryCode, merchantCnt);
            mLogger.debug("Generated merchant id: "+merchantId);

            merchant.setAuto_id(merchantId);
            merchant.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
            merchant.setStatus_reason(DbConstants.ENABLED_ACTIVE);
            merchant.setStatus_update_time(new Date());
            merchant.setAdmin_remarks("New registered merchant");
            merchant.setMobile_num(merchant.getMobile_num());
            merchant.setFirst_login_ok(false);
            // set cashback and transaction table names
            setCbAndTransTables(merchant, merchantCnt);

            // generate and set password
            String pwd = CommonUtils.generateTempPassword();
            mLogger.debug("Generated passwd: "+pwd);

            BackendlessUser user = new BackendlessUser();
            user.setProperty("user_id", merchantId);
            user.setPassword(pwd);
            user.setProperty("user_type", DbConstants.USER_TYPE_MERCHANT);
            user.setProperty("merchant", merchant);

            user = BackendOps.registerUser(user);
            // register successfull - can write to edr now
            mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();

            try {
                BackendOps.assignRole(merchantId, BackendConstants.ROLE_MERCHANT);

            } catch(Exception e) {
                mLogger.fatal("Failed to assign role to merchant user: "+merchantId+","+e.toString());
                rollbackRegister(merchantId);
                throw e;
            }

            // create directories for 'txnCsv' and 'txnImage' files
            String fileDir = null;
            String filePath = null;
            try {
                fileDir = CommonUtils.getMerchantTxnDir(merchantId);
                filePath = fileDir + CommonConstants.FILE_PATH_SEPERATOR+BackendConstants.DUMMY_FILENAME;
                // saving dummy files to create parent directories
                Backendless.Files.saveFile(filePath, BackendConstants.DUMMY_DATA.getBytes("UTF-8"), true);
                // Give this merchant permissions for this directory
                FilePermission.READ.grantForUser( user.getObjectId(), fileDir);
                FilePermission.DELETE.grantForUser( user.getObjectId(), fileDir);
                FilePermission.WRITE.grantForUser( user.getObjectId(), fileDir);
                mLogger.debug("Saved dummy txn csv file: " + filePath);

                fileDir = CommonUtils.getTxnImgDir(merchantId);
                filePath = fileDir + CommonConstants.FILE_PATH_SEPERATOR+BackendConstants.DUMMY_FILENAME;
                Backendless.Files.saveFile(filePath, BackendConstants.DUMMY_DATA.getBytes("UTF-8"), true);
                // Give read access to this merchant to this directory
                FilePermission.WRITE.grantForUser( user.getObjectId(), fileDir);
                mLogger.debug("Saved dummy txn image file: " + filePath);

            } catch(Exception e) {
                mLogger.fatal("Failed to create merchant directories: "+merchantId+","+e.toString());
                rollbackRegister(merchantId);
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, e.toString());
            }

            // send SMS with user id
            String smsText = String.format(SmsConstants.SMS_MERCHANT_ID_FIRST, merchantId);
            if(SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mLogger)) {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
            } else {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            if(merchantId!=null && !merchantId.isEmpty()) {
                BackendOps.decrementCounterValue(DbConstantsBackend.MERCHANT_ID_COUNTER);
            }
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
     * Private helper methods
     */
    private void setCbAndTransTables(Merchants merchant, long regCounter) {
        // decide on the cashback table using round robin
        int pool_size = BackendConstants.CASHBACK_TABLE_POOL_SIZE;
        int pool_start = BackendConstants.CASHBACK_TABLE_POOL_START;

        // use last 4 numeric digits for round-robin
        int table_suffix = pool_start + ((int)(regCounter % pool_size));

        String cbTableName = DbConstantsBackend.CASHBACK_TABLE_NAME + String.valueOf(table_suffix);
        merchant.setCashback_table(cbTableName);
        mLogger.debug("Generated cashback table name:" + cbTableName);

        // use the same prefix for cashback and transaction tables
        // as there is 1-to-1 mapping in the table schema - transaction0 maps to cashback0 only
        String transTableName = DbConstantsBackend.TRANSACTION_TABLE_NAME + String.valueOf(table_suffix);
        merchant.setTxn_table(transTableName);
        mLogger.debug("Generated transaction table name:" + transTableName);
    }

//    private void rollbackRegister(BackendlessUser user) {
    private void rollbackRegister(String mchntId) {
        // TODO: add as 'Major' alarm - user to be removed later manually
        // rollback to not-usable state
        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
        try {
            Merchants merchant = BackendOps.getMerchant(mchntId, false);
            merchant.setAdmin_status(DbConstants.USER_STATUS_REG_ERROR);
            merchant.setStatus_reason(DbConstants.REG_ERROR_ROLE_ASSIGN_FAILED);
            merchant.setAdmin_remarks("Registration failed");
            //user.setProperty("merchant", merchant);
            //BackendOps.updateUser(user);
            BackendOps.updateMerchant(merchant);
        } catch(Exception ex) {
            mLogger.fatal("registerMerchant: Merchant Rollback failed: "+ex.toString());
            //TODO: raise critical alarm
            throw ex;
        }
    }
}

