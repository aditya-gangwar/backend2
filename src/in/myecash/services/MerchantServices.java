package in.myecash.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.FilePermission;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import in.myecash.common.*;
import in.myecash.events.persistence_service.TxnProcessHelper;
import in.myecash.messaging.SmsConstants;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
    public Transaction cancelTxn(String txnId, String cardId, String pin, boolean isOtp) {
        TxnProcessHelper txnEventHelper = new TxnProcessHelper();
        return txnEventHelper.cancelTxn(InvocationContext.getUserId(), txnId, cardId, pin, isOtp);
    }

    public Transaction commitTxn(String csvTxnData, String pin, boolean isOtp) throws Exception {
        TxnProcessHelper txnEventHelper = new TxnProcessHelper();
        return txnEventHelper.handleTxnCommit(InvocationContext.getUserId(), csvTxnData, pin, isOtp, true, true);
    }

    public void generateTxnOtp(String custMobileOrId) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "generateTxnOtp";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = custMobileOrId;

        boolean validException = false;
        try {
            mLogger.debug("In generateTxnOtp: " + custMobileOrId);

            // Fetch merchant with all child - as the same instance is to be returned too
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);

            String custMobile = null;
            int idType = BackendUtils.getCustomerIdType(custMobileOrId);
            if(idType!=CommonConstants.ID_TYPE_MOBILE) {
                // fetch customer with given id
                Customers customer = BackendOps.getCustomer(custMobileOrId, idType, false);
                custMobile = customer.getMobile_num();
                // check if mCustomer is enabled
                BackendUtils.checkCustomerStatus(customer, mEdr, mLogger);
            } else {
                custMobile = custMobileOrId;
            }
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = custMobileOrId;

            // Generate OTP to verify new mobile number
            AllOtp newOtp = new AllOtp();
            newOtp.setUser_id(merchant.getAuto_id());
            newOtp.setMobile_num(custMobile);
            newOtp.setOpcode(DbConstants.OP_TXN_COMMIT);
            BackendOps.generateOtp(newOtp,merchant.getName(),mEdr,mLogger);

            // OTP generated successfully - return exception to indicate so
            validException = true;
            throw new BackendlessException(String.valueOf(ErrorCodes.OTP_GENERATED), "");

        } catch(Exception e) {
            BackendUtils.handleException(e,validException,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }

    }

    /*public Transaction writeTxn(String csvTxnData, String tableName) throws Exception {
        Transaction txn = CsvConverter.txnFromCsvStr(csvTxnData);
        Backendless.Data.mapTableToClass(tableName, Transaction.class);
        return Backendless.Persistence.save(txn);
    }*/

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
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, true);
            String oldMobile = merchant.getMobile_num();

            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // New mobile number should not be registered with other merchant
                Merchants newMchnt = null;
                try {
                    newMchnt = BackendOps.getMerchant(newMobile, false, false);
                } catch (BackendlessException be) {
                    // No such Merchant exist - we can proceed
                }
                if(newMchnt!=null) {
                    // If here - means customer exist - return error
                    mLogger.debug("Merchant already registered: "+newMobile+","+merchant.getMobile_num());
                    throw new BackendlessException(String.valueOf(ErrorCodes.MOBILE_ALREADY_REGISTERED), "");
                }

                // Validate based on given current number
                if (!merchant.getDob().equals(verifyparam)) {
                    int cnt = BackendUtils.handleWrongAttempt(merchant.getAuto_id(), merchant, DbConstants.USER_TYPE_MERCHANT,
                            DbConstantsBackend.WRONG_PARAM_TYPE_DOB, DbConstants.OP_CHANGE_PASSWD, mEdr, mLogger);
                    validException = true;
                    throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED_DOB), String.valueOf(cnt));
                }

                // Generate OTP to verify new mobile number
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(merchant.getAuto_id());
                newOtp.setMobile_num(newMobile);
                newOtp.setOpcode(DbConstants.OP_CHANGE_MOBILE);
                BackendOps.generateOtp(newOtp,"",mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                validException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OTP_GENERATED), "");

            } else {
                // Second run, as OTP available
                if(!BackendOps.validateOtp(merchant.getAuto_id(), DbConstants.OP_CHANGE_MOBILE, otp, mEdr, mLogger)) {
                    validException = true;
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_OTP), "");
                }
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
                String smsText = SmsHelper.buildMobileChangeSMS(merchant.getAuto_id(), newMobile);
                SmsHelper.sendSMS(smsText, oldMobile + "," + newMobile, mEdr, mLogger, true);
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

    public Merchants updateSettings(String cbRate, boolean addClEnabled, String email, String contactPhone,
                                    boolean askLinkedInvNum, boolean linkedInvNumOptional, boolean invNumOnlyNmbrs,
                                    String ppCbRate, int ppMinAmt) {

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
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, true);

            // check merchant status
            BackendUtils.checkMerchantStatus(merchant, mEdr, mLogger);
            if(merchant.getAdmin_status()==DbConstants.USER_STATUS_UNDER_CLOSURE) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.ACC_UNDER_EXPIRY), "");
            }

            // update settings
            merchant.setCb_rate(cbRate);
            merchant.setCl_add_enable(addClEnabled);
            merchant.setEmail(email);
            merchant.setContactPhone(contactPhone);
            merchant.setInvoiceNumAsk(askLinkedInvNum);
            merchant.setInvoiceNumOptional(linkedInvNumOptional);
            merchant.setInvoiceNumOnlyNumbers(invNumOnlyNmbrs);
            merchant.setPrepaidCbRate(ppCbRate);
            merchant.setPrepaidCbMinAmt(ppMinAmt);
            // update object in DB
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

    public void deleteTrustedDevice(String deviceId, String curDeviceId) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "deleteTrustedDevice";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = deviceId;

        try {
            mLogger.debug("In deleteTrustedDevice: " + deviceId);

            // Fetch merchant with all child - as the same instance is to be returned too
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(
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
                    if (SecurityHelper.verifyDeviceId(device, curDeviceId)) {
                        throw new BackendlessException(String.valueOf(ErrorCodes.LOGGED_IN_DEVICE_DELETE), "");
                    }
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
            //return merchant;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

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

            // Fetch merchant - send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediately after
            Object userObj = BackendUtils.fetchCurrentUser(null, mEdr, mLogger, false);
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
            mLogger.setProperties(InvocationContext.getUserId(), userType, debugLogs);

            int customerIdType = CommonUtils.getCustomerIdType(customerId);
            //mLogger.debug("In getCashback: " + merchantId + ": " + customerId);
            //mLogger.debug("Before context: "+InvocationContext.asString());
            //mLogger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            // Fetch customer
            Customers customer = null;
            try {
                customer = BackendOps.getCustomer(customerId, customerIdType, false);
            } catch(BackendlessException e) {
                if(e.getCode().equals(String.valueOf(ErrorCodes.NO_SUCH_USER)) &&
                        customerIdType!=CommonConstants.ID_TYPE_AUTO) {
                    // this will happen always in case of 'user registration' etc

                    // if card was scanned, check its validity
                    if(customerIdType==CommonConstants.ID_TYPE_CARD) {
                        CustomerCards card = BackendOps.getCustomerCard(customerId, true);
                        BackendUtils.checkCardForAllocation(card, merchantId, mEdr, mLogger);
                    }
                    validException = true;
                }
                throw e;
            }
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getMobile_num();

            // Check customer status - mainky to allow unlocking if 'acc lock' duration passed
            // Else ignore the status and return customer details
            try {
                BackendUtils.checkCustomerStatus(customer, mEdr, mLogger);
            } catch (BackendlessException e) {
                // ignore the exception
            }

            // Cashback details to be returned - even if customer account/card is disabled/locked
            // so not checking for customer/card status

            // Fetch cashback record
            // Create where clause to fetch cashback
            String whereClause = "rowid = '" + customer.getPrivate_id()+merchantId + "'";
            mLogger.debug("Before fetchCashback: "+whereClause+","+merchantCbTable);
            ArrayList<Cashback> data = BackendOps.fetchCashback(whereClause, merchantCbTable, false);
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

            // Add 'customer details' in the cashback object to be returned
            // these details are not stored in DB along with cashback object
            cashback.setOther_details(MyCustomer.toCsvString(customer));
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
            // Fetch merchant - send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediatly after
            BackendlessUser user = BackendUtils.fetchCurrentBLUser(null, mEdr, mLogger, false);
            //Object userObj = BackendUtils.fetchCurrentUser(null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            boolean callByCC = false;
            Merchants merchant = null;
            if(userType==DbConstants.USER_TYPE_MERCHANT) {
                merchant = (Merchants) user.getProperty("merchant");;
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
            // fetch merchant stat object, if exists
            MerchantStats stats = BackendOps.fetchMerchantStats(merchantId);
            // create object if not already available
            if (stats == null) {
                mLogger.debug("Creating new stats object");
                stats = new MerchantStats();
                stats.setMerchant_id(merchantId);
            } else {
                calculateAgain = CommonUtils.mchntStatsRefreshReq(stats);
                // return old object, if last updated within configured hours
                /*Date updateTime = stats.getUpdated();
                if(updateTime==null) {
                    updateTime = stats.getCreated();
                }
                long updated = updateTime.getTime();
                long now = (new Date()).getTime();
                if( (now - updated) < (MyGlobalSettings.getMchntDashBNoRefreshHrs()*60*60*1000) ) {
                    // return old object - dont calculate again
                    calculateAgain = false;
                }*/
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
                        merchant.getCashback_table(), true);
                if (data != null) {
                    // loop on all cashback objects and calculate stats
                    mLogger.debug("Fetched cashback records: " + merchantId + ", " + data.size());

                    StringBuilder sb = new StringBuilder(CsvConverter.CB_CSV_MAX_SIZE * (data.size()+1));
                    // Add first line as header - to give the file creation time in epoch
                    sb.append(String.valueOf((new Date()).getTime())).append(CommonConstants.CSV_DELIMETER);

                    for (int k = 0; k < data.size(); k++) {
                        Cashback cb = data.get(k);

                        // if all amounts 0 - then ignore
                        // i.e. probably this customer bought the card from this merchant
                        // but didn't do any txn as such
                        if(cb.getCb_credit()==0 &&
                                cb.getCb_debit()==0 &&
                                cb.getCl_credit()==0 &&
                                cb.getCl_debit()==0) {
                            continue;
                        }

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
                            // customer details as CSV - in other_details field of CB object
                            cb.setOther_details(MyCustomer.toCsvString(cb.getCustomer()));
                            // CB details as CSV
                            sb.append(CommonConstants.NEWLINE_SEP).append(CsvConverter.csvStrFromCb(cb));
                        } else {
                            // All cb shud have linked customer
                            // ignore error - but log the same
                            mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.IGNORED_ERROR_CB_WITH_NO_CUST;
                        }
                    }

                    // upload data as CSV file
                    createCsvFile(sb.toString(), merchantId, user.getObjectId(), callByCC);
                }

                // save stats object - don't bother about return status
                // This is just for our own reporting purpose,
                // as for merchant stats anyways are calculated fresh each time from cashback objects
                try {
                    stats = BackendOps.saveMerchantStats(stats);

                    long updateTime = (stats.getUpdated()==null) ?
                            stats.getCreated().getTime() :
                            stats.getUpdated().getTime();
                    mLogger.debug("Re-calculated and updated stats: " + updateTime);
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
            Object userObj = BackendUtils.fetchCurrentUser(null, mEdr, mLogger, false);
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
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(
                    DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);
            // not checking for merchant account status

            // archive txns
            //mLogger.debug("Context: "+InvocationContext.asString());
            //mLogger.debug("Headers: "+ HeadersManager.getInstance().getHeaders().toString());
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

    public MerchantOrders createMchntOrder(String itemSku, int itemQty, int totalPrice) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "createMchntOrder";

        try {
            mLogger.debug("In createMchntOrder");

            // Fetch merchant
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);

            // Check SKU and Price
            int price = 0;
            if(itemSku.equals(DbConstants.SKU_CUSTOMER_CARDS)) {
                price = itemQty * MyGlobalSettings.getCustCardPrice();
                if(price!=totalPrice) {
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Total Price not correct: "+totalPrice);
                }
            } else {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Invalid SKU: "+itemSku);
            }

            // Check if first order
            boolean isFirstOrder = false;
            String whereClause = "merchantId = '"+merchant.getAuto_id()+"'";
            if(BackendOps.getMchntOrderCnt(whereClause)==0) {
                isFirstOrder = true;
            }

            // Create Order object
            Date now = new Date();
            MerchantOrders order = new MerchantOrders();
            order.setOrderId(BackendUtils.generateMchntOrderId());
            order.setMerchantId(merchant.getAuto_id());
            order.setCreateTime(now);
            order.setIsFirstOrder(isFirstOrder);

            order.setItemSku(itemSku);
            order.setItemQty(itemQty);
            order.setItemPrice(MyGlobalSettings.getCustCardPrice());
            order.setTotalPrice(price);

            order.setStatus(DbConstants.MCHNT_ORDER_STATUS.New.name());
            order.setStatusChangeTime(now);
            order.setStatusChangeUser(merchant.getAuto_id());

            // Save order object
            MerchantOrders savedOrder = BackendOps.saveMchntOrder(order);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

            return savedOrder;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void deleteMchntOrder(String orderId) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "deleteMchntOrder";

        try {
            mLogger.debug("In deleteMchntOrder");

            // Fetch merchant
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);

            // fetch order
            String whereClause = "orderId = '"+orderId+"'";
            List<MerchantOrders> orders = BackendOps.fetchMchntOrders(whereClause);
            if(orders==null) {
                throw new BackendlessException(String.valueOf(ErrorCodes.NO_DATA_FOUND), "No Order with ID: "+orderId);
            }
            MerchantOrders dbOrder = orders.get(0);

            // Check that order belongs to this mchnt only
            if(!dbOrder.getMerchantId().equals(merchant.getAuto_id())) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Merchant Order does not below to this merchant: "+orderId);
            }

            // Only New order can be deleted
            if(!dbOrder.getStatus().equals(DbConstants.MCHNT_ORDER_STATUS.New.name())) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                throw new BackendlessException(String.valueOf(ErrorCodes.MO_DEL_INVALID_STATUS), "Invalid MO status for delete:"+orderId);
            }

            // delete order
            BackendOps.deleteMchntOrder(dbOrder);

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
     * Public methods: Backend REST APIs
     * Customer operations by merchant
     */
    public Cashback registerCustomer(String customerMobile, String dob, int sex, String cardId, String otp, String firstName, String lastName) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "registerCustomer";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = customerMobile+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                cardId;

        Cashback cashback = null;
        boolean validException = false;
        try {
            mLogger.debug("Before context: "+InvocationContext.asString());
            mLogger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            // Fetch merchant
            Merchants merchant = (Merchants) BackendUtils.fetchCurrentUser(DbConstants.USER_TYPE_MERCHANT, mEdr, mLogger, false);
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customerMobile;

            // customer should not exist
            Customers customer = null;
            try {
                customer = BackendOps.getCustomer(customerMobile, CommonConstants.ID_TYPE_MOBILE, false);
            } catch (BackendlessException be) {
                // No such customer exist - we can proceed
                mLogger.debug("Customer not registered: "+customerMobile);
            }

            if(customer!=null) {
                // If here - means customer exist - return error
                mLogger.debug("Customer already registered: "+customerMobile+","+customer.getMobile_num());
                throw new BackendlessException(String.valueOf(ErrorCodes.USER_ALREADY_REGISTERED), customerMobile+" is already registered as customer");
            }

            // fetch customer card object
            CustomerCards card = null;
            if(cardId!=null && !cardId.isEmpty()) {
                card = BackendOps.getCustomerCard(cardId, true);
                mEdr[BackendConstants.EDR_CUST_CARD_NUM_IDX] = card.getCardNum();
                BackendUtils.checkCardForAllocation(card, merchant.getAuto_id(), mEdr, mLogger);
            }

            if (otp == null || otp.isEmpty()) {
                // Generate OTP and send SMS
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(customerMobile);
                newOtp.setMobile_num(customerMobile);
                newOtp.setOpcode(DbConstants.OP_REG_CUSTOMER);
                BackendOps.generateOtp(newOtp,"",mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                validException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OTP_GENERATED), "");

            } else {
                // Second run, as OTP available
                // Verify OTP
                if(!BackendOps.validateOtp(customerMobile, DbConstants.OP_REG_CUSTOMER, otp, mEdr, mLogger)) {
                    validException = true;
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_OTP), "");
                }

                // Create customer object
                customer = createCustomer();
                // generate PIN
                String pin = SecurityHelper.generateCustPin(customer, mLogger);
                // set fields
                // new record - so set it directly
                customer.setCashback_table(merchant.getCashback_table());
                customer.setTxn_tables(merchant.getTxn_table());
                customer.setMobile_num(customerMobile);
                customer.setName(firstName);
                customer.setDob(dob);
                customer.setSex(sex);
                customer.setRegDate(new Date());
                customer.setRegMchntId(merchant.getAuto_id());
                if(card!=null) {
                    customer.setCardId(card.getCard_id());
                    // set membership card
                    card.setCustId(customer.getPrivate_id());
                    card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ACTIVE);
                    card.setStatus_update_time(new Date());
                    customer.setMembership_card(card);
                } else {
                    customer.setCardId("");
                }

                // Create customer user
                BackendlessUser customerUser = new BackendlessUser();
                customerUser.setProperty("user_id", customerMobile);
                // use generated PIN as password
                customerUser.setPassword(BackendUtils.generateTempPassword());
                customerUser.setProperty("user_type", DbConstants.USER_TYPE_CUSTOMER);
                // Both 'user' and 'customer' objects get created in single go
                // This also ensures that 'customer' object's 'ownerId' remains null
                // This helps to avoid direct update from app by the merchant who created this customer object
                customerUser.setProperty("customer", customer);

                //mLogger.debug("Context: "+InvocationContext.asString());
                //mLogger.debug("Headers: "+ HeadersManager.getInstance().getHeaders().toString());
                customerUser = BackendOps.registerUser(customerUser);
                try {
                    customer = (Customers) customerUser.getProperty("customer");
                    // assign custom role to it
                    BackendOps.assignRole(customerMobile, BackendConstants.ROLE_CUSTOMER);

                } catch(Exception e) {
                    // rollback to not-usable state
                    rollbackRegister(customerMobile, card);
                    throw e;
                }

                // create directory for 'txnCsv' files
                String fileDir = null;
                String filePath = null;
                try {
                    fileDir = CommonUtils.getCustomerTxnDir(customer.getPrivate_id());
                    filePath = fileDir + CommonConstants.FILE_PATH_SEPERATOR+BackendConstants.DUMMY_FILENAME;
                    // saving dummy files to create parent directories
                    Backendless.Files.saveFile(filePath, BackendConstants.DUMMY_DATA.getBytes("UTF-8"), true);
                    // Give this customer permissions for this directory
                    FilePermission.READ.grantForUser( customerUser.getObjectId(), fileDir);
                    //FilePermission.DELETE.grantForUser( user.getObjectId(), fileDir);
                    //FilePermission.WRITE.grantForUser( user.getObjectId(), fileDir);
                    mLogger.debug("Saved dummy txn csv file: " + filePath);

                } catch(Exception e) {
                    mLogger.fatal("Failed to create customer directory: "+customerMobile+","+e.toString());
                    rollbackRegister(customerMobile,card);
                    throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), e.toString());
                }

                // create cashback also - to avoid another call to 'getCashback' from merchant
                cashback = createCbObject(merchant, customer);

                // Add 'customer details' in the cashback object to be returned
                // these details are not stored in DB along with cashback object
                cashback.setOther_details(MyCustomer.toCsvString(customer));
                // remove 'not needed sensitive' fields from cashback object
                stripCashback(cashback);

                // Send welcome sms to the customer
                String smsText = String.format(SmsConstants.SMS_CUSTOMER_REGISTER, customer.getName(), CommonConstants.CUSTOMER_CARE_NUMBER);
                SmsHelper.sendSMS(smsText, customerMobile, mEdr, mLogger, true);

                // Send SMS containing PIN
                smsText = String.format(SmsConstants.SMS_PIN, customerMobile, pin);
                SmsHelper.sendSMS(smsText, customerMobile, mEdr, mLogger, true);
            }

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


    /*
     * Private helper methods
     */
    private Customers createCustomer() {
        Customers customer = new Customers();
        customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
        customer.setStatus_reason(DbConstantsBackend.ENABLED_ACTIVE);
        customer.setStatus_update_time(new Date());
        //customer.setLastRenewDate(new Date());

        // get customer counter value and encode the same to get customer private id
        Long customerCnt =  BackendOps.fetchCounterValue(DbConstantsBackend.CUSTOMER_ID_COUNTER);
        String private_id = Base35.fromBase10(customerCnt, CommonConstants.CUSTOMER_INTERNAL_ID_LEN);
        mLogger.debug("Generated private id: "+private_id);
        customer.setPrivate_id(private_id);

        //String pin = BackendUtils.generateCustomerPIN();
        //mLogger.debug("Generated PIN: "+pin);
        // get salted hash for the PIN

        //customer.setTxn_pin(pin);

        return customer;
    }

    private Cashback handleCashbackCreate(Merchants merchant, Customers customer) {

        // Add cashback table name (of this merchant's) in customer record, if not already added (by some other merchant)
        String currTables = customer.getCashback_table();
        String newTables = (currTables==null||currTables.isEmpty())
                ? merchant.getCashback_table()
                : ( (currTables.contains(merchant.getCashback_table())
                    ? currTables
                    : currTables+CommonConstants.CSV_DELIMETER+merchant.getCashback_table()) );
        customer.setCashback_table(newTables);

        // Add transaction table name (of this merchant's) in customer record, if not already added (by some other merchant)
        currTables = customer.getTxn_tables();
        newTables = (currTables==null||currTables.isEmpty())
                ? merchant.getTxn_table()
                : ( (currTables.contains(merchant.getTxn_table())
                ? currTables
                : currTables+CommonConstants.CSV_DELIMETER+merchant.getTxn_table()) );
        customer.setTxn_tables(newTables);

        // not updating customer - as the same will be automatically done
        // along with cashback save in 'createCbObject' method

        // create new cashback object
        // intentionally doing it after updating customer for cashback table name
        return createCbObject(merchant, customer);
    }

    private Cashback createCbObject(Merchants merchant, Customers customer) {
        Cashback cashback = new Cashback();

        cashback.setRowid(customer.getPrivate_id() + merchant.getAuto_id());

        cashback.setMerchant_id(merchant.getAuto_id());
        cashback.setCust_private_id(customer.getPrivate_id());

        cashback.setCb_credit(0);
        cashback.setCb_debit(0);
        cashback.setCl_credit(0);
        cashback.setCl_debit(0);
        cashback.setTotal_billed(0);
        cashback.setCb_billed(0);

        //cashback.setMerchant(merchant);
        cashback.setCustomer(customer);

        return BackendOps.saveCashback(cashback, merchant.getCashback_table());
    }

    // Strip cashback object for information not needed by merchant app
    private void stripCashback(Cashback cashback) {
        cashback.setCust_private_id(null);
        cashback.setCustomer(null);
        //cashback.setMerchant(null);
        cashback.setRowid(null);
    }

    private void rollbackRegister(String custId, CustomerCards card) {
        mLogger.debug("In rollbackRegister");
        // add flag for manual check
        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;

        // rollback to not-usable state
        try {
            //BackendOps.decrementCounterValue(DbConstantsBackend.CUSTOMER_ID_COUNTER);

            Customers customer = BackendOps.getCustomer(custId, CommonConstants.ID_TYPE_MOBILE, false);
            customer.setMembership_card(null);
            BackendUtils.setCustomerStatus(customer, DbConstants.USER_STATUS_REG_ERROR, DbConstantsBackend.REG_ERROR_REG_FAILED,
                    mEdr, mLogger);
            /*customer.setAdmin_status(DbConstants.USER_STATUS_REG_ERROR);
            customer.setStatus_reason(DbConstantsBackend.REG_ERROR_REG_FAILED);
            BackendOps.updateCustomer(customer);*/

            // free up the card for next allocation
            if(card!=null) {
                card.setStatus(DbConstants.CUSTOMER_CARD_STATUS_NEW);
                // TODO: set any other fields required
                BackendOps.saveCustomerCard(card);
            }

        } catch(Exception ex) {
            mLogger.error("registerCustomer: Customer register Rollback failed: "+ex.toString(), ex);
            throw ex;
        }
    }

    private void createCsvFile(String data, String merchantId, String userObjId, boolean callByCC) {
        try {
            String filePath = CommonUtils.getMerchantCustFilePath(merchantId);

            // Do Base64 encode
            //mLogger.debug("Text bytes: "+data);
            byte[] bytes = Base64.getEncoder().encode(data.getBytes(StandardCharsets.UTF_8));
            //mLogger.debug("Encoded data: "+new String(bytes));

            String fileUrl = Backendless.Files.saveFile(filePath,bytes, true);
            //mLogger.debug("Customer data CSV file uploaded: " + fileUrl);

            // Give read access to this merchant to this file
            // no other merchant can read it
            if(!callByCC) {
                FilePermission.READ.grantForUser(userObjId, filePath);
            }
            //mLogger.debug("Gave read access to: " + filePath);

        } catch (Exception e) {
            mLogger.error("Customer data CSV file upload failed: "+ e.toString(),e);
            // For multiple days, single failure will be considered failure for all days
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Failed to create customer data CSV file: "+e.toString());
        }
    }
}
