package in.myecash.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import in.myecash.common.CommonUtils;
import in.myecash.common.DateUtil;
import in.myecash.common.MyGlobalSettings;
import in.myecash.messaging.SmsConstants;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;
import in.myecash.constants.*;
import in.myecash.database.*;
import in.myecash.common.database.*;
import in.myecash.common.constants.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static in.myecash.common.constants.DbConstants.OP_RESET_PASSWD;

/**
 * Created by adgangwa on 19-07-2016.
 */
public class AdminServices implements IBackendlessService {

    private static final String ADMIN_LOGINID = "admin";
    
    private MyLogger mLogger = new MyLogger("services.AdminServices");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];

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
        registerInternalUser(argUserId, DbConstants.USER_TYPE_CNT, mobile, name, dob, pwd);
    }


    /*
     * Merchant Manual Request Operations
     */
    public void mchntSendPasswdResetHint(String merchantId, String ticketNum, String reason, String remarks, String adminPwd) {
        execMchntManualOp(DbConstants.OP_SEND_PASSWD_RESET_HINT, merchantId, ticketNum, reason, remarks, "", adminPwd);
    }
    public void mchntEnableAccount(String merchantId, String ticketNum, String reason, String remarks, String adminPwd) {
        execMchntManualOp(DbConstants.OP_ENABLE_ACC, merchantId, ticketNum, reason, remarks, "", adminPwd);
    }
    public void mchntResetTrustedDevices(String merchantId, String ticketNum, String reason, String remarks, String adminPwd) {
        execMchntManualOp(DbConstants.OP_RESET_TRUSTED_DEVICES, merchantId, ticketNum, reason, remarks, "", adminPwd);
    }
    public void mchntChangeMobileNum(String newMobile, String merchantId, String ticketNum, String reason, String remarks, String adminPwd) {
        execMchntManualOp(DbConstants.OP_CHANGE_MOBILE, merchantId, ticketNum, reason, remarks, newMobile, adminPwd);
    }
    public void mchntAccountClosure(String merchantId, String ticketNum, String reason, String remarks, String adminPwd) {
        execMchntManualOp(DbConstants.OP_ACC_CLOSURE, merchantId, ticketNum, reason, remarks, "", adminPwd);
    }
    public void mchntCancelAccountClosure(String merchantId, String ticketNum, String reason, String remarks, String adminPwd) {
        execMchntManualOp(DbConstants.OP_CANCEL_ACC_CLOSURE, merchantId, ticketNum, reason, remarks, "", adminPwd);
    }

    /*
     * Single function for all Merchant Manual Request Operations
     */
    private void execMchntManualOp(String manualOp, String merchantId, String ticketNum, String reason, String remarks,
                                   String newMobileNum, String adminPwd) {
        long startTime = System.currentTimeMillis();
        try {
            BackendUtils.initAll();
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = manualOp;
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    ticketNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    reason+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    newMobileNum;
            mEdr[BackendConstants.EDR_USER_ID_IDX] = ADMIN_LOGINID;
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_ADMIN);
            mLogger.setProperties(ADMIN_LOGINID, DbConstants.USER_TYPE_ADMIN, true);

            // login using 'admin' user
            BackendOps.loginUser(ADMIN_LOGINID,adminPwd);

            // fetch merchant object
            Merchants merchant = BackendOps.getMerchant(merchantId, true, false);
            mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();

            int oldStatus = merchant.getAdmin_status();
            Date oldUpdateTime = merchant.getStatus_update_time();
            String oldMobile = merchant.getMobile_num();
            String oldReason = merchant.getStatus_reason();
            mLogger.debug("oldStatus: "+oldStatus+", oldTime: "+oldUpdateTime.toString());

            // Add merchant op first - then update status
            MerchantOps op = new MerchantOps();
            op.setCreateTime(new Date());
            op.setMerchant_id(merchant.getAuto_id());
            op.setMobile_num(merchant.getMobile_num());
            op.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
            op.setTicketNum(ticketNum);
            op.setReason(reason);
            op.setRemarks(remarks);
            op.setAgentId(ADMIN_LOGINID);
            op.setInitiatedBy( DbConstantsBackend.USER_OP_INITBY_MCHNT);
            op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_MANUAL);
            op.setOp_code(manualOp);
            // set extra params - as per op value
            if(manualOp.equals(DbConstants.OP_CHANGE_MOBILE)) {
                String extraParams = "Old Mobile: "+oldMobile+", New Mobile: "+newMobileNum;
                op.setExtra_op_params(extraParams);
            }
            op = BackendOps.saveMerchantOp(op);

            // process as per possible manual operation
            int newStatus;
            String smsExtraParam = null;
            try {
                switch (manualOp) {
                    case DbConstants.OP_SEND_PASSWD_RESET_HINT:
                        smsExtraParam = merchant.getDob();
                        newStatus = DbConstants.USER_STATUS_ACTIVE;
                        break;

                    case DbConstants.OP_ENABLE_ACC:
                        newStatus = DbConstants.USER_STATUS_ACTIVE;
                        break;

                    case DbConstants.OP_CHANGE_MOBILE:
                        // disable if not already disabled
                        // if already disabled - fx will simply return same object
                        //merchant = BackendUtils.setMerchantStatus(merchant, DbConstants.USER_STATUS_DISABLED, reason, mEdr, mLogger);
                        merchant.setMobile_num(newMobileNum);
                        newStatus = DbConstants.USER_STATUS_ACTIVE;
                        smsExtraParam = oldMobile;
                        break;

                    case DbConstants.OP_RESET_TRUSTED_DEVICES:
                        merchant.setTrusted_devices(null);
                        newStatus = DbConstants.USER_STATUS_ACTIVE;
                        break;

                    case DbConstants.OP_ACC_CLOSURE:
                        // merchant should be in active state
                        if(merchant.getAdmin_status() != DbConstants.USER_STATUS_ACTIVE) {
                            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Merchant account is not active");
                        }
                        // disable cb and cl add
                        merchant.setCb_rate("0");
                        merchant.setCl_add_enable(false);
                        merchant.setRemoveReqDate(new Date());

                        newStatus = DbConstants.USER_STATUS_UNDER_CLOSURE;
                        break;

                    case DbConstants.OP_CANCEL_ACC_CLOSURE:
                        // merchant should be in closure state
                        if(merchant.getAdmin_status() != DbConstants.USER_STATUS_UNDER_CLOSURE) {
                            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Merchant account is not under closure state");
                        }
                        merchant.setRemoveReqDate(null);
                        newStatus = DbConstants.USER_STATUS_ACTIVE;
                        break;

                    default:
                        throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Invalid Manual operation");
                }

                // any applicable merchant updates done
                if(merchant.getAdmin_status().equals(newStatus)) {
                    // old and new status is same
                    // update merchant object
                    merchant = BackendOps.updateMerchant(merchant);
                } else {
                    // old and new status are different
                    // change status - this will save the updated merchant object too
                    merchant = BackendUtils.setMerchantStatus(merchant, newStatus, reason, mEdr, mLogger);
                }

                try {
                    sendManualReqSms(manualOp, merchant, smsExtraParam);
                } catch (Exception ex) {
                    // ignore status for all oders
                    if(manualOp.equals(DbConstants.OP_SEND_PASSWD_RESET_HINT)) {
                        // can afford to throw exception in this case
                        // as for OP_SEND_PASSWD_RESET_HINT - no change in merchant object is done
                        // so nothing to rollback - except merchant op row deletion - which is happening
                        throw ex;
                    }
                }

            } catch(Exception e) {
                mLogger.error("execMchntManualOp: Exception while processing manual op: "+manualOp+", "+merchantId);
                // Rollback - delete merchant op added
                try {
                    BackendOps.deleteMerchantOp(op);
                } catch(Exception ex) {
                    mLogger.fatal("execMchntManualOp: Failed to rollback: merchant op deletion failed: "+merchantId);
                    // Rollback also failed
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw ex;
                }
                throw e;
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendOps.logoutUser();
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    private void sendManualReqSms(String manualOp, Merchants merchant, String extraParam) {
        boolean status = true;

        switch (manualOp) {
            case DbConstants.OP_SEND_PASSWD_RESET_HINT:
                String smsText = String.format(SmsConstants.SMS_ADMIN_MCHNT_SEND_PSWD_RESET_HINT,extraParam);
                status = SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger);
                break;

            case DbConstants.OP_ENABLE_ACC:
                smsText = String.format(SmsConstants.SMS_ADMIN_MCHNT_ACC_ENABLE,CommonUtils.getPartialVisibleStr(merchant.getAuto_id()));
                status = SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger);
                break;

            case DbConstants.OP_CHANGE_MOBILE:
                // Send SMS on old and new mobile
                smsText = String.format(SmsConstants.SMS_ADMIN_MCHNT_MOBILE_CHANGE,
                        CommonUtils.getPartialVisibleStr(merchant.getAuto_id()),
                        CommonUtils.getPartialVisibleStr(merchant.getMobile_num()));
                status = SmsHelper.sendSMS(smsText, extraParam + "," + merchant.getMobile_num(), mEdr, mLogger);
                break;

            case DbConstants.OP_RESET_TRUSTED_DEVICES:
                smsText = SmsConstants.SMS_ADMIN_MCHNT_RESET_TRUSTED_DEVICES;
                status = SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger);
                break;

            case DbConstants.OP_ACC_CLOSURE:
                DateUtil now = new DateUtil(BackendConstants.TIMEZONE);
                now.addDays(MyGlobalSettings.getMchntExpiryDays());
                SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

                smsText = String.format(SmsConstants.SMS_ADMIN_MCHNT_ACC_CLOSURE,
                        CommonUtils.getPartialVisibleStr(merchant.getAuto_id()),
                        String.valueOf(MyGlobalSettings.getMchntExpiryDays()),
                        sdf.format(now.getTime()));

                status = SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger);
                break;

            case DbConstants.OP_CANCEL_ACC_CLOSURE:
                smsText = SmsConstants.SMS_ADMIN_MCHNT_CANCEL_ACC_CLOSURE;
                status = SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger);
                break;

            default:
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Invalid Manual operation");
        }

        if(!status) {
            throw new BackendlessException(String.valueOf(ErrorCodes.SEND_SMS_FAILED), "");
        }
    }

    public void createMerchantIdBatches(String countryCode, String rangeId, int batchCnt, String adminPwd) {
        long startTime = System.currentTimeMillis();
        try {
            BackendUtils.initAll();
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "createMerchantIdBatches";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = countryCode+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    rangeId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    batchCnt;
            mEdr[BackendConstants.EDR_USER_ID_IDX] = ADMIN_LOGINID;
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_ADMIN);
            mLogger.setProperties(ADMIN_LOGINID, DbConstants.USER_TYPE_ADMIN, true);

            mLogger.debug("In createMerchantIdBatches: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser(ADMIN_LOGINID,adminPwd);

            String tableName = DbConstantsBackend.MERCHANT_ID_BATCH_TABLE_NAME+countryCode;
            // get highest 'batch id' from already created batches - for given range id
            MerchantIdBatches highestBatch = BackendOps.firstMerchantIdBatchByBatchId(tableName, "rangeId = '"+rangeId+"'", true);

            int highestBatchId = (highestBatch!=null) ? highestBatch.getBatchId() : 0;
            if(highestBatchId >= BackendConstants.MERCHANT_ID_MAX_BATCH_ID_PER_RANGE) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "All batches already available: "+countryCode+","+rangeId);
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
                batch.setStatus(DbConstantsBackend.BATCH_STATUS_AVAILABLE);
                batch.setRangeId(rangeId);
                batch.setBatchId(i);
                String batchId = String.format("%02d",i);
                batch.setRangeBatchId(rangeId+batchId);
                batch.setStatusTime(new Date());
                BackendOps.saveMerchantIdBatch(tableName, batch);
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendOps.logoutUser();
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void openNextMerchantIdBatch(String countryCode, String rangeId, String adminPwd) {
        long startTime = System.currentTimeMillis();
        try {
            BackendUtils.initAll();
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "openNextMerchantIdBatch";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = countryCode+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    rangeId;
            mEdr[BackendConstants.EDR_USER_ID_IDX] = ADMIN_LOGINID;
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_ADMIN);
            mLogger.setProperties(ADMIN_LOGINID, DbConstants.USER_TYPE_ADMIN, true);

            mLogger.debug("In openNextMerchantIdBatch: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser(ADMIN_LOGINID,adminPwd);

            String tableName = DbConstantsBackend.MERCHANT_ID_BATCH_TABLE_NAME+countryCode;

            // get current open batch
            MerchantIdBatches openBatch = BackendOps.fetchMerchantIdBatch(tableName,
                    "status = '"+DbConstantsBackend.BATCH_STATUS_OPEN +"'");
            if(openBatch!=null && !openBatch.getRangeId().equals(rangeId)) {
                // If rangeId of 'current open batch' is different from the one provided
                // first close the current open batch manually, and then try again.
                // Check added just to be more sure - when new range is being opened
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "RangeId of 'current open batch' is different : "+countryCode+","+rangeId);
            }

            // check if current open batch is still empty
            if(openBatch != null) {
                String merchantIdPrefix = countryCode+openBatch.getRangeBatchId();
                int cnt = BackendOps.getMerchantCnt("auto_id like '"+merchantIdPrefix+"%'");
                if(cnt < BackendConstants.MERCHANT_ID_MAX_SNO_PER_BATCH) {
                    throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Current open batch still empty : "+cnt+","+countryCode+","+rangeId);
                }

                // close current open batch
                openBatch.setStatus(DbConstantsBackend.BATCH_STATUS_CLOSED);
                BackendOps.saveMerchantIdBatch(tableName, openBatch);
            }

            // find next available batch
            MerchantIdBatches lowestBatch = BackendOps.firstMerchantIdBatchByBatchId(tableName,
                    "rangeId = '"+rangeId+"' and status = '"+DbConstantsBackend.BATCH_STATUS_AVAILABLE +"'",
                    false);
            if(lowestBatch==null) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "No available batch in given range id : "+countryCode+","+rangeId);
            }

            // update status of the batch
            lowestBatch.setStatus(DbConstantsBackend.BATCH_STATUS_OPEN);
            BackendOps.saveMerchantIdBatch(tableName, lowestBatch);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendOps.logoutUser();
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void createCardIdBatches(String countryCode, String rangeId, int batchCnt, String adminPwd) {
        long startTime = System.currentTimeMillis();
        try {
            BackendUtils.initAll();
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "createCardIdBatches";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = countryCode+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    rangeId;
            mEdr[BackendConstants.EDR_USER_ID_IDX] = ADMIN_LOGINID;
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_ADMIN);
            mLogger.setProperties(ADMIN_LOGINID, DbConstants.USER_TYPE_ADMIN, true);

            mLogger.debug("In createCardBatches: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser(ADMIN_LOGINID,adminPwd);

            String tableName = DbConstantsBackend.CARD_ID_BATCH_TABLE_NAME+countryCode;
            // get highest 'batch id' from already created batches - for given range id
            CardIdBatches highestBatch = BackendOps.firstCardIdBatchByBatchId(tableName, "rangeId = '"+rangeId+"'", true);

            int highestBatchId = (highestBatch!=null) ? highestBatch.getBatchId() : -1;
            if(highestBatchId >= BackendConstants.CARD_ID_MAX_BATCH_ID_PER_RANGE) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "All batches already available: "+countryCode+","+rangeId);
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
                batch.setStatus(DbConstantsBackend.BATCH_STATUS_AVAILABLE);
                batch.setRangeId(rangeId);
                batch.setBatchId(i);
                String batchId = String.format("%03d",i);
                batch.setRangeBatchId(rangeId+batchId);
                batch.setStatusTime(new Date());
                BackendOps.saveCardIdBatch(tableName, batch);
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendOps.logoutUser();
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void openNextCardIdBatch(String countryCode, String rangeId, String adminPwd) {
        long startTime = System.currentTimeMillis();
        try {
            BackendUtils.initAll();
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "openNextCardIdBatch";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = countryCode+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    rangeId;
            mEdr[BackendConstants.EDR_USER_ID_IDX] = ADMIN_LOGINID;
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_ADMIN);
            mLogger.setProperties(ADMIN_LOGINID, DbConstants.USER_TYPE_ADMIN, true);

            mLogger.debug("In openNextCardIdBatch: "+countryCode+": "+rangeId);
            // login using 'admin' user
            BackendOps.loginUser(ADMIN_LOGINID,adminPwd);

            String tableName = DbConstantsBackend.CARD_ID_BATCH_TABLE_NAME+countryCode;

            // get current open batch
            CardIdBatches openBatch = BackendOps.fetchOpenCardIdBatch(tableName);
            if(openBatch!=null && !openBatch.getRangeId().equals(rangeId)) {
                // If rangeId of 'current open batch' is different from the one provided
                // first close the current open batch manually, and then try again.
                // Check added just to be more sure - when new range is being opened
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "RangeId of 'current open batch' is different : "+countryCode+","+rangeId);
            }

            // check if current open batch is still empty
            if(openBatch != null) {
                String cardIdPrefix = BackendConstants.MY_CARD_ISSUER_ID+countryCode+openBatch.getRangeBatchId();
                int cnt = BackendOps.getCardCnt("card_id like '"+cardIdPrefix+"%'");
                if(cnt < BackendConstants.CARD_ID_MAX_SNO_PER_BATCH) {
                    throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Current open batch still empty : "+cnt+","+countryCode+","+rangeId);
                }

                // close current open batch
                openBatch.setStatus(DbConstantsBackend.BATCH_STATUS_CLOSED);
                BackendOps.saveCardIdBatch(tableName, openBatch);
            }

            // find next available batch
            CardIdBatches lowestBatch = BackendOps.firstCardIdBatchByBatchId(tableName,
                    "rangeId = '"+rangeId+"' and status = '"+DbConstantsBackend.BATCH_STATUS_AVAILABLE+"'",
                    false);
            if(lowestBatch==null) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "No available batch in given range id : "+countryCode+","+rangeId);
            }

            // update status of the batch
            lowestBatch.setStatus(DbConstantsBackend.BATCH_STATUS_OPEN);
            BackendOps.saveCardIdBatch(tableName, lowestBatch);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendOps.logoutUser();
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
     * Private helper methods
     */
    private void registerInternalUser(String argUserId, int userType, String mobile, String name, String dob, String pwd) {
        long startTime = System.currentTimeMillis();
        try {
            BackendUtils.initAll();
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "registerInternalUser";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = argUserId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    userType+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    mobile+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    name+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    dob;
            mEdr[BackendConstants.EDR_USER_ID_IDX] = ADMIN_LOGINID;
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_ADMIN);
            mLogger.setProperties(ADMIN_LOGINID, DbConstants.USER_TYPE_ADMIN, true);

            mLogger.debug("In registerInternalUser: "+argUserId+": "+mobile);
            //mLogger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            String prefix = null;
            switch (userType) {
                case DbConstants.USER_TYPE_AGENT:
                    prefix = BackendConstants.PREFIX_AGENT_ID;
                    break;
                case DbConstants.USER_TYPE_CC:
                    prefix = BackendConstants.PREFIX_CC_ID;
                    break;
                case DbConstants.USER_TYPE_CNT:
                    prefix = BackendConstants.PREFIX_CCNT_ID;
                    break;
            }
            String userId = prefix+argUserId;
            if(userId.length() != CommonConstants.INTERNAL_USER_ID_LEN) {
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "User ID length is wrong");
            }

            // login using 'admin' user
            BackendOps.loginUser(ADMIN_LOGINID,pwd);
            //mLogger.debug("Before2: "+ HeadersManager.getInstance().getHeaders().toString());

            // Create agent object and register
            InternalUser internalUser = new InternalUser();
            internalUser.setId(userId);
            internalUser.setMobile_num(mobile);
            internalUser.setDob(dob);
            internalUser.setName(name);
            internalUser.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
            internalUser.setStatus_reason(DbConstantsBackend.ENABLED_ACTIVE);

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
                case DbConstants.USER_TYPE_CNT:
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
            if (!SmsHelper.sendSMS(smsText, mobile, mEdr, mLogger)) {
                // TODO: write to alarm table for retry later
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendOps.logoutUser();
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }
}

    /*
    public void openMerchantIdBatch(String countryCode, String rangeId, String batchId, String adminPwd) {
        initCommon();
        try {
            mLogger.debug("In openMerchantIdBatch: "+countryCode+","+rangeId+","+batchId);
            // login using 'admin' user
            BackendOps.loginUser(ADMIN_LOGINID,adminPwd);

            // make sure batch is not already open
            String tableNameMerchantIds = "MerchantIds"+countryCode+rangeId;
            if(BackendOps.merchantIdBatchOpen(tableNameMerchantIds, batchId)) {
                throw CommonUtils.getException(BackendResponseCodes.OPERATION_NOT_ALLOWED, "Batch is already open: "+countryCode+","+rangeId+","+batchId);
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
                        openBatch.setStatus(DbConstantsBackend.BATCH_STATUS_CLOSED);
                        BackendOps.saveMerchantIdBatch(tableNameBatches, openBatch);
                        mLogger.info("Closed batch "+openBatch.getBatchId()+" in table "+tableNameBatches);
                    }
                }
            }

            // fetch new batch object
            MerchantIdBatches batch = BackendOps.fetchMerchantIdBatch(tableNameBatches, rangeId, batchId);
            if(batch.getStatus().equals(DbConstantsBackend.BATCH_STATUS_CLOSED)) {
                throw CommonUtils.getException(BackendResponseCodes.OPERATION_NOT_ALLOWED, "Invalid new batch status: "+countryCode+","+rangeId+","+batchId);
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
            if(batch.getStatus().equals(DbConstantsBackend.BATCH_STATUS_CLOSED)) {
                batch.setStatus(DbConstantsBackend.BATCH_STATUS_OPEN);
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
            BackendOps.loginUser(ADMIN_LOGINID,adminPwd);

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

    /*private String resetMchntPassword(Merchants merchant) {

        // check if any request already pending
        if( BackendOps.findActiveMchntPwdResetReqs(merchant.getAuto_id()) != null) {
            throw new BackendlessException(String.valueOf(ErrorCodes.DUPLICATE_ENTRY), "");
        }

        // fetch user with the given id with related merchant object
        BackendlessUser user = BackendOps.fetchUser(merchant.getAuto_id(), DbConstants.USER_TYPE_MERCHANT, false);
        int userType = (Integer)user.getProperty("user_type");
        if(userType != DbConstants.USER_TYPE_MERCHANT) {
            mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED),merchant.getAuto_id()+" is not a merchant.");
        }

        // reset password immediatly
        // generate password
        String passwd = BackendUtils.generateTempPassword();
        // update user account for the password
        user.setPassword(passwd);
        BackendOps.updateUser(user);

        return passwd;
    }*/

    /*
     * Performs either of below two operations
     *
     * 1) only reset login data i.e.
     * Delete all trusted devices, and set status to indicate that 'forget password' is allowed from
     * non-trusted devices also - first time only - just like in case of first login.
     *
     * 2) Change mobile number
     * This involves above 'reset login data' operation also.
     *
     * These ops are always done - upon manual submission of application and documents.
     */
    /*public void resetMchntLoginOrChangeMob(String merchantId, String ticketNum, String reason, String remarks,
                                           String newMobileNum, String adminPwd) {
        long startTime = System.currentTimeMillis();
        try {
            BackendUtils.initAll();
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "resetMchntLoginOrChangeMob";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    ticketNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    reason+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    newMobileNum;
            mEdr[BackendConstants.EDR_USER_ID_IDX] = ADMIN_LOGINID;
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_ADMIN);
            mLogger.setProperties(ADMIN_LOGINID, DbConstants.USER_TYPE_ADMIN, true);

            // just for easier comparisons later on - change empty string to null
            if(newMobileNum!=null && newMobileNum.isEmpty()) {
                newMobileNum = null;
            }

            // login using 'admin' user
            BackendOps.loginUser(ADMIN_LOGINID,adminPwd);

            // fetch user with the given id with related merchant object
            Merchants merchant = BackendOps.getMerchant(merchantId, true, false);
            mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();

            // merchant should be in disabled state
            if(merchant.getAdmin_status() != DbConstants.USER_STATUS_DISABLED) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Merchant account is not disabled yet");
            }

            int oldStatus = merchant.getAdmin_status();
            Date oldUpdateTime = merchant.getStatus_update_time();
            String oldMobile = merchant.getMobile_num();
            String oldReason = merchant.getStatus_reason();
            mLogger.debug("oldStatus: "+oldStatus+", oldTime: "+oldUpdateTime.toString());

            // Add merchant op first - then update status
            MerchantOps op = new MerchantOps();
            op.setCreateTime(new Date());
            op.setMerchant_id(merchant.getAuto_id());
            op.setMobile_num(merchant.getMobile_num());
            op.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
            op.setTicketNum(ticketNum);
            op.setReason(reason);
            op.setRemarks(remarks);
            op.setAgentId(ADMIN_LOGINID);
            op.setInitiatedBy( DbConstantsBackend.USER_OP_INITBY_MCHNT);
            op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_MANUAL);
            if(newMobileNum==null) {
                op.setOp_code(DbConstants.OP_RESET_ACC_FOR_LOGIN);
            } else {
                op.setOp_code(DbConstants.OP_CHANGE_MOBILE);
                // set extra params in presentable format
                String extraParams = "Old Mobile: "+oldMobile+", New Mobile: "+newMobileNum;
                op.setExtra_op_params(extraParams);
                //op.setExtra_op_params(newMobileNum);
            }
            op = BackendOps.saveMerchantOp(op);

            // update merchant status (and mobile num, if required)
            try {
                if(newMobileNum!=null) {
                    merchant.setMobile_num(newMobileNum);
                }
                BackendUtils.setMerchantStatus(merchant, DbConstants.USER_STATUS_READY_TO_ACTIVE, reason,
                        mEdr, mLogger);

            } catch(Exception e) {
                mLogger.error("resetMchntLoginOrChangeMob: Exception while updating merchant status: "+merchantId);
                // Rollback - delete merchant op added
                try {
                    BackendOps.deleteMerchantOp(op);
                } catch(Exception ex) {
                    mLogger.fatal("resetMchntLoginOrChangeMob: Failed to rollback: merchant op deletion failed: "+merchantId);
                    // Rollback also failed
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw ex;
                }
                throw e;
            }

            // Delete all trusted devices
            try {
                List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
                int i = 0;
                if (trustedDevices.size() > 0) {
                    mLogger.debug("Available devices: " + trustedDevices.size());
                    // iterate and delete one by one
                    for (MerchantDevice device : trustedDevices) {
                        BackendOps.deleteMchntDevice(device);
                        i++;
                    }
                }
                mLogger.debug("Deleted devices: " + i);
            } catch(Exception e) {
                mLogger.error("resetMchntLoginOrChangeMob: Exception while deleting trusted devices: "+merchantId);
                // Rollback - delete merchant op added, and rollback merchant status
                try {
                    BackendOps.deleteMerchantOp(op);
                    if(newMobileNum!=null) {
                        merchant.setMobile_num(oldMobile);
                    }
                    BackendUtils.setMerchantStatus(merchant, oldStatus, oldReason,
                            mEdr, mLogger);
                } catch(Exception ex) {
                    mLogger.fatal("resetMchntLoginOrChangeMob: Failed to rollback: "+merchantId);
                    // Rollback also failed
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw ex;
                }
                throw e;
            }

            // send SMS
            String smsText = null;
            if(newMobileNum==null) {
                smsText = String.format(SmsConstants.SMS_MCHNT_LOGIN_RESET, CommonUtils.getPartialVisibleStr(merchantId));
            } else {
                smsText = String.format(SmsConstants.SMS_MCHNT_MOBILE_CHANGE_ADMIN, CommonUtils.getPartialVisibleStr(merchantId), newMobileNum);
            }

            SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendOps.logoutUser();
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void removeMerchant(String merchantId, String ticketNum, String reason, String remarks, String adminPwd) {
        long startTime = System.currentTimeMillis();
        try {
            BackendUtils.initAll();
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "removeMerchant";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    ticketNum;
            mEdr[BackendConstants.EDR_USER_ID_IDX] = ADMIN_LOGINID;
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_ADMIN);
            mLogger.setProperties(ADMIN_LOGINID, DbConstants.USER_TYPE_ADMIN, true);

            // login using 'admin' user
            BackendOps.loginUser(ADMIN_LOGINID,adminPwd);

            // fetch user with the given id with related merchant object
            Merchants merchant = BackendOps.getMerchant(merchantId, true, false);
            mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();

            // merchant should be in active state
            if(merchant.getAdmin_status() != DbConstants.USER_STATUS_ACTIVE) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Merchant account is not active");
            }

            int oldStatus = merchant.getAdmin_status();
            Date oldUpdateTime = merchant.getStatus_update_time();
            mLogger.debug("oldStatus: "+oldStatus+", oldTime: "+oldUpdateTime.toString());

            // Add merchant op first - then update status
            MerchantOps op = new MerchantOps();
            op.setCreateTime(new Date());
            op.setMerchant_id(merchant.getAuto_id());
            op.setMobile_num(merchant.getMobile_num());
            op.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
            op.setTicketNum(ticketNum);
            op.setReason(reason);
            op.setRemarks(remarks);
            op.setAgentId(ADMIN_LOGINID);
            op.setInitiatedBy( DbConstantsBackend.USER_OP_INITBY_MCHNT);
            op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_MANUAL);
            op.setOp_code(DbConstants.OP_REMOVE_ACC);
            op = BackendOps.saveMerchantOp(op);

            // update merchant status (and mobile num, if required)
            try {
                // disable cb and cl add
                merchant.setCb_rate("0");
                merchant.setCl_add_enable(false);
                // set time when request is made
                merchant.setRemoveReqDate(new Date());
                // update status and save merchant object
                BackendUtils.setMerchantStatus(merchant, DbConstants.USER_STATUS_UNDER_CLOSURE, reason,
                        mEdr, mLogger);

            } catch(Exception e) {
                mLogger.error("removeMerchant: Exception while updating merchant status: "+merchantId);
                // Rollback - delete merchant op added
                try {
                    BackendOps.deleteMerchantOp(op);
                } catch(Exception ex) {
                    mLogger.fatal("removeMerchant: Failed to rollback: merchant op deletion failed: "+merchantId);
                    // Rollback also failed
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw ex;
                }
                throw e;
            }

            DateUtil now = new DateUtil(BackendConstants.TIMEZONE);
            now.addDays(MyGlobalSettings.getMchntExpiryDays());
            SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

            // send SMS
            String smsText = String.format(SmsConstants.SMS_MERCHANT_REMOVE,
                    merchantId,
                    String.valueOf(MyGlobalSettings.getMchntExpiryDays()),
                    sdf.format(now.getTime()));

            SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendOps.logoutUser();
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }*/

