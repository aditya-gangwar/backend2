package in.myecash.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.FilePermission;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import in.myecash.common.CommonUtils;
import in.myecash.common.MyCardForAction;
import in.myecash.messaging.SmsConstants;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    public List<MyCardForAction> execActionForCards(String codes, String action, String allotToUserId) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "execActionForCards";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = action+BackendConstants.BACKEND_EDR_SUB_DELIMETER+allotToUserId;

        try {
            // Fetch user
            InternalUser internalUser = (InternalUser) BackendUtils.fetchCurrentUser(null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            // check for allowed roles and their actions
            switch (userType) {
                case DbConstants.USER_TYPE_CCNT:
                    if( !action.equals(CommonConstants.CARDS_UPLOAD_TO_POOL) &&
                            !action.equals(CommonConstants.CARDS_ALLOT_TO_AGENT) &&
                            !action.equals(CommonConstants.CARDS_RETURN_BY_AGENT) ) {
                        throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Action not allowed to this user type");
                    }
                    break;

                case DbConstants.USER_TYPE_AGENT:
                    if( !action.equals(CommonConstants.CARDS_ALLOT_TO_MCHNT) &&
                            !action.equals(CommonConstants.CARDS_RETURN_BY_MCHNT) ) {
                        throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Action not allowed to this user type");
                    }
                    break;

                case DbConstants.USER_TYPE_CC:
                case DbConstants.USER_TYPE_CUSTOMER:
                case DbConstants.USER_TYPE_MERCHANT:
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                    throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            List<MyCardForAction> actionResults = null;

            // Loop on all given codes
            String[] csvFields = codes.split(CommonConstants.CSV_DELIMETER, -1);
            for (String code : csvFields) {
                if(code==null || code.isEmpty()) {
                    continue;
                }
                if(actionResults==null) {
                    actionResults = new ArrayList<>(csvFields.length);
                }

                MyCardForAction cardForAction = new MyCardForAction();
                cardForAction.setScannedCode(code);
                try {
                    // find card row against this code
                    CustomerCards card = BackendOps.getCustomerCard(code,true);
                    int curStatus = card.getStatus();
                    cardForAction.setCardNum(card.getCard_id());

                    // update card row - as per requested action
                    boolean updateCard = false;
                    switch (action) {
                        case CommonConstants.CARDS_UPLOAD_TO_POOL:
                            if(curStatus==DbConstants.CUSTOMER_CARD_STATUS_FOR_PRINT) {
                                card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_NEW);
                                card.setCcntId(internalUser.getId());
                                updateCard = true;
                            } else {
                                cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_WRONG_STATUS);
                            }
                            break;

                        case CommonConstants.CARDS_ALLOT_TO_AGENT:
                            if(curStatus==DbConstants.CUSTOMER_CARD_STATUS_NEW) {
                                // Find agent whom to allot
                                InternalUser iu = BackendOps.getInternalUser(allotToUserId);
                                if(BackendUtils.getUserType(iu.getId()) != DbConstants.USER_TYPE_AGENT) {
                                    cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_WRONG_ALLOT);
                                } else {
                                    BackendUtils.checkInternalUserStatus(iu);

                                    card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_WITH_AGENT);
                                    card.setCcntId(internalUser.getId());
                                    card.setAgentId(iu.getId());
                                    updateCard = true;
                                }
                            } else {
                                cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_WRONG_STATUS);
                            }
                            break;

                        case CommonConstants.CARDS_ALLOT_TO_MCHNT:
                            if(curStatus==DbConstants.CUSTOMER_CARD_STATUS_WITH_AGENT) {
                                // Find merchant whom to allot
                                Merchants mchnt = BackendOps.getMerchant(allotToUserId,false,false);
                                if(BackendUtils.getUserType(mchnt.getAuto_id()) != DbConstants.USER_TYPE_MERCHANT) {
                                    cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_WRONG_ALLOT);
                                } else {
                                    BackendUtils.checkMerchantStatus(mchnt, mEdr, mLogger);
                                    card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_WITH_MERCHANT);
                                    card.setAgentId(internalUser.getId());
                                    card.setMchntId(mchnt.getAuto_id());
                                    updateCard = true;
                                }
                            } else {
                                cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_WRONG_STATUS);
                            }
                            break;

                        case CommonConstants.CARDS_RETURN_BY_MCHNT:
                            if(curStatus==DbConstants.CUSTOMER_CARD_STATUS_WITH_MERCHANT) {
                                card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_WITH_AGENT);
                                card.setAgentId(internalUser.getId());
                                updateCard = true;
                            } else {
                                cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_WRONG_STATUS);
                            }
                            break;

                        case CommonConstants.CARDS_RETURN_BY_AGENT:
                            if(curStatus==DbConstants.CUSTOMER_CARD_STATUS_WITH_AGENT) {
                                card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_NEW);
                                card.setCcntId(internalUser.getId());
                                updateCard = true;
                            } else {
                                cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_WRONG_STATUS);
                            }
                            break;
                    }

                    if(updateCard) {
                        cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_OK);
                        card.setStatus_update_time(new Date());
                        card.setStatus_reason("");
                        BackendOps.saveCustomerCard(card);
                    }

                } catch(Exception e) {
                    // ignore error - only set action result accordingly
                    if( e instanceof BackendlessException ) {
                        int beCode = Integer.parseInt( ((BackendlessException) e).getCode() );
                        switch (beCode) {
                            case ErrorCodes.NO_SUCH_CARD:
                                cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_NSC);
                                break;
                            case ErrorCodes.NO_SUCH_USER:
                                cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_NSA);
                                break;
                            case ErrorCodes.USER_ACC_DISABLED:
                            case ErrorCodes.USER_ACC_LOCKED:
                                cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_WRONG_ALLOT_STATUS);
                                break;
                            default:
                                mLogger.error("execActionForCards: BE exception",e);
                                cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_ERROR);
                        }
                    } else {
                        mLogger.error("execActionForCards: Exception",e);
                        cardForAction.setActionStatus(MyCardForAction.ACTION_STATUS_ERROR);
                    }
                }

                // Add to result
                actionResults.add(cardForAction);
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

            /*if(actionResults!=null) {
                for (MyCardForAction card :
                        actionResults) {
                    mLogger.debug(card.getActionStatus() + "," + card.getScannedCode() + "," + card.getCardNum());
                }
            }*/
            return actionResults;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

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
            InternalUser agent = (InternalUser) BackendUtils.fetchCurrentUser(DbConstants.USER_TYPE_AGENT, mEdr, mLogger, false);

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
            mLogger.debug("File rename done");

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
            merchant.setCl_credit_limit_for_pin(-1);
            merchant.setCl_debit_limit_for_pin(-1);
            merchant.setCb_debit_limit_for_pin(-1);
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
            mLogger.debug("Register success");
            // register successful - can write to edr now
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
                //FilePermission.READ.denyForRole( DbConstantsBackend.ROLE_MERCHANT, fileDir);
                FilePermission.READ.grantForUser( user.getObjectId(), fileDir);
                //FilePermission.DELETE.grantForUser( user.getObjectId(), fileDir);
                //FilePermission.WRITE.grantForUser( user.getObjectId(), fileDir);
                mLogger.debug("Saved dummy txn csv file: " + filePath);

                fileDir = CommonUtils.getTxnImgDir(merchantId);
                filePath = fileDir + CommonConstants.FILE_PATH_SEPERATOR+BackendConstants.DUMMY_FILENAME;
                Backendless.Files.saveFile(filePath, BackendConstants.DUMMY_DATA.getBytes("UTF-8"), true);
                // Give write access to this merchant to this directory
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
            InternalUser internalUser = (InternalUser) BackendUtils.fetchCurrentUser(
                    null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            if( userType!=DbConstants.USER_TYPE_CC && userType!=DbConstants.USER_TYPE_ADMIN ) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // Fetch merchant
            Merchants merchant = BackendOps.getMerchant(merchantId, false, false);

            if(merchant.getAdmin_status()!=DbConstants.USER_STATUS_ACTIVE) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Merchant is not Active.");
            }

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

    public void disableCustomer(boolean ltdModeCase, String privateId, String ticketNum, String reason, String remarks) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "disableCustomer";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = ltdModeCase+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                privateId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                ticketNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                reason;

        try {
            // Fetch customer care user
            InternalUser internalUser = (InternalUser) BackendUtils.fetchCurrentUser(null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            if( userType!=DbConstants.USER_TYPE_CC && userType!=DbConstants.USER_TYPE_ADMIN) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // Fetch customer
            Customers customer = BackendOps.getCustomer(privateId, BackendConstants.ID_TYPE_AUTO, false);

            if(customer.getAdmin_status()!=DbConstants.USER_STATUS_ACTIVE) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Customer is not Active.");
            }

            // Add customer op first - then update status
            CustomerOps op = new CustomerOps();
            op.setCreateTime(new Date());
            op.setPrivateId(privateId);
            op.setMobile_num(customer.getMobile_num());
            if(ltdModeCase) {
                op.setOp_code(DbConstants.OP_LIMITED_MODE_ACC);
            } else {
                op.setOp_code(DbConstants.OP_DISABLE_ACC);
            }
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
                if(ltdModeCase) {
                    BackendUtils.setCustomerStatus(customer, DbConstants.USER_STATUS_LIMITED_CREDIT_ONLY, reason, mEdr, mLogger);
                } else {
                    BackendUtils.setCustomerStatus(customer, DbConstants.USER_STATUS_DISABLED, reason, mEdr, mLogger);
                }
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
            String smsText = null;
            if(ltdModeCase) {
                smsText = String.format(SmsConstants.SMS_ACCOUNT_LIMITED_MODE, CommonUtils.getPartialVisibleStr(customer.getMobile_num()));
            } else {
                smsText = String.format(SmsConstants.SMS_ACCOUNT_DISABLE, CommonUtils.getPartialVisibleStr(customer.getMobile_num()));
            }
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

    public void disableCustCard(String privateId, String cardNum, String ticketNum, String reason, String remarks) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "disableCustCard";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = cardNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                privateId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                ticketNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                reason;

        try {
            // Fetch customer care user
            InternalUser internalUser = (InternalUser) BackendUtils.fetchCurrentUser(null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            if( userType!=DbConstants.USER_TYPE_CC && userType!=DbConstants.USER_TYPE_ADMIN) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // Fetch customer
            Customers customer = BackendOps.getCustomer(privateId, BackendConstants.ID_TYPE_AUTO, true);
            CustomerCards card = customer.getMembership_card();

            if(!card.getCard_id().equals(cardNum)) {
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Wrong Card Number");
            }
            if(card.getStatus()!=DbConstants.CUSTOMER_CARD_STATUS_ACTIVE) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Customer Card is not Active");
            }

            // Add customer op first - then update status
            CustomerOps op = new CustomerOps();
            op.setCreateTime(new Date());
            op.setPrivateId(privateId);
            op.setMobile_num(customer.getMobile_num());
            op.setOp_code(DbConstants.OP_DISABLE_CARD);
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
            String extra = "Card Num: "+card.getCard_id();
            op.setExtra_op_params(extra);
            op = BackendOps.saveCustomerOp(op);

            // Update Card Object
            try {
                card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_DISABLED);
                card.setStatus_update_time(new Date());
                card.setStatus_reason(reason);
                BackendOps.saveCustomerCard(card);

            } catch(Exception e) {
                mLogger.error("disableCustCard: Exception while updating card status: "+privateId);
                // Rollback - delete merchant op added
                try {
                    BackendOps.deleteCustomerOp(op);
                } catch(Exception ex) {
                    mLogger.fatal("disableCustCard: Failed to rollback: merchant op deletion failed: "+privateId);
                    // Rollback also failed
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw ex;
                }
                throw e;
            }

            // send SMS
            String smsText = String.format(SmsConstants.SMS_DISABLE_CARD, customer.getFirstName(),
                    CommonUtils.getPartialVisibleStr(card.getCard_id()));
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

