package com.myecash.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.myecash.constants.*;
import com.myecash.database.InternalUser;
import com.myecash.database.CardIdBatches;
import com.myecash.database.MerchantIdBatches;
import com.myecash.messaging.SmsConstants;
import com.myecash.messaging.SmsHelper;
import com.myecash.utilities.BackendOps;
import com.myecash.utilities.CommonUtils;
import com.myecash.utilities.MyLogger;

import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 19-07-2016.
 */
public class AdminServices implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.AdminServices");

    /*
     * Public methods: Backend REST APIs
     */
    public void registerAgent(String argUserId, String mobile, String name, String dob, String pwd) {
        registerInternalUser(argUserId, DbConstants.USER_TYPE_AGENT, mobile, name, dob, pwd);
    }

    public void registerCCUser(String argUserId, String mobile, String name, String dob, String pwd) {
        registerInternalUser(argUserId, DbConstants.USER_TYPE_CC, mobile, name, dob, pwd);
    }

    public void registerCCntUser(String argUserId, String mobile, String name, String dob, String pwd) {
        registerInternalUser(argUserId, DbConstants.USER_TYPE_CCNT, mobile, name, dob, pwd);
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
    private void registerInternalUser(String argUserId, int userType, String mobile, String name, String dob, String pwd) {
        //initCommon();
        try {
            CommonUtils.initTableToClassMappings();
            mLogger.setProperties("admin", userType, true);
            mLogger.debug("In registerInternalUser: "+argUserId+": "+mobile);
            //mLogger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            String prefix = null;
            switch (userType) {
                case DbConstants.USER_TYPE_AGENT:
                    prefix = CommonConstants.PREFIX_AGENT_ID;
                    break;
                case DbConstants.USER_TYPE_CC:
                    prefix = CommonConstants.PREFIX_CC_ID;
                    break;
                case DbConstants.USER_TYPE_CCNT:
                    prefix = CommonConstants.PREFIX_CCNT_ID;
                    break;
            }
            String userId = prefix+argUserId;
            if(userId.length() != CommonConstants.INTERNAL_USER_ID_LEN) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA, "User ID length is wrong");
            }

            // login using 'admin' user
            BackendOps.loginUser("admin",pwd);
            //mLogger.debug("Before2: "+ HeadersManager.getInstance().getHeaders().toString());

            // Create agent object and register
            InternalUser internalUser = new InternalUser();
            internalUser.setId(userId);
            internalUser.setMobile_num(mobile);
            internalUser.setDob(dob);
            internalUser.setName(name);
            internalUser.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
            internalUser.setStatus_reason(DbConstants.ENABLED_ACTIVE);

            BackendlessUser backendlessUser = new BackendlessUser();
            backendlessUser.setProperty("user_id", userId);
            backendlessUser.setPassword(dob);
            backendlessUser.setProperty("user_type", userType);
            backendlessUser.setProperty("internalUser",internalUser);

            // print roles - for debug purpose
            List<String> roles = Backendless.UserService.getUserRoles();
            mLogger.debug("Roles: "+roles.toString());

            // register the user
            backendlessUser = BackendOps.registerUser(backendlessUser);
            mLogger.debug("Internal User Registration successful");

            // assign role
            String role = null;
            switch (userType) {
                case DbConstants.USER_TYPE_AGENT:
                    role = BackendConstants.ROLE_AGENT;
                    break;
                case DbConstants.USER_TYPE_CC:
                    role = BackendConstants.ROLE_CC;
                    break;
                case DbConstants.USER_TYPE_CCNT:
                    role = BackendConstants.ROLE_CCNT;
                    break;
            }
            try {
                BackendOps.assignRole(userId, role);
            } catch (Exception e) {
                // TODO: add as 'Major' alarm - user to be removed later manually
                throw e;
            }

            // Send sms to the customer with PIN
            String smsText = String.format(SmsConstants.SMS_REG_INTERNAL_USER, userId);
            if (!SmsHelper.sendSMS(smsText, mobile, mLogger)) {
                // TODO: write to alarm table for retry later
            }

            // logout admin user
            BackendOps.logoutUser();

        } catch (Exception e) {
            mLogger.error("Exception in registerInternalUser: "+e.toString());
            BackendOps.logoutUser();
            mLogger.flush();
            throw e;
        }
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

