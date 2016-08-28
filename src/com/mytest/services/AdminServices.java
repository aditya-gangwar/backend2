package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.mytest.constants.*;
import com.mytest.database.Agents;
import com.mytest.database.CardIdBatches;
import com.mytest.database.MerchantIdBatches;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;
import com.mytest.utilities.MyLogger;

import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 19-07-2016.
 */
public class AdminServices implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.AdminServices");
    //private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     */
    public void registerAgent(String userId, String mobile, String name, String dob, String pwd) {
        //initCommon();
        try {
            CommonUtils.initTableToClassMappings();
            mLogger.setProperties("admin", DbConstants.USER_TYPE_AGENT, true);
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
            mLogger.flush();
            throw e;
        }
    }

    public void createMerchantIdBatches(String countryCode, String rangeId, int batchCnt, String adminPwd) {
        //initCommon();
        try {
            CommonUtils.initTableToClassMappings();
            mLogger.setProperties("admin", DbConstants.USER_TYPE_AGENT, true);
            mLogger.debug("In createMerchantIdBatches: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser("admin",adminPwd);

            String tableName = DbConstantsBackend.MERCHANT_ID_BATCH_TABLE_NAME+countryCode;
            // get highest 'batch id' from already created batches - for given range id
            MerchantIdBatches highestBatch = BackendOps.firstMerchantIdBatchByBatchId(tableName, "rangeId = '"+rangeId+"'", true);

            int highestBatchId = (highestBatch!=null) ? highestBatch.getBatchId() : 0;
            if(highestBatchId >= BackendConstants.MERCHANT_ID_MAX_BATCH_ID_PER_RANGE) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "All batches already available: "+countryCode+","+rangeId);
            }

            // batch will start from 1, and not 0, which is reserved for future use
            int startIdx = highestBatchId+1;
            int endIdx = startIdx+batchCnt;
            if(endIdx > BackendConstants.MERCHANT_ID_MAX_BATCH_ID_PER_RANGE) {
                endIdx = BackendConstants.MERCHANT_ID_MAX_BATCH_ID_PER_RANGE;
            }

            // add batches for this range
            // assuming 2 digit batch ids from 01 - 99 (00 reserved for now)
            for(int i=startIdx; i<endIdx; i++) {
                MerchantIdBatches batch = new MerchantIdBatches();
                batch.setStatus(DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_AVAILABLE);
                batch.setRangeId(rangeId);
                batch.setBatchId(i);
                String batchId = String.format("%02d",i);
                batch.setRangeBatchId(rangeId+batchId);
                batch.setStatusTime(new Date());
                BackendOps.saveMerchantIdBatch(tableName, batch);
            }

            // logout admin user
            BackendOps.logoutUser();

        } catch (Exception e) {
            mLogger.error("Exception in createMerchantIdBatches: "+e.toString());
            BackendOps.logoutUser();
            mLogger.flush();
            throw e;
        }
    }

    public void openNextMerchantIdBatch(String countryCode, String rangeId, String adminPwd) {
        //initCommon();
        try {
            CommonUtils.initTableToClassMappings();
            mLogger.setProperties("admin", DbConstants.USER_TYPE_AGENT, true);
            mLogger.debug("In openNextMerchantIdBatch: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser("admin",adminPwd);

            String tableName = DbConstantsBackend.MERCHANT_ID_BATCH_TABLE_NAME+countryCode;

            // get current open batch
            MerchantIdBatches openBatch = BackendOps.fetchMerchantIdBatch(tableName,
                    "status = '"+DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_OPEN+"'");
            if(openBatch!=null && !openBatch.getRangeId().equals(rangeId)) {
                // If rangeId of 'current open batch' is different from the one provided
                // first close the current open batch manually, and then try again.
                // Check added just to be more sure - when new range is being opened
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "RangeId of 'current open batch' is different : "+countryCode+","+rangeId);
            }

            // check if current open batch is still empty
            if(openBatch != null) {
                String merchantIdPrefix = countryCode+openBatch.getRangeBatchId();
                int cnt = BackendOps.getMerchantCnt("auto_id like '"+merchantIdPrefix+"%'");
                if(cnt < BackendConstants.MERCHANT_ID_MAX_SNO_PER_BATCH) {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "Current open batch still empty : "+cnt+","+countryCode+","+rangeId);
                }

                // close current open batch
                openBatch.setStatus(DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_CLOSED);
                BackendOps.saveMerchantIdBatch(tableName, openBatch);
            }

            // find next available batch
            MerchantIdBatches lowestBatch = BackendOps.firstMerchantIdBatchByBatchId(tableName,
                    "rangeId = '"+rangeId+"' and status = '"+DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_AVAILABLE+"'",
                    false);
            if(lowestBatch==null) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "No available batch in given range id : "+countryCode+","+rangeId);
            }

            // update status of the batch
            lowestBatch.setStatus(DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_OPEN);
            BackendOps.saveMerchantIdBatch(tableName, lowestBatch);

            // logout admin user
            BackendOps.logoutUser();

        } catch (Exception e) {
            mLogger.error("Exception in openNextMerchantIdBatch: "+e.toString());
            mLogger.flush();
            BackendOps.logoutUser();
            throw e;
        }
    }

    public void createCardIdBatches(String countryCode, String rangeId, int batchCnt, String adminPwd) {
        //initCommon();
        try {
            CommonUtils.initTableToClassMappings();
            mLogger.setProperties("admin", DbConstants.USER_TYPE_AGENT, true);
            mLogger.debug("In createCardBatches: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser("admin",adminPwd);

            String tableName = DbConstantsBackend.CARD_ID_BATCH_TABLE_NAME+countryCode;
            // get highest 'batch id' from already created batches - for given range id
            CardIdBatches highestBatch = BackendOps.firstCardIdBatchByBatchId(tableName, "rangeId = '"+rangeId+"'", true);

            int highestBatchId = (highestBatch!=null) ? highestBatch.getBatchId() : -1;
            if(highestBatchId >= BackendConstants.CARD_ID_MAX_BATCH_ID_PER_RANGE) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "All batches already available: "+countryCode+","+rangeId);
            }

            int startIdx = highestBatchId+1;
            int endIdx = startIdx+batchCnt;
            if(endIdx > BackendConstants.CARD_ID_MAX_BATCH_ID_PER_RANGE) {
                endIdx = BackendConstants.CARD_ID_MAX_BATCH_ID_PER_RANGE;
            }

            // add batches for this range
            // assuming 2 digit batch ids from 00 - 99
            for(int i=startIdx; i<endIdx; i++) {
                CardIdBatches batch = new CardIdBatches();
                batch.setStatus(DbConstantsBackend.CARD_ID_BATCH_STATUS_AVAILABLE);
                batch.setRangeId(rangeId);
                batch.setBatchId(i);
                String batchId = String.format("%03d",i);
                batch.setRangeBatchId(rangeId+batchId);
                batch.setStatusTime(new Date());
                BackendOps.saveCardIdBatch(tableName, batch);
            }

            // logout admin user
            BackendOps.logoutUser();

        } catch (Exception e) {
            mLogger.error("Exception in createCardIdBatches: "+e.toString());
            BackendOps.logoutUser();
            mLogger.flush();
            throw e;
        }
    }

    public void openNextCardIdBatch(String countryCode, String rangeId, String adminPwd) {
        //initCommon();
        try {
            CommonUtils.initTableToClassMappings();
            mLogger.setProperties("admin", DbConstants.USER_TYPE_AGENT, true);
            mLogger.debug("In openNextCardIdBatch: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser("admin",adminPwd);

            String tableName = DbConstantsBackend.CARD_ID_BATCH_TABLE_NAME+countryCode;

            // get current open batch
            CardIdBatches openBatch = BackendOps.fetchOpenCardIdBatch(tableName);
            if(openBatch!=null && !openBatch.getRangeId().equals(rangeId)) {
                // If rangeId of 'current open batch' is different from the one provided
                // first close the current open batch manually, and then try again.
                // Check added just to be more sure - when new range is being opened
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "RangeId of 'current open batch' is different : "+countryCode+","+rangeId);
            }

            // check if current open batch is still empty
            if(openBatch != null) {
                String cardIdPrefix = BackendConstants.MY_CARD_ISSUER_ID+countryCode+openBatch.getRangeBatchId();
                int cnt = BackendOps.getCardCnt("card_id like '"+cardIdPrefix+"%'");
                if(cnt < BackendConstants.CARD_ID_MAX_SNO_PER_BATCH) {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "Current open batch still empty : "+cnt+","+countryCode+","+rangeId);
                }

                // close current open batch
                openBatch.setStatus(DbConstantsBackend.CARD_ID_BATCH_STATUS_CLOSED);
                BackendOps.saveCardIdBatch(tableName, openBatch);
            }

            // find next available batch
            CardIdBatches lowestBatch = BackendOps.firstCardIdBatchByBatchId(tableName,
                    "rangeId = '"+rangeId+"' and status = '"+DbConstantsBackend.CARD_ID_BATCH_STATUS_AVAILABLE+"'",
                    false);
            if(lowestBatch==null) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "No available batch in given range id : "+countryCode+","+rangeId);
            }

            // update status of the batch
            lowestBatch.setStatus(DbConstantsBackend.CARD_ID_BATCH_STATUS_OPEN);
            BackendOps.saveCardIdBatch(tableName, lowestBatch);

            // logout admin user
            BackendOps.logoutUser();

        } catch (Exception e) {
            mLogger.error("Exception in openNextCardIdBatch: "+e.toString());
            mLogger.flush();
            BackendOps.logoutUser();
            throw e;
        }
    }

    /*
     * Private helper methods
     */
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


    /*
    public void openCardIdRange(String countryCode, String rangeId, int startIdx, int endIdx, String adminPwd) {
        initCommon();
        try {
            mLogger.debug("In openCardIdRange: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser("admin",adminPwd);

            String tableName = DbConstantsBackend.CARD_ID_BATCH_TABLE_NAME+countryCode;

            // add batches for this range
            // assuming 3 digit batch ids from 000 - 999
            for(int i=startIdx; i<=endIdx; i++) {
                CardBatches batch = new CardBatches();
                batch.setStatus(DbConstantsBackend.CARD_BATCH_STATUS_AVAILABLE);
                batch.setRangeId(rangeId);
                String batchId = String.format("%03d",i);
                batch.setBatchId(batchId);
                batch.setRangeBatchId(rangeId+batchId);
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
    }*/

