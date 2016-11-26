package in.myecash.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.FilePermission;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import in.myecash.common.CommonUtils;
import in.myecash.messaging.SmsConstants;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

import java.util.Date;

import in.myecash.common.database.*;
import in.myecash.common.constants.*;
import in.myecash.constants.*;
import in.myecash.database.*;

/**
 * Created by adgangwa on 12-08-2016.
 */
public class InternalUserServices implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.AgentServicesNoLogin");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     */
    public String registerMerchant(Merchants merchant)
    {
        BackendUtils.initAll();
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
            InternalUser agent = (InternalUser) BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_AGENT, mEdr, mLogger, false);

            // Fetch city
            Cities city = BackendOps.fetchCity(merchant.getAddress().getCity());

            // get open merchant id batch
            String countryCode = city.getCountryCode();
            String batchTableName = DbConstantsBackend.MERCHANT_ID_BATCH_TABLE_NAME+countryCode;
            String whereClause = "status = '"+DbConstantsBackend.BATCH_STATUS_OPEN +"'";
            MerchantIdBatches batch = BackendOps.fetchMerchantIdBatch(batchTableName,whereClause);
            if(batch == null) {
                throw new BackendlessException(String.valueOf(ErrorCodes.MERCHANT_ID_RANGE_ERROR),
                        "No open merchant id batch available: "+batchTableName+","+whereClause);
            }

            // get merchant counter value and use the same to generate merchant id
            Long merchantCnt =  BackendOps.fetchCounterValue(DbConstantsBackend.MERCHANT_ID_COUNTER);
            mLogger.debug("Fetched merchant cnt: "+merchantCnt);
            // generate merchant id
            merchantId = BackendUtils.generateMerchantId(batch, countryCode, merchantCnt);
            mLogger.debug("Generated merchant id: "+merchantId);

            // rename mchnt dp to include 'merchant id'
            String currFilePath = CommonConstants.MERCHANT_DISPLAY_IMAGES_DIR + merchant.getDisplayImage();
            String newName = BackendUtils.getMchntDpFilename(merchantId);
            BackendOps.renameFile(currFilePath, newName);
            merchant.setDisplayImage(newName);

            // set or update other fields
            merchant.setAuto_id(merchantId);
            merchant.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
            merchant.setStatus_reason(DbConstantsBackend.ENABLED_ACTIVE);
            merchant.setStatus_update_time(new Date());
            merchant.setLastRenewDate(new Date());
            //merchant.setAdmin_remarks("New registered merchant");
            merchant.setMobile_num(merchant.getMobile_num());
            merchant.setFirst_login_ok(false);
            merchant.setAgentId(agent.getId());
            // set cashback and transaction table names
            //setCbAndTransTables(merchant, merchantCnt);
            merchant.setCashback_table(DbConstantsBackend.CASHBACK_TABLE_NAME + city.getCbTableCode());
            BackendOps.describeTable(merchant.getCashback_table()); // just to check that the table exists
            merchant.setTxn_table(DbConstantsBackend.TRANSACTION_TABLE_NAME + city.getCbTableCode());
            BackendOps.describeTable(merchant.getTxn_table()); // just to check that the table exists

            // generate and set password
            String pwd = BackendUtils.generateTempPassword();
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
                throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), e.toString());
            }

            // send SMS with user id
            String smsText = String.format(SmsConstants.SMS_MERCHANT_ID_FIRST, merchantId);
            SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger, true);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return merchantId;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            if(merchantId!=null && !merchantId.isEmpty()) {
                BackendOps.decrementCounterValue(DbConstantsBackend.MERCHANT_ID_COUNTER);
            }
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void disableMerchant(String merchantId, String ticketNum, String reason, String remarks) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "disableMerchant";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                ticketNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                reason;

        try {
            // Fetch customer care user
            InternalUser internalUser = (InternalUser) BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            if( userType!=DbConstants.USER_TYPE_CC && userType!=DbConstants.USER_TYPE_ADMIN ) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // Fetch merchant
            Merchants merchant = BackendOps.getMerchant(merchantId, false, false);

            // Add merchant op first - then update status
            MerchantOps op = new MerchantOps();
            op.setCreateTime(new Date());
            op.setMerchant_id(merchant.getAuto_id());
            op.setMobile_num(merchant.getMobile_num());
            op.setOp_code(DbConstants.OP_DISABLE_ACC);
            op.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
            op.setTicketNum(ticketNum);
            op.setReason(reason);
            op.setRemarks(remarks);
            op.setAgentId(internalUser.getId());
            op.setInitiatedBy( (userType==DbConstants.USER_TYPE_CC)?
                    DbConstantsBackend.USER_OP_INITBY_MCHNT :
                    DbConstantsBackend.USER_OP_INITBY_ADMIN);
            if(userType==DbConstants.USER_TYPE_CC) {
                op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_CC);
            }
            op = BackendOps.saveMerchantOp(op);

            // Update status
            try {
                BackendUtils.setMerchantStatus(merchant, DbConstants.USER_STATUS_DISABLED, reason,
                        mEdr, mLogger);
                /*merchant.setAdmin_status(DbConstants.USER_STATUS_DISABLED);
                merchant.setStatus_update_time(new Date());
                merchant.setStatus_reason(reason);
                merchant = BackendOps.updateMerchant(merchant);*/
            } catch(Exception e) {
                mLogger.error("disableMerchant: Exception while updating merchant status: "+merchantId);
                // Rollback - delete merchant op added
                try {
                    BackendOps.deleteMerchantOp(op);
                } catch(Exception ex) {
                    mLogger.fatal("disableMerchant: Failed to rollback: merchant op deletion failed: "+merchantId);
                    // Rollback also failed
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw ex;
                }
                throw e;
            }

            // send SMS
            String smsText = String.format(SmsConstants.SMS_ACCOUNT_DISABLE, CommonUtils.getPartialVisibleStr(merchantId));
            SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger, true);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void disableCustomer(String privateId, String ticketNum, String reason, String remarks) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "disableCustomer";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = privateId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                ticketNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                reason;

        try {
            // Fetch customer care user
            InternalUser internalUser = (InternalUser) BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            if( userType!=DbConstants.USER_TYPE_CC && userType!=DbConstants.USER_TYPE_ADMIN) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // Fetch customer
            Customers customer = BackendOps.getCustomer(privateId, BackendConstants.ID_TYPE_AUTO, false);

            // Add customer op first - then update status
            CustomerOps op = new CustomerOps();
            op.setCreateTime(new Date());
            op.setPrivateId(privateId);
            op.setMobile_num(customer.getMobile_num());
            op.setOp_code(DbConstants.OP_DISABLE_ACC);
            op.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
            op.setTicketNum(ticketNum);
            op.setReason(reason);
            op.setRemarks(remarks);
            op.setRequestor_id(internalUser.getId());
            op.setInitiatedBy( (userType==DbConstants.USER_TYPE_CC)?
                    DbConstantsBackend.USER_OP_INITBY_MCHNT :
                    DbConstantsBackend.USER_OP_INITBY_ADMIN);
            if(userType==DbConstants.USER_TYPE_CC) {
                op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_CC);
            }
            op = BackendOps.saveCustomerOp(op);

            // Update status
            try {
                BackendUtils.setCustomerStatus(customer, DbConstants.USER_STATUS_DISABLED, reason,
                        mEdr, mLogger);
            } catch(Exception e) {
                mLogger.error("disableMerchant: Exception while updating merchant status: "+privateId);
                // Rollback - delete merchant op added
                try {
                    BackendOps.deleteCustomerOp(op);
                } catch(Exception ex) {
                    mLogger.fatal("disableMerchant: Failed to rollback: merchant op deletion failed: "+privateId);
                    // Rollback also failed
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw ex;
                }
                throw e;
            }

            // send SMS
            String smsText = String.format(SmsConstants.SMS_ACCOUNT_DISABLE, CommonUtils.getPartialVisibleStr(customer.getMobile_num()));
            SmsHelper.sendSMS(smsText, customer.getMobile_num(), mEdr, mLogger, true);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
     * Private helper methods
     */
    /*
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
    }*/

//    private void rollbackRegister(BackendlessUser user) {
    private void rollbackRegister(String mchntId) {
        // TODO: add as 'Major' alarm - user to be removed later manually
        // rollback to not-usable state
        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
        try {
            Merchants merchant = BackendOps.getMerchant(mchntId, false, false);
            BackendUtils.setMerchantStatus(merchant, DbConstants.USER_STATUS_REG_ERROR, DbConstantsBackend.REG_ERROR_REG_FAILED,
                    mEdr, mLogger);

            /*merchant.setAdmin_status(DbConstants.USER_STATUS_REG_ERROR);
            merchant.setStatus_reason(DbConstantsBackend.REG_ERROR_REG_FAILED);
            BackendOps.updateMerchant(merchant);*/
        } catch(Exception ex) {
            mLogger.fatal("registerMerchant: Merchant Rollback failed: "+ex.toString());
            mLogger.error(BackendUtils.stackTraceStr(ex));
            mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
            throw ex;
        }
    }
}

