package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.FilePermission;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import com.mytest.constants.*;
import com.mytest.database.MerchantIdBatches;
import com.mytest.database.Merchants;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

import java.util.Date;

/**
 * Created by adgangwa on 12-08-2016.
 */
public class AgentServices  implements IBackendlessService {

    private Logger mLogger;

    /*
     * Public methods: Backend REST APIs
     */
    public void registerMerchant(Merchants merchant)
    {
        initCommon();
        try {
            mLogger.debug("In registerMerchant");
            mLogger.debug("registerMerchant: Before: "+ InvocationContext.asString());
            mLogger.debug("registerMerchant: Before: "+HeadersManager.getInstance().getHeaders().toString());
            Backendless.Logging.flush();

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

            merchant.setAuto_id(merchantId);
            merchant.setAdmin_status(DbConstants.USER_STATUS_NEW_REGISTERED);
            merchant.setStatus_reason(DbConstants.ENABLED_NEW_USER);
            merchant.setStatus_update_time(new Date());
            merchant.setAdmin_remarks("New registered merchant");
            merchant.setMobile_num(CommonUtils.addMobileCC(merchant.getMobile_num()));
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
                mLogger.debug("Filepath: " + filePath);
                // saving dummy files to create parent directories
                Backendless.Files.saveFile(filePath, BackendConstants.DUMMY_DATA.getBytes("UTF-8"), true);
                // Give this merchant permissions for this directory
                FilePermission.READ.grantForUser( user.getObjectId(), fileDir);
                FilePermission.DELETE.grantForUser( user.getObjectId(), fileDir);
                FilePermission.WRITE.grantForUser( user.getObjectId(), fileDir);
                mLogger.debug("Saved dummy txn csv file: " + filePath);

                fileDir = CommonUtils.getTxnImgDir(merchantId);
                filePath = fileDir + CommonConstants.FILE_PATH_SEPERATOR+BackendConstants.DUMMY_FILENAME;
                mLogger.debug("Filepath: " + filePath);
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
            SmsHelper.sendSMS(smsText, merchant.getMobile_num());

        } catch (Exception e) {
            mLogger.error("Exception in registerMerchant: "+e.toString());
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
        mLogger = Backendless.Logging.getLogger("com.mytest.services.AgentServices");
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

//    private void rollbackRegister(BackendlessUser user) {
    private void rollbackRegister(String mchntId) {
        // TODO: add as 'Major' alarm - user to be removed later manually
        // rollback to not-usable state
        try {
            BackendOps.decrementCounterValue(DbConstantsBackend.MERCHANT_ID_COUNTER);
            //BackendOps.loadMerchant(user);
            //Merchants merchant = (Merchants)user.getProperty("merchant");
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

