package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.DbConstants;
import com.mytest.constants.DbConstantsBackend;
import com.mytest.database.Agents;
import com.mytest.database.MerchantIdBatches;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 19-07-2016.
 */
public class AdminServices implements IBackendlessService {

    private Logger mLogger;

    /*
     * Public methods: Backend REST APIs
     */
    public void registerAgent(String userId, String mobile, String name, String dob, String pwd) {
        initCommon();
        try {
            mLogger.debug("In registerAgent: "+userId+": "+mobile);
            //mLogger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            // login using 'admin' user
            BackendOps.loginUser("admin",pwd);
            //mLogger.debug("Before2: "+ HeadersManager.getInstance().getHeaders().toString());

            // Create agent object and register
            Agents agent = new Agents();
            agent.setId(userId);
            agent.setMobile_num(mobile);
            agent.setDob(dob);
            agent.setName(name);
            agent.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
            agent.setStatus_reason(DbConstants.ENABLED_ACTIVE);

            BackendlessUser agentUser = new BackendlessUser();
            agentUser.setProperty("user_id", userId);
            agentUser.setPassword(dob);
            agentUser.setProperty("user_type", DbConstants.USER_TYPE_AGENT);
            agentUser.setProperty("agent",agent);

            // print roles - for debug purpose
            List<String> roles = Backendless.UserService.getUserRoles();
            mLogger.debug("Roles: "+roles.toString());

            // register the user
            agentUser = BackendOps.registerUser(agentUser);
            mLogger.debug("Agent Registration successful");

            // assign role
            try {
                BackendOps.assignRole(userId, BackendConstants.ROLE_AGENT);
            } catch (Exception e) {
                // TODO: add as 'Major' alarm - user to be removed later manually
                throw e;
            }

            // Send sms to the customer with PIN
            String smsText = String.format(SmsConstants.SMS_REG_AGENT, userId);
            if (!SmsHelper.sendSMS(smsText, mobile)) {
                // TODO: write to alarm table for retry later
            }

            // logout admin user
            BackendOps.logoutUser();

        } catch (Exception e) {
            mLogger.error("Exception in registerAgent: "+e.toString());
            BackendOps.logoutUser();
            Backendless.Logging.flush();
            throw e;
        }
    }

    /*
     * Merchant Id services
     */
    public void openMerchantIdRange(String countryCode, String rangeId, int startIdx, int endIdx, String adminPwd) {
        initCommon();
        try {
            mLogger.debug("In openMerchantIdRange: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser("admin",adminPwd);

            // make sure range is not already open
            String tableName = "MerchantIdBatches"+countryCode;
            if(BackendOps.merchantIdRangeOpen(tableName, rangeId)) {
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "Range is already open: "+countryCode+","+rangeId);
            }

            // add batches for this range
            // assuming 2 digit batch ids from 00 - 99
            for(int i=startIdx; i<=endIdx; i++) {
                MerchantIdBatches batch = new MerchantIdBatches();
                batch.setRangeId(rangeId);
                batch.setStatus(DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_AVAILABLE);
                batch.setBatchId(String.format("%02d",i));
                batch.setStatusTime(new Date());
                BackendOps.saveMerchantIdBatch(tableName, batch);
            }

            // logout admin user
            BackendOps.logoutUser();

        } catch (Exception e) {
            mLogger.error("Exception in openMerchantIdRange: "+e.toString());
            BackendOps.logoutUser();
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
        mLogger = Backendless.Logging.getLogger("com.mytest.services.AdminServices");
        CommonUtils.initTableToClassMappings();
    }

}

    /*
    public void openMerchantIdBatch(String countryCode, String rangeId, String batchId, String adminPwd) {
        initCommon();
        try {
            mLogger.debug("In openMerchantIdBatch: "+countryCode+","+rangeId+","+batchId);
            // login using 'admin' user
            BackendOps.loginUser("admin",adminPwd);

            // make sure batch is not already open
            String tableNameMerchantIds = "MerchantIds"+countryCode+rangeId;
            if(BackendOps.merchantIdBatchOpen(tableNameMerchantIds, batchId)) {
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "Batch is already open: "+countryCode+","+rangeId+","+batchId);
            }

            String tableNameBatches = "MerchantIdBatches"+countryCode;

            // fetch currently open batches
            List<MerchantIdBatches> openBatches = BackendOps.fetchOpenMerchantIdBatches(tableNameBatches);
            if(openBatches != null) {
                // check if any batch is to be closed - if yes, do so
                for (MerchantIdBatches openBatch:openBatches) {
                    if(BackendOps.getAvailableMerchantIdCnt(tableNameMerchantIds, openBatch.getBatchId())==0 &&
                            BackendOps.getTotalMerchantIdCnt(tableNameMerchantIds, openBatch.getBatchId())==BackendConstants.MERCHANT_ID_MAX_IDS_PER_BATCH) {
                        // update batch status
                        openBatch.setStatus(DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_CLOSED);
                        BackendOps.saveMerchantIdBatch(tableNameBatches, openBatch);
                        mLogger.info("Closed batch "+openBatch.getBatchId()+" in table "+tableNameBatches);
                    }
                }
            }

            // fetch new batch object
            MerchantIdBatches batch = BackendOps.fetchMerchantIdBatch(tableNameBatches, rangeId, batchId);
            if(batch.getStatus().equals(DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_CLOSED)) {
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "Invalid new batch status: "+countryCode+","+rangeId+","+batchId);
            }

            // add 'merchant ids' for this range
            // Merchant id format: <1-3 digit country code> + <0-2 digit range id> + <2 digit batch id> + <3 digit s.no.>
            MerchantIds merchantId = new MerchantIds();
            merchantId.setBatchId(batchId);
            merchantId.setStatus(DbConstantsBackend.MERCHANT_ID_STATUS_AVAILABLE);
            for(int i=0; i<=999; i++) {
                String serialNo = String.format("%03d",i);
                merchantId.setSerialNo(serialNo);
                merchantId.setStatusTime(new Date());

                String finalMerchantId = countryCode+rangeId+batchId+serialNo;
                merchantId.setMerchantId(finalMerchantId);

                BackendOps.createMerchantId(tableNameMerchantIds, merchantId);
            }

            // update batch status
            if(batch.getStatus().equals(DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_CLOSED)) {
                batch.setStatus(DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_OPEN);
                BackendOps.saveMerchantIdBatch(tableNameBatches, batch);
                mLogger.info("Opened batch "+batchId+" in table "+tableNameBatches);
            }
            // logout admin user
            BackendOps.logoutUser();

        } catch (Exception e) {
            mLogger.error("Exception in openMerchantIdBatch: "+e.toString());
            BackendOps.logoutUser();
            Backendless.Logging.flush();
            throw e;
        }
    }
    */


