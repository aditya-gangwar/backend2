package in.myecash.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import in.myecash.common.CommonUtils;
import in.myecash.common.CsvConverter;
import in.myecash.common.MyCustomer;
import in.myecash.common.MyGlobalSettings;
import in.myecash.events.persistence_service.TxnTableEventHelper;
import in.myecash.messaging.SmsConstants;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import in.myecash.common.database.*;
import in.myecash.common.constants.*;
import in.myecash.constants.*;
import in.myecash.database.*;

/**
 * Created by adgangwa on 15-05-2016.
 */
public class MerchantServices implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.MerchantServices");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     * Merchant operations
     */
    public Merchants changeMobile(String verifyparam, String newMobile, String otp) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "changeMobile";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = verifyparam+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                newMobile+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                otp;

        boolean validException = false;

        try {
            mLogger.debug("In changeMobile: " + verifyparam + "," + newMobile);

            // Fetch merchant with all child - as the same instance is to be returned too
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, true);
            String oldMobile = merchant.getMobile_num();

            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // Validate based on given current number
                if (!merchant.getDob().equals(verifyparam)) {
                    validException = true;
                    throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
                }

                // Generate OTP to verify new mobile number
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(merchant.getAuto_id());
                newOtp.setMobile_num(newMobile);
                newOtp.setOpcode(DbConstants.OP_CHANGE_MOBILE);
                BackendOps.generateOtp(newOtp,mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                validException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OTP_GENERATED), "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(merchant.getAuto_id(), DbConstants.OP_CHANGE_MOBILE, otp);
                mLogger.debug("OTP matched for given merchant operation: " + merchant.getAuto_id());

                // first add record in merchant ops table
                MerchantOps merchantops = new MerchantOps();
                merchantops.setCreateTime(new Date());
                merchantops.setMerchant_id(merchant.getAuto_id());
                merchantops.setOp_code(DbConstants.OP_CHANGE_MOBILE);
                merchantops.setMobile_num(oldMobile);
                merchantops.setInitiatedBy(DbConstantsBackend.USER_OP_INITBY_MCHNT);
                merchantops.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_APP);
                merchantops.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
                // set extra params in presentable format
                String extraParams = "Old Mobile: "+oldMobile+", New Mobile: "+newMobile;
                merchantops.setExtra_op_params(extraParams);
                merchantops = BackendOps.saveMerchantOp(merchantops);

                // Update with new mobile number
                try {
                    merchant.setMobile_num(newMobile);
                    merchant = BackendOps.updateMerchant(merchant);
                } catch(Exception e) {
                    mLogger.error("changeMobile: Exception while updating merchant status: "+merchant.getAuto_id());
                    // Rollback - delete merchant op added
                    try {
                        BackendOps.deleteMerchantOp(merchantops);
                    } catch(Exception ex) {
                        mLogger.fatal("changeMobile: Failed to rollback: merchant op deletion failed: "+merchant.getAuto_id());
                        // Rollback also failed
                        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                        throw ex;
                    }
                    throw e;
                }

                mLogger.debug("Processed mobile change for: " + merchant.getAuto_id());

                // Send SMS on old and new mobile - ignore sent status
                String smsText = SmsHelper.buildMobileChangeSMS(oldMobile, newMobile);
                SmsHelper.sendSMS(smsText, oldMobile + "," + newMobile, mEdr, mLogger);
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return merchant;

        } catch(Exception e) {
            BackendUtils.handleException(e,validException,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public Merchants updateSettings(String cbRate, boolean addClEnabled, String email,
                                    boolean askLinkedInvNum, boolean linkedInvNumOptional, boolean invNumOnlyNmbrs) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "updateSettings";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = cbRate+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                addClEnabled+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                email;

        try {
            //mLogger.debug("In updateSettings: "+cbRate+": "+addClEnabled+": "+email);
            mLogger.debug("Before context: "+InvocationContext.asString());
            mLogger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            // Fetch merchant with all child - as the same instance is to be returned too
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, true);

            // check merchant status
            BackendUtils.checkMerchantStatus(merchant, mEdr, mLogger);
            if(merchant.getAdmin_status()==DbConstants.USER_STATUS_READY_TO_REMOVE) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.ACC_UNDER_EXPIRY), "");
            }

            // update settings
            merchant.setCb_rate(cbRate);
            merchant.setCl_add_enable(addClEnabled);
            merchant.setEmail(email);
            merchant.setInvoiceNumAsk(askLinkedInvNum);
            merchant.setInvoiceNumOptional(linkedInvNumOptional);
            merchant.setInvoiceNumOnlyNumbers(invNumOnlyNmbrs);
            merchant = BackendOps.updateMerchant(merchant);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return merchant;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public Merchants deleteTrustedDevice(String deviceId) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "deleteTrustedDevice";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = deviceId;

        try {
            mLogger.debug("In changeMobile: " + deviceId);

            // Fetch merchant with all child - as the same instance is to be returned too
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, true);

            List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
            if(trustedDevices.size() <= 1) {
                // already restricted in app
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "You are not allowed to delete last device");
            }

            // find matching object
            MerchantDevice matched = null;
            for (MerchantDevice device : trustedDevices) {
                if (device.getDevice_id().equals(deviceId)) {
                    matched = device;
                }
            }
            // delete device
            if(matched!=null){
                BackendOps.deleteMchntDevice(matched);
            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.BL_ERROR_NO_DATA_FOUND), "No such trusted device: "+deviceId);
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return merchant;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    // Taking 'merchantId' and 'merchantCbTable' values from caller is a bit of security risk
    // as it will allow any logged-in merchant - to read (not update) cb details of other merchants too
    // but ignoring this for now - to keep this API as fast as possible - as this will be most called API
    public Cashback getCashback(String merchantId, String merchantCbTable, String customerId, boolean debugLogs) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        boolean validException = false;
        try {
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "getCashback";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    merchantCbTable+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                    customerId;

            //mLogger.setProperties(merchantId, DbConstants.USER_TYPE_MERCHANT, debugLogs);
            // Fetch merchant - send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediately after
            Object userObj = BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            Merchants merchant = null;
            boolean byCCUser = false;
            if(userType==DbConstants.USER_TYPE_MERCHANT) {
                merchant = (Merchants) userObj;
                // check to ensure that merchant is request CB for itself only
                if (!merchant.getAuto_id().equals(merchantId)) {
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA),"Invalid merchant id provided: " + merchantId);
                }
                merchantCbTable = merchant.getCashback_table();

            } else if(userType==DbConstants.USER_TYPE_CC) {
                // use provided merchant values
                byCCUser = true;
            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            int customerIdType = BackendUtils.getCustomerIdType(customerId);
            mLogger.debug("In getCashback: " + merchantId + ": " + customerId);
            mLogger.debug("Before context: "+InvocationContext.asString());
            mLogger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            // Fetch customer using mobile or cardId
            // Required as we need the private id of the customer to fetch the cashback
            Customers customer = null;
            try {
                customer = BackendOps.getCustomer(customerId, customerIdType, true);
            } catch(BackendlessException e) {
                if(e.getCode().equals(String.valueOf(ErrorCodes.NO_SUCH_USER)) &&
                        customerIdType!=BackendConstants.ID_TYPE_AUTO) {
                    // this will happen always in case of 'user registration' etc
                    validException = true;
                }
                throw e;
            }
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getMobile_num();

            // Fetch cashback record
            // Create where clause to fetch cashback
            String whereClause = "rowid = '" + customer.getPrivate_id()+merchantId + "'";
            ArrayList<Cashback> data = BackendOps.fetchCashback(whereClause, merchantCbTable, false, false);
            Cashback cashback = null;
            if(data == null) {
                if(byCCUser) {
                    // if called by CC, return from here - to skip cb creation logic
                    validException = true;
                    throw new BackendlessException(String.valueOf(ErrorCodes.CUST_NOT_REG_WITH_MCNT), "");
                }
                cashback = handleCashbackCreate(merchant, customer);
            } else {
                cashback = data.get(0);
            }

            // Cashback details to be returned - even if customer account/card is disabled/locked
            // so not checking for customer account/card status

            // Add 'customer details' in the cashback object to be returned
            // these details are not stored in DB along with cashback object
            //cashback.setOther_details(buildCustomerDetails(customer, byCCUser, CommonConstants.CSV_SUB_DELIMETER));
            cashback.setOther_details(MyCustomer.toCsvString(customer, byCCUser));
            stripCashback(cashback);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return cashback;

        } catch(Exception e) {
            BackendUtils.handleException(e,validException,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public MerchantStats getMerchantStats(String mchntId) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getMerchantStats";

        try {
            //mLogger.debug("In getMerchantStats");

            // Fetch merchant - send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediatly after
            Object userObj = BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            boolean callByCC = false;
            Merchants merchant = null;
            if(userType==DbConstants.USER_TYPE_MERCHANT) {
                merchant = (Merchants) userObj;
            } else if(userType==DbConstants.USER_TYPE_CC) {
                // fetch merchant
                merchant = BackendOps.getMerchant(mchntId, false, false);
                callByCC = true;
            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            String merchantId = merchant.getAuto_id();

            // not checking for merchant account status

            boolean calculateAgain = true;
            // TODO: see if it is really required to fetch customer.card too - can be avoided if card status is not required
            // fetch merchant stat object, if exists
            MerchantStats stats = BackendOps.fetchMerchantStats(merchantId);
            // create object if not already available
            if (stats == null) {
                mLogger.debug("Creating new stats object");
                stats = new MerchantStats();
                stats.setMerchant_id(merchantId);
            } else {
                // return old object, if last updated within configured hours
                Date updateTime = stats.getUpdated();
                if(updateTime==null) {
                    updateTime = stats.getCreated();
                }
                long updated = updateTime.getTime();
                long now = (new Date()).getTime();
                if( (now - updated) < (MyGlobalSettings.getMchntDashBNoRefreshHrs()*60*60*1000) ) {
                    // return old object - dont calculate again
                    calculateAgain = false;
                }
            }

            if(calculateAgain) {
                //reset all stats to 0
                stats.setBill_amt_no_cb(0);
                stats.setBill_amt_total(0);
                stats.setCash_credit(0);
                stats.setCb_credit(0);
                stats.setCash_debit(0);
                stats.setCb_debit(0);
                stats.setCust_cnt_cash(0);
                stats.setCust_cnt_cb(0);
                stats.setCust_cnt_cb_and_cash(0);
                stats.setCust_cnt_no_balance(0);

                // fetch all CB records for this merchant
                ArrayList<Cashback> data = BackendOps.fetchCashback("merchant_id = '" + merchantId + "'",
                        merchant.getCashback_table(), true, false);
                if (data != null) {
                    // loop on all cashback objects and calculate stats
                    mLogger.debug("Fetched cashback records: " + merchantId + ", " + data.size());

                    StringBuilder sb = new StringBuilder(CsvConverter.CB_CSV_MAX_SIZE * (data.size()+1));
                    //sb.append("Customer Id,Account Balance,Cashback Balance,Total Account Debit,Total Account Credit,Total Cashback Debit,Total Cashback Credit,Total Billed,Total Cashback Billed");
                    //sb.append(CommonConstants.CSV_NEWLINE);

                    // Add first line as header - to give the file creation time in epoch
                    sb.append(String.valueOf((new Date()).getTime())).append(CommonConstants.CSV_DELIMETER);

                    for (int k = 0; k < data.size(); k++) {
                        Cashback cb = data.get(k);
                        // update customer counts
                        // no need to check for 'debit' amount - as 'credit' amount is total amount and includes debit amount too
                        if (cb.getCb_credit() > 0 && cb.getCl_credit() > 0) {
                            stats.cust_cnt_cb_and_cash++;
                        } else if (cb.getCb_credit() > 0) {
                            stats.cust_cnt_cb++;
                        } else if (cb.getCl_credit() > 0) {
                            stats.cust_cnt_cash++;
                        } else {
                            stats.cust_cnt_no_balance++;
                        }

                        // update amounts
                        stats.cb_credit = stats.cb_credit + cb.getCb_credit();
                        stats.cb_debit = stats.cb_debit + cb.getCb_debit();
                        stats.cash_credit = stats.cash_credit + cb.getCl_credit();
                        stats.cash_debit = stats.cash_debit + cb.getCl_debit();
                        stats.bill_amt_total = stats.bill_amt_total + cb.getTotal_billed();
                        stats.bill_amt_no_cb = stats.bill_amt_no_cb + (cb.getTotal_billed() - cb.getCb_billed());

                        // write record as csv string
                        if(cb.getCustomer()!=null) {
                            cb.setOther_details(MyCustomer.toCsvString(cb.getCustomer(), callByCC));
                            //sb.append(buildCashbackDetails(cb, callByCC)).append(CommonConstants.CSV_NEWLINE);
                            sb.append(CommonConstants.CSV_NEWLINE).append(CsvConverter.csvStrFromCb(cb));
                        } else {
                            // All cb shud have linked customer
                            // ignore error - but log the same
                            mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.IGNORED_ERROR_CB_WITH_NO_CUST;
                        }

                    }

                    // upload data as CSV file
                    createCsvFile(sb.toString(), merchantId);
                }

                // save stats object - don't bother about return status
                // This is just for our own reporting purpose,
                // as for merchant stats anyways are calculated fresh each time from cashback objects
                try {
                    stats = BackendOps.saveMerchantStats(stats);
                } catch (Exception e) {
                    // ignore the exception
                    mLogger.error("Exception while saving merchantStats object: " + e.toString());
                }
            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_OLD_STATS_RETURNED;
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return stats;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public List<MerchantOps> getMerchantOps(String merchantId) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        boolean positiveException = false;

        try {
            mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "getMerchantOps";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantId;

            //mLogger.setProperties(merchantId, DbConstants.USER_TYPE_MERCHANT, debugLogs);
            // Fetch merchant - send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediately after
            Object userObj = BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            Merchants merchant = null;
            boolean byCCUser = false;
            if(userType==DbConstants.USER_TYPE_MERCHANT) {
                merchant = (Merchants) userObj;
                // check to ensure that merchant is request CB for itself only
                if (!merchant.getAuto_id().equals(merchantId)) {
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA),"Invalid merchant id provided: " + merchantId);
                }
            } else if(userType==DbConstants.USER_TYPE_CC) {
                // use provided merchant values
                byCCUser = true;
            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // not checking for merchant account status

            // fetch merchant ops
            String whereClause = "merchant_id = '"+merchantId+"'";
            if(!byCCUser) {
                // return only 'completed' ops to merchant
                whereClause = whereClause+" AND op_status = '"+DbConstantsBackend.USER_OP_STATUS_COMPLETE +"'";
            }
            mLogger.debug("where clause: "+whereClause);

            List<MerchantOps> ops = BackendOps.fetchMerchantOps(whereClause);
            if(ops==null) {
                // not exactly a positive exception - but using it to avoid logging of this as error
                // as it can happen frequently as valid scenario
                positiveException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.BL_ERROR_NO_DATA_FOUND), "");
            }

            if(!byCCUser) {
                // remove sensitive fields - from in-memory objects
                for (MerchantOps op: ops) {
                    op.setTicketNum("");
                    op.setReason("");
                    //op.setOtp("");
                    op.setOp_status("");
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return ops;

        } catch(Exception e) {
            BackendUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void archiveTxns() {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "archiveTxns";

        try {
            //mLogger.debug("In archiveTxns");

            // Fetch merchant
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);
            // not checking for merchant account status

            // archive txns
            TxnArchiver archiver = new TxnArchiver(mLogger, merchant, InvocationContext.getUserToken());
            archiver.archiveMerchantTxns(mEdr);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public Transaction cancelTxn(String txnId, String cardId, String pin) {

        TxnTableEventHelper txnEventHelper = new TxnTableEventHelper();
        return txnEventHelper.cancelTxn(InvocationContext.getUserId(), txnId, cardId, pin);
    }

    /*
     * Public methods: Backend REST APIs
     * Customer operations by merchant
     */
    public Cashback registerCustomer(String customerMobile, String cardId, String otp) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "registerCustomer";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = customerMobile+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                cardId;

        Customers customer = null;
        CustomerCards card = null;
        BackendlessUser customerUser = null;
        Cashback cashback = null;
        boolean positiveException = false;

        try {
            // Fetch merchant
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customerMobile;

            // fetch customer card object
            card = BackendOps.getCustomerCard(cardId);
            mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = card.getCard_id();
            BackendUtils.checkCardForAllocation(card);
            // TODO: enable in production
            /*
            if(!card.getMerchant_id().equals(merchantId)) {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_CARD_WRONG_MERCHANT, "");
            }*/

            if (otp == null || otp.isEmpty()) {
                // Generate OTP and send SMS
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(customerMobile);
                newOtp.setMobile_num(customerMobile);
                newOtp.setOpcode(DbConstants.OP_REG_CUSTOMER);
                BackendOps.generateOtp(newOtp,mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OTP_GENERATED), "");

            } else {
                // Second run, as OTP available
                // Verify OTP
                BackendOps.validateOtp(customerMobile, DbConstants.OP_REG_CUSTOMER, otp);

                // Create customer object
                customer = createCustomer();
                // set fields
                // new record - so set it directly
                customer.setCashback_table(merchant.getCashback_table());
                customer.setTxn_tables(merchant.getTxn_table());
                customer.setMobile_num(customerMobile);
                //customer.setName(name);
                customer.setCardId(cardId);
                // set membership card
                card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
                card.setStatus_update_time(new Date());
                customer.setMembership_card(card);

                // Create customer user
                customerUser = new BackendlessUser();
                customerUser.setProperty("user_id", customerMobile);
                // use generated PIN as password
                customerUser.setPassword(customer.getTxn_pin());
                customerUser.setProperty("user_type", DbConstants.USER_TYPE_CUSTOMER);
                // Both 'user' and 'customer' objects get created in single go
                // This also ensures that 'customer' object's 'ownerId' remains null
                // This helps to avoid direct update from app by the merchant who created this customer object
                customerUser.setProperty("customer", customer);

                customerUser = BackendOps.registerUser(customerUser);
                try {
                    customer = (Customers) customerUser.getProperty("customer");
                    // assign custom role to it
                    BackendOps.assignRole(customerMobile, BackendConstants.ROLE_CUSTOMER);

                    // create cashback also - to avoid another call to 'getCashback' from merchant
                    cashback = createCbObject(merchant, customer);
                    // Add 'customer details' in the cashback object to be returned
                    // these details are not stored in DB along with cashback object
                    //cashback.setOther_details(buildCustomerDetails(customer, false, CommonConstants.CSV_SUB_DELIMETER));
                    cashback.setOther_details(MyCustomer.toCsvString(customer, false));
                    // remove 'not needed sensitive' fields from cashback object
                    stripCashback(cashback);

                } catch(Exception e) {
                    // rollback to not-usable state
                    rollbackRegister(customerMobile, card);
                    throw e;
                }

                // Send welcome sms to the customer
                String smsText = String.format(SmsConstants.SMS_CUSTOMER_REGISTER, customerMobile);
                SmsHelper.sendSMS(smsText, customerMobile, mEdr, mLogger);

                // Send SMS containing PIN
                smsText = String.format(SmsConstants.SMS_PIN, customerMobile, customer.getTxn_pin());
                if (!SmsHelper.sendSMS(smsText, customerMobile, mEdr, mLogger)) {
                    // TODO: write to alarm table for retry later
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return cashback;

        } catch(Exception e) {
            BackendUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
    public void execCustomerOp(String opCode, String customerId, String scannedCardId, String otp, String pin, String opParam) {

        CommonUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "execCustomerOp";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = opCode+BackendConstants.BACKEND_EDR_SUB_DELIMETER +
                customerId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                scannedCardId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                otp+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                pin+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                opParam;

        boolean positiveException = false;

        try {
            // Fetch merchant
            Merchants merchant = (Merchants) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);

            // Fetch customer user
            BackendlessUser custUser = BackendOps.fetchUser(customerId, DbConstants.USER_TYPE_CUSTOMER, false);
            Customers customer = (Customers) custUser.getProperty("customer");
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getMobile_num();
            String cardId = customer.getMembership_card().getCard_id();
            mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = cardId;
            // check if customer is enabled
            CommonUtils.checkCustomerStatus(customer, mLogger);

            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // Don't verify QR card# for 'new card' operation
                if (!opCode.equals(DbConstants.OP_NEW_CARD) &&
                        !cardId.equals(scannedCardId)) {
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_CARD), "Wrong membership card");
                }

                // Don't verify PIN for 'reset PIN' operation
                if (!opCode.equals(DbConstants.OP_RESET_PIN) &&
                        !customer.getTxn_pin().equals(pin)) {

                    CommonUtils.handleWrongAttempt(customerId, customer, DbConstants.USER_TYPE_CUSTOMER,
                            DbConstantsBackend.WRONG_PARAM_TYPE_PIN, opCode, mLogger);
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_PIN), "Wrong PIN attempt: " + customer.getMobile_num());
                }

                // Generate OTP and send SMS
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(customerId);
                if (opCode.equals(DbConstants.OP_CHANGE_MOBILE)) {
                    newOtp.setMobile_num(opParam);
                } else {
                    newOtp.setMobile_num(customer.getMobile_num());
                }
                newOtp.setOpcode(opCode);
                BackendOps.generateOtp(newOtp,mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OTP_GENERATED), "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(customerId, opCode, otp);

                switch (opCode) {
                    case DbConstants.OP_NEW_CARD:
                        changeCustomerCard(customer, scannedCardId, opParam);
                        break;
                    case DbConstants.OP_CHANGE_MOBILE:
                        changeCustomerMobile(custUser, opParam);
                        break;
                    case DbConstants.OP_RESET_PIN:
                        resetCustomerPin(custUser);
                        break;
                }

                // add to customer ops table - for records purpose
                try {
                    CustomerOps op = new CustomerOps();
                    op.setOp_code(opCode);
                    op.setRequestor_id(merchant.getAuto_id());
                    op.setQr_card(scannedCardId);
                    op.setExtra_op_params(opParam);
                    BackendOps.saveCustomerOp(op);
                } catch(Exception e) {
                    // ignore error
                    mLogger.error("Customer change card op record save failed: "+e.toString());
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }*/

    /*
     * Private helper methods
     */
    private Customers createCustomer() {
        Customers customer = new Customers();
        customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
        customer.setStatus_reason(DbConstantsBackend.ENABLED_ACTIVE);
        customer.setStatus_update_time(new Date());
        customer.setLastRenewDate(new Date());

        // get customer counter value and encode the same to get customer private id
        Long customerCnt =  BackendOps.fetchCounterValue(DbConstantsBackend.CUSTOMER_ID_COUNTER);
        String private_id = Base35.fromBase10(customerCnt, CommonConstants.CUSTOMER_INTERNAL_ID_LEN);
        mLogger.debug("Generated private id: "+private_id);
        customer.setPrivate_id(private_id);

        // generate and set PIN
        String pin = BackendUtils.generateCustomerPIN();
        mLogger.debug("Generated PIN: "+pin);
        customer.setTxn_pin(pin);

        return customer;
    }

    private Cashback handleCashbackCreate(Merchants merchant, Customers customer) {
        // Add cashback table name (of this merchant's) in customer record, if not already added (by some other merchant)
        //boolean cbTableUpdated = false;
        String currCbTables = customer.getCashback_table();
        if(currCbTables==null || currCbTables.isEmpty()) {
            mLogger.debug("Setting new CB tables for customer: "+merchant.getCashback_table()+","+currCbTables);
            customer.setCashback_table(merchant.getCashback_table());
            //cbTableUpdated = true;

        } else if(!currCbTables.contains(merchant.getCashback_table())) {
            String newCbTables = currCbTables+CommonConstants.CSV_DELIMETER+merchant.getCashback_table();
            mLogger.debug("Setting new CB tables for customer: "+newCbTables+","+currCbTables);
            customer.setCashback_table(newCbTables);
            //cbTableUpdated = true;
        }

        // not updating customer - as the same will be automatically done
        // along with cashback save in 'createCbObject' method
        /*
        if(cbTableUpdated) {
            BackendOps.updateCustomer(customer);
        }*/

        // create new cashback object
        // intentionally doing it after updating customer for cashback table name
        return createCbObject(merchant, customer);
    }

    private Cashback createCbObject(Merchants merchant, Customers customer) {
        Cashback cashback = new Cashback();

        // rowid_card - "customer card id"+"merchant id"+"terminal id"
        //String cardId = customer.getMembership_card().getCard_id();
        //cashback.setRowid_card(cardId + merchantId);
        // rowid - "customer mobile no"+"merchant id"+"terminal id"
        cashback.setRowid(customer.getPrivate_id() + merchant.getAuto_id());

        cashback.setMerchant_id(merchant.getAuto_id());
        cashback.setCust_private_id(customer.getPrivate_id());

        cashback.setCb_credit(0);
        cashback.setCb_debit(0);
        cashback.setCl_credit(0);
        cashback.setCl_debit(0);
        cashback.setTotal_billed(0);
        cashback.setCb_billed(0);

        cashback.setMerchant(merchant);
        cashback.setCustomer(customer);

        return BackendOps.saveCashback(cashback, merchant.getCashback_table());
    }

    // Strip cashback object for information not needed by merchant app
    private void stripCashback(Cashback cashback) {
        cashback.setCust_private_id(null);
        cashback.setCustomer(null);
        cashback.setMerchant(null);
        cashback.setRowid(null);
        //cashback.setRowid_card(null);
    }

    private void rollbackRegister(String custId, CustomerCards card) {
        mLogger.debug("In rollbackRegister");
        // add flag for manual check
        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;

        // rollback to not-usable state
        try {
            BackendOps.decrementCounterValue(DbConstantsBackend.CUSTOMER_ID_COUNTER);

            Customers customer = BackendOps.getCustomer(custId, BackendConstants.ID_TYPE_MOBILE, false);
            customer.setMembership_card(null);
            BackendUtils.setCustomerStatus(customer, DbConstants.USER_STATUS_REG_ERROR, DbConstantsBackend.REG_ERROR_REG_FAILED,
                    mEdr, mLogger);
            /*customer.setAdmin_status(DbConstants.USER_STATUS_REG_ERROR);
            customer.setStatus_reason(DbConstantsBackend.REG_ERROR_REG_FAILED);
            BackendOps.updateCustomer(customer);*/

            // free up the card for next allocation
            card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_NEW);
            // TODO: set any other fields required
            BackendOps.saveCustomerCard(card);

        } catch(Exception ex) {
            mLogger.fatal("registerCustomer: Customer register Rollback failed: "+ex.toString());
            mLogger.error(BackendUtils.stackTraceStr(ex));
            throw ex;
        }
    }

    /*
    private void changeCustomerCard(Customers customer, String newCardId, String reason) {
        // fetch new card record
        CustomerCards newCard = BackendOps.getCustomerCard(newCardId);
        CommonUtils.checkCardForAllocation(newCard);
        //TODO: enable this in final testing
        //if(!newCard.getMerchantId().equals(merchantId)) {
        //  return ResponseCodes.RESPONSE_CODE_QR_WRONG_MERCHANT;
        //}

        CustomerCards oldCard = customer.getMembership_card();
        mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = oldCard.getCard_id();

        // update 'customer' and 'CustomerCard' objects for new card
        newCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
        newCard.setStatus_update_time(new Date());
        customer.setCardId(newCard.getCard_id());
        customer.setMembership_card(newCard);

        // save updated customer object
        BackendOps.updateCustomer(customer);

        // update old card status
        try {
            oldCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_REMOVED);
            oldCard.setStatus_reason(reason);
            oldCard.setStatus_update_time(new Date());
            BackendOps.saveCustomerCard(oldCard);

        } catch (BackendlessException e) {
            // ignore as not considered as failure for whole 'changeCustomerCard' operation
            // but log as alarm for manual correction
            // TODO: raise alarm
            mLogger.error("Exception while updating old card status: "+e.toString());
            mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX]=BackendConstants.IGNORED_ERROR_OLDCARD_SAVE_FAILED;
        }

        // Send message to customer informing the same - ignore sent status
        String smsText = SmsHelper.buildNewCardSMS(customer.getMobile_num(), newCardId);
        if(SmsHelper.sendSMS(smsText, customer.getMobile_num(), mLogger)) {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
        }
    }

    private void changeCustomerMobile(BackendlessUser custUser, String newMobile) {
        Customers customer = (Customers) custUser.getProperty("customer");
        String oldMobile = customer.getMobile_num();

        // update mobile number
        custUser.setProperty("user_id", newMobile);
        customer.setMobile_num(newMobile);
        custUser.setProperty("customer", customer);
        BackendOps.updateUser(custUser);

        // Send message to customer informing the same - ignore sent status
        String smsText = SmsHelper.buildMobileChangeSMS( oldMobile, newMobile );
        if(SmsHelper.sendSMS(smsText, oldMobile+","+newMobile, mLogger)){
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
        };
    }

    private void resetCustomerPin(BackendlessUser custUser) {
        Customers customer = (Customers) custUser.getProperty("customer");
        // generate pin
        String newPin = CommonUtils.generateCustomerPIN();

        // update user account for the PIN
        custUser.setPassword(newPin);
        //TODO: encode PIN
        customer.setTxn_pin(newPin);
        custUser.setProperty("customer",customer);

        BackendOps.updateUser(custUser);

        // Send SMS through HTTP
        String smsText = SmsHelper.buildCustPinResetSMS(customer.getMobile_num(), newPin);
        if( !SmsHelper.sendSMS(smsText, customer.getMobile_num(), mLogger)) {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
        }
    }*/

    private void createCsvFile(String data, String merchantId) {
        try {
            String fileUrl = Backendless.Files.saveFile(CommonUtils.getMerchantCustFilePath(merchantId),
                    data.getBytes("UTF-8"), true);
            mLogger.debug("Customer data CSV file uploaded: " + fileUrl);
        } catch (UnsupportedEncodingException e) {
            mLogger.error("Customer data CSV file upload failed: "+ e.toString());
            // For multiple days, single failure will be considered failure for all days
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Failed to create customer data CSV file: "+e.toString());
        }
    }
}

    /*
    public void changePassword(String userId, String oldPasswd, String newPasswd, String mobileNum) {
        initCommon();
        mLogger.debug("In changePassword: "+userId+": "+mobileNum);

        mLogger.debug("Before: "+InvocationContext.asString());
        mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

        // we are anyways gonna login next, so dont need this
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // Login to verify the old password
        // Note: afterLogin event handler will not get called - so 'trusted device' check will not happen
        // As event handlers are not called - for API calls made from server code.
        // In normal situation, this is not an issue - as user can call 'change password' only after login
        // However, in hacked situation, 'trusted device' check wont happen - ignoring this for now.
        BackendlessUser user = mBackendOps.loginUser(userId,oldPasswd);
        if(user==null) {
            //return mBackendOps.mLastOpStatus;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        mLogger.debug("After: "+InvocationContext.asString());
        mLogger.debug("After: "+HeadersManager.getInstance().getHeaders().toString());

        // check admin status
        String status = CommonUtils.checkMerchantStatus(merchant);
        if(status != null) {
            mBackendOps.logoutUser();
            CommonUtils.throwException(mLogger,status, "Merchant account is inactive", false);
        }

        // Change password
        user.setPassword(newPasswd);
        user = mBackendOps.updateUser(user);
        if(user==null) {
            //return mBackendOps.mLastOpStatus;
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }

        // Send SMS through HTTP
        String smsText = buildPwdChangeSMS(userId);
        if( !SmsHelper.sendSMS(smsText, mobileNum) )
        {
            //TODO: add in alarms table
            CommonUtils.throwException(mLogger,BackendResponseCodes.SEND_SMS_FAILED, "Failed to send password reset SMS: "+userId, false);
        }

        //mLogger.flush();
        //return BackendResponseCodes.NO_ERROR;
    }
    private String buildPwdChangeSMS(String userId) {
        return String.format(SmsConstants.SMS_PASSWD_CHANGED, CommonUtils.getHalfVisibleId(userId));
    }

    */


    /*
    public void changeCustomerCard(String customerId, String newCardId, String reason, String otp, String pin) {
        initCommon();

        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "changeCustomerCard";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = customerId + BackendConstants.BACKEND_EDR_SUB_DELIMETER +
                newCardId + BackendConstants.BACKEND_EDR_SUB_DELIMETER +
                reason;

        boolean positiveException = false;

        try {
            // Fetch merchant
            Merchants merchant = (Merchants) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    true, DbConstants.USER_TYPE_MERCHANT, mEdr);

            // Fetch customer
            Customers customer = BackendOps.getCustomer(customerId, BackendConstants.ID_TYPE_MOBILE);
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getMobile_num();
            String cardId = customer.getMembership_card().getCard_id();
            mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = cardId;
            // check if customer is enabled
            CommonUtils.checkCustomerStatus(customer);

            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // Verify PIN
                if (!customer.getTxn_pin().equals(pin)) {
                    CommonUtils.handleWrongAttempt(customerId, customer, DbConstants.USER_TYPE_CUSTOMER, DbConstantsBackend.WRONG_PARAM_TYPE_PIN);
                    throw new BackendlessException(BackendResponseCodes.WRONG_PIN, "Wrong PIN attempt: " + customer.getMobile_num());
                }

                // Generate OTP and send SMS
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(customer.getMobile_num());
                newOtp.setMobile_num(customer.getMobile_num());
                newOtp.setOpcode(DbConstants.OP_NEW_CARD);
                BackendOps.generateOtp(newOtp);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.OTP_GENERATED, "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(customerId, otp);
                // fetch new card record
                CustomerCards newCard = BackendOps.getCustomerCard(newCardId);
                CommonUtils.checkCardForAllocation(newCard);
                //TODO: enable this in final testing
                //if(!newCard.getMerchantId().equals(merchantId)) {
                //  return ResponseCodes.RESPONSE_CODE_QR_WRONG_MERCHANT;
                //}

                CustomerCards oldCard = customer.getMembership_card();
                mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = oldCard.getCard_id();

                // update 'customer' and 'CustomerCard' objects for new card
                newCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
                newCard.setStatus_update_time(new Date());
                customer.setCardId(newCard.getCard_id());
                customer.setMembership_card(newCard);

                // save updated customer object
                BackendOps.updateCustomer(customer);

                // update old card status
                try {
                    oldCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_REMOVED);
                    oldCard.setStatus_reason(reason);
                    oldCard.setStatus_update_time(new Date());
                    BackendOps.saveCustomerCard(oldCard);

                } catch (BackendlessException e) {
                    // ignore as not considered as failure for whole 'changeCustomerCard' operation
                    // but log as alarm for manual correction
                    // TODO: raise alarm
                    mLogger.error("Exception while updating old card status: "+e.toString());
                    mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX]=BackendConstants.IGNORED_ERROR_OLDCARD_SAVE_FAILED;
                }

                // add to customer ops table - for records purpose
                try {
                    CustomerOps op = new CustomerOps();
                    op.setRequestor_id(merchant.getAuto_id());
                    op.setOp_status(DbConstants.OP_NEW_CARD);
                    op.setQr_card(oldCard.getCard_id());
                    op.setExtra_op_params(reason);
                    BackendOps.saveCustomerOp(op);
                } catch(Exception e) {
                    // ignore error
                    mLogger.error("Customer change card op record save failed: "+e.toString());
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch (Exception e) {
            handleException(e,positiveException);
            throw e;
        } finally {
            finalHandling(startTime);
        }
    }*/

    /*
    private String buildCashbackDetails(Cashback cb, boolean addCustCareData) {

        // Build cashback data as CSV record
        // <Account Balance>,<Total Account Credit>,<Total Account Debit>,
        // <Cashback Balance>,<Total Cashback Credit>,<Total Cashback Debit>,
        // <Total Billed>,<Total Cashback Billed>,
        // <create time>,<update time>
        String[] csvFields = new String[CommonConstants.CB_CSV_TOTAL_FIELDS];
        csvFields[CommonConstants.CB_CSV_CUST_PVT_ID] = String.valueOf(cb.getCust_private_id()) ;
        csvFields[CommonConstants.CB_CSV_MCHNT_ID] = String.valueOf(cb.getMerchant_id()) ;
        //csvFields[CommonConstants.CB_CSV_ACC_BAL] = String.valueOf(cb.getCl_credit() - cb.getCl_debit()) ;
        csvFields[CommonConstants.CB_CSV_ACC_CR] = String.valueOf(cb.getCl_credit()) ;
        csvFields[CommonConstants.CB_CSV_ACC_DB] = String.valueOf(cb.getCl_debit()) ;
        //csvFields[CommonConstants.CB_CSV_BAL] = String.valueOf(cb.getCb_credit() - cb.getCb_debit()) ;
        csvFields[CommonConstants.CB_CSV_CR] = String.valueOf(cb.getCb_credit()) ;
        csvFields[CommonConstants.CB_CSV_DB] = String.valueOf(cb.getCb_debit()) ;
        csvFields[CommonConstants.CB_CSV_TOTAL_BILL] = String.valueOf(cb.getTotal_billed()) ;
        csvFields[CommonConstants.CB_CSV_BILL] = String.valueOf(cb.getCb_billed()) ;
        csvFields[CommonConstants.CB_CSV_CREATE_TIME] = String.valueOf(cb.getCreated().getTime()) ;
        csvFields[CommonConstants.CB_CSV_UPDATE_TIME] = String.valueOf(cb.getUpdated().getTime()) ;
        if(cb.getCustomer()!=null) {
            //csvFields[CommonConstants.CB_CSV_OTHER_DETAILS] = buildCustomerDetails(cb.getCustomer(), addCustCareData, CommonConstants.CSV_SUB_DELIMETER);
            csvFields[CommonConstants.CB_CSV_OTHER_DETAILS] = MyCustomer.toCsvString(cb.getCustomer(), addCustCareData);
        }

        // combine to single string
        StringJoiner sj = new StringJoiner(CommonConstants.CSV_DELIMETER);
        for(String s:csvFields) sj.add(s);

        mLogger.debug("Generated cashback details: "+sj.toString());
        return sj.toString();
    }

    private String buildCustomerDetails(Customers customer, boolean addCustCareData, String delim) {

        // Build customer detail in below CSV format
        // size = 10+10+50+5+10+10+1+1+10+50+11+1+10 = ~180 (round off to 256)
        // <private id>,<mobile_num>,<<name>>,<<first login ok>>,<<cust_create_time>>,
        // <acc_status>,<acc_status_reason>,<acc_status_update_time>,<<admin remarks>>
        // <card_id>,<card_status>,<card_status_update_time>
        // records with double bracket '<<>>' are only sent to 'customer care' users

        CustomerCards card = customer.getMembership_card();
        String[] csvFields = new String[CommonConstants.CUST_CSV_TOTAL_FIELDS];
        csvFields[CommonConstants.CUST_CSV_PRIVATE_ID] = customer.getPrivate_id() ;
        csvFields[CommonConstants.CUST_CSV_MOBILE_NUM] = customer.getMobile_num() ;
        csvFields[CommonConstants.CUST_CSV_ACC_STATUS] = String.valueOf(customer.getAdmin_status()) ;
        csvFields[CommonConstants.CUST_CSV_STATUS_REASON] = String.valueOf(customer.getStatus_reason()) ;
        csvFields[CommonConstants.CUST_CSV_STATUS_UPDATE_TIME] = String.valueOf(customer.getStatus_update_time().getTime()) ;
        csvFields[CommonConstants.CUST_CSV_CARD_ID] = card.getCard_id() ;
        csvFields[CommonConstants.CUST_CSV_CARD_STATUS] = String.valueOf(card.getStatus()) ;
        if(addCustCareData) {
            csvFields[CommonConstants.CUST_CSV_NAME] = customer.getName() ;
            csvFields[CommonConstants.CUST_CSV_FIRST_LOGIN_OK] = String.valueOf(customer.getFirst_login_ok()) ;
            csvFields[CommonConstants.CUST_CSV_CUST_CREATE_TIME] = String.valueOf(customer.getCreated().getTime()) ;
            //csvFields[CommonConstants.CUST_CSV_ADMIN_REMARKS] = customer.getAdmin_remarks() ;
            csvFields[CommonConstants.CUST_CSV_CARD_STATUS_UPDATE_TIME] = String.valueOf(card.getStatus_update_time().getTime()) ;
        } else {
            csvFields[CommonConstants.CUST_CSV_NAME] = "";
            csvFields[CommonConstants.CUST_CSV_FIRST_LOGIN_OK] = "";
            csvFields[CommonConstants.CUST_CSV_CUST_CREATE_TIME] = "";
            //csvFields[CommonConstants.CUST_CSV_ADMIN_REMARKS] = "";
            csvFields[CommonConstants.CUST_CSV_CARD_STATUS_UPDATE_TIME] = "";
        }

        // combine to single string
        StringJoiner sj = new StringJoiner(delim);
        for(String s:csvFields) sj.add(s);

        mLogger.debug("Generated customer details: "+sj.toString());
        return sj.toString();
    }*/
