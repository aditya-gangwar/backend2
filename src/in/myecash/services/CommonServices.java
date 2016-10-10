package in.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import in.myecash.constants.BackendConstants;
import in.myecash.constants.DbConstantsBackend;
import in.myecash.database.AllOtp;
import in.myecash.database.CustomerOps;
import in.myecash.database.InternalUser;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;
import in.myecash.common.database.*;
import in.myecash.common.constants.*;

import java.util.Date;

/**
 * Created by adgangwa on 19-07-2016.
 */

public class CommonServices implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.CommonServices");;
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];

    /*
     * Public methods: Backend REST APIs
     */
    public void changePassword(String userId, String oldPasswd, String newPasswd) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "changePassword";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = userId;

        try {
            mLogger.debug("In changePassword: "+userId);

            // Login to verify the old password
            // Note: afterLogin event handler will not get called - so 'trusted device' check will not happen
            // As event handlers are not called - for API calls made from server code.
            // In normal situation, this is not an issue - as user can call 'change password' only after login
            // However, in hacked situation, 'trusted device' check wont happen - ignoring this for now.
            BackendlessUser user = null;
            int userType = -1;
            boolean verifyFailed = false;
            try {
                user = BackendOps.loginUser(userId, oldPasswd);
                userType = (Integer)user.getProperty("user_type");
            } catch (BackendlessException e) {
                // mark for wrong attempt handling
                verifyFailed = true;
            }
            mEdr[BackendConstants.EDR_USER_ID_IDX] = (String)user.getProperty("user_id");
            if(userType==-1) {
                // exception in login scenario
                userType = BackendUtils.getUserType(userId);
            }
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(userType);

            // Find mobile number
            String mobileNum = null;
            switch(userType) {
                case DbConstants.USER_TYPE_MERCHANT:
                    mLogger.debug("Usertype is Merchant");
                    BackendOps.loadMerchant(user);
                    Merchants merchant = (Merchants)user.getProperty("merchant");
                    mLogger.setProperties(merchant.getAuto_id(), DbConstants.USER_TYPE_MERCHANT, merchant.getDebugLogs());
                    mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();

                    if(verifyFailed) {
                        BackendUtils.handleWrongAttempt(userId, merchant, userType,
                                DbConstantsBackend.WRONG_PARAM_TYPE_PASSWD, DbConstants.OP_CHANGE_PASSWD, mEdr, mLogger);
                        throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
                    }
                    mobileNum = merchant.getMobile_num();
                    break;

                case DbConstants.USER_TYPE_CUSTOMER:
                    mLogger.debug("Usertype is Customer");
                    BackendOps.loadCustomer(user);
                    Customers customer = (Customers)user.getProperty("customer");
                    mLogger.setProperties(customer.getPrivate_id(), DbConstants.USER_TYPE_CUSTOMER, customer.getDebugLogs());
                    mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getPrivate_id();

                    if(verifyFailed) {
                        BackendUtils.handleWrongAttempt(userId, customer, userType,
                                DbConstantsBackend.WRONG_PARAM_TYPE_PASSWD, DbConstants.OP_CHANGE_PASSWD, mEdr, mLogger);
                        throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
                    }
                    mobileNum = customer.getMobile_num();
                    break;

                case DbConstants.USER_TYPE_AGENT:
                    mLogger.debug("Usertype is Agent");
                    BackendOps.loadInternalUser(user);
                    InternalUser agent = (InternalUser)user.getProperty("agent");
                    mLogger.setProperties(agent.getId(), DbConstants.USER_TYPE_AGENT, agent.getDebugLogs());
                    mEdr[BackendConstants.EDR_INTERNAL_USER_ID_IDX] = agent.getId();
                    mobileNum = agent.getMobile_num();
                    break;
            }
            mLogger.debug("changePassword: User mobile number: "+mobileNum);

            // Change password
            user.setPassword(newPasswd);
            user = BackendOps.updateUser(user);

            // Send SMS through HTTP
            if(mobileNum!=null) {
                String smsText = SmsHelper.buildPwdChangeSMS(userId);
                if( SmsHelper.sendSMS(smsText, mobileNum, mEdr, mLogger) ){
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
                } else {
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                };
            } else {
                //TODO: raise alarm
                mLogger.error("In changePassword: mobile number is null");
                mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.IGNORED_ERROR_MOBILE_NUM_NA;
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            BackendOps.logoutUser();

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public Merchants getMerchant(String merchantId) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getMerchant";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantId;

        try {
            mLogger.debug("In getMerchant");

            // Send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediatly after
            Object userObj = BackendUtils.fetchCurrentUser(InvocationContext.getUserId(), null, mEdr, mLogger, true);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            Merchants merchant = null;
            if (userType == DbConstants.USER_TYPE_MERCHANT) {
                merchant = (Merchants) userObj;
                if (!merchant.getAuto_id().equals(merchantId)) {
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA),
                            "Invalid merchant id provided: " + merchantId);
                }
            } else if (userType == DbConstants.USER_TYPE_CC ||
                    userType == DbConstants.USER_TYPE_AGENT) {
                // fetching merchant user instead of direct merchant object - for lastLogin value
                //BackendlessUser user = BackendOps.fetchUser(merchantId, DbConstants.USER_TYPE_MERCHANT, true);
                //merchant = (Merchants)user.getProperty("merchant");
                //merchant.setLastLogin((Date)user.getProperty("lastLogin"));
                merchant = BackendOps.getMerchant(merchantId, true, true);
                mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();
            } else {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return merchant;
        } catch (Exception e) {
            BackendUtils.handleException(e, false, mLogger, mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime, mLogger, mEdr);
        }
    }

    public Customers getCustomer(String custId) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getCustomer";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = custId;

        try {
            mLogger.debug("In getCustomer");

            // Send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediatly after
            Object userObj = BackendUtils.fetchCurrentUser(InvocationContext.getUserId(), null, mEdr, mLogger, true);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            Customers customer = null;
            if (userType == DbConstants.USER_TYPE_CUSTOMER) {
                customer = (Customers) userObj;
                if (!customer.getMobile_num().equals(custId)) {
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA),
                            "Invalid customer id provided: " + custId);
                }
            } else if (userType == DbConstants.USER_TYPE_CC) {
                // fetching merchant user instead of direct merchant object - for lastLogin value
                //BackendlessUser user = BackendOps.fetchUser(merchantId, DbConstants.USER_TYPE_MERCHANT, true);
                //merchant = (Merchants)user.getProperty("merchant");
                //merchant.setLastLogin((Date)user.getProperty("lastLogin"));
                customer = BackendOps.getCustomer(custId, BackendUtils.getCustomerIdType(custId), true);
                mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getPrivate_id();
            } else {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return customer;
        } catch (Exception e) {
            BackendUtils.handleException(e, false, mLogger, mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime, mLogger, mEdr);
        }
    }

    /*
     * OP_NEW_CARD - Need Mobile, PIN and OTP on registered number
     * OP_CHANGE_MOBILE - Need Mobile, CardId, PIN and OTP on new number
     * OP_RESET_PIN - Need CardId and OTP on registered number (OTP not required if done by customer himself from app)
     * OP_CHANGE_PIN - Need PIN(existing) - only from customer app
     */
    public void execCustomerOp(String opCode, String custId, String cardId, String otp, String pin, String opParam) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "execCustomerOp";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = opCode+BackendConstants.BACKEND_EDR_SUB_DELIMETER +
                custId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                cardId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                otp+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                pin+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                opParam;

        boolean positiveException = false;

        try {
            // Both merchant and customer users are allowed

            // We need the 'user' object also for 'change mobile' scenario
            // so, not using 'fetchCurrentUser' function directly
            BackendlessUser user = BackendOps.fetchUserByObjectId(InvocationContext.getUserId(), false);
            mEdr[BackendConstants.EDR_USER_ID_IDX] = (String) user.getProperty("user_id");
            int userType = (Integer)user.getProperty("user_type");
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(userType);

            String merchantId = null;
            Customers customer = null;
            BackendlessUser custUser = null;
            switch (userType) {
                case DbConstants.USER_TYPE_MERCHANT:
                    // OP_CHANGE_PIN not allowed to merchant
                    if(opCode.equals(DbConstants.OP_CHANGE_PIN)) {
                        throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
                    }
                    Merchants merchant = (Merchants) user.getProperty("merchant");
                    merchantId = merchant.getAuto_id();
                    mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();
                    mLogger.setProperties(mEdr[BackendConstants.EDR_USER_ID_IDX], userType, merchant.getDebugLogs());
                    // check if merchant is enabled
                    BackendUtils.checkMerchantStatus(merchant, mEdr, mLogger);

                    // Fetch Customer user
                    custUser = BackendOps.fetchUser(custId, DbConstants.USER_TYPE_CUSTOMER, false);
                    customer = (Customers) custUser.getProperty("customer");
                    break;

                case DbConstants.USER_TYPE_CUSTOMER:
                    // OP_NEW_CARD not allowed from customer app
                    if(opCode.equals(DbConstants.OP_NEW_CARD)) {
                        throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
                    }
                    custUser = user;
                    customer = (Customers) user.getProperty("customer");
                    if(!custId.isEmpty() && !custId.equals(customer.getMobile_num())) {
                        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                        throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
                    }

                    mLogger.setProperties(mEdr[BackendConstants.EDR_USER_ID_IDX], userType, customer.getDebugLogs());
                    break;

                default:
                    throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
            }

            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getPrivate_id();
            String cardIdDb = customer.getMembership_card().getCard_id();
            mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = cardIdDb;

            // check if customer is enabled
            BackendUtils.checkCustomerStatus(customer, mEdr, mLogger);
            // if customer in 'restricted access' mode - customer ops not allowed
            if(customer.getAdmin_status()==DbConstants.USER_STATUS_MOB_CHANGE_RECENT) {
                throw new BackendlessException(String.valueOf(ErrorCodes.USER_MOB_CHANGE_RESTRICTED_ACCESS), "");
            }

            // OTP not required for 'Change PIN' operation
            // for others generate the same, if not provided
            if (otp == null || otp.isEmpty() &&
                    !opCode.equals(DbConstants.OP_CHANGE_PIN)) {

                // Don't verify QR card# for 'new card' operation
                if (!opCode.equals(DbConstants.OP_NEW_CARD) &&
                        !cardIdDb.equals(cardId)) {

                    BackendUtils.handleWrongAttempt(custId, customer, DbConstants.USER_TYPE_CUSTOMER, opCode,
                            DbConstantsBackend.WRONG_PARAM_TYPE_VERIFICATION, mEdr, mLogger);
                    throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
                }

                // Don't verify PIN for 'reset PIN' operation
                if (!opCode.equals(DbConstants.OP_RESET_PIN) &&
                        !customer.getTxn_pin().equals(pin)) {

                    BackendUtils.handleWrongAttempt(custId, customer, DbConstants.USER_TYPE_CUSTOMER,
                            DbConstantsBackend.WRONG_PARAM_TYPE_PIN, opCode, mEdr, mLogger);
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_PIN), "Wrong PIN attempt: " + customer.getMobile_num());
                }

                // Generate OTP and send SMS
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(custId);
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
                // or 'Change PIN' operation

                // Verify OTP
                if(!opCode.equals(DbConstants.OP_CHANGE_PIN)) {
                    BackendOps.validateOtp(custId, opCode, otp);
                }

                //String oldMobile = customer.getMobile_num();
                //String oldCard = customer.getMembership_card().getCard_id();

                // First add to the Merchant Ops table, and then do the actual update
                // Doing so, as its easy to rollback by deleting added customerOp record
                // then the other way round.
                // Need to ensure that CustomerOp table record is always there, in case update is succesfull
                CustomerOps customerOp = new CustomerOps();
                customerOp.setPrivateId(customer.getPrivate_id());
                customerOp.setOp_code(opCode);
                customerOp.setMobile_num(customer.getMobile_num());
                if(userType==DbConstants.USER_TYPE_CUSTOMER) {
                    customerOp.setInitiatedBy(DbConstantsBackend.USER_OP_INITBY_CUSTOMER);
                } else {
                    customerOp.setInitiatedBy(DbConstantsBackend.USER_OP_INITBY_MCHNT);
                    customerOp.setRequestor_id(merchantId);
                }
                customerOp.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_APP);
                if(opCode==DbConstants.OP_RESET_PIN) {
                    customerOp.setOp_status(DbConstantsBackend.USER_OP_STATUS_PENDING);
                } else {
                    customerOp.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
                }

                // set extra params in presentable format
                if(opCode.equals(DbConstants.OP_NEW_CARD)) {
                    String extraParams = "Old Card: "+customer.getMembership_card().getCard_id()+", New Card: "+cardId;
                    customerOp.setExtra_op_params(extraParams);
                    customerOp.setReason(opParam);

                } else if(opCode.equals(DbConstants.OP_CHANGE_MOBILE)) {
                    String extraParams = "Old Mobile: "+customer.getMobile_num()+", New Mobile: "+customer.getMobile_num();
                    customerOp.setExtra_op_params(extraParams);
                }
                customerOp = BackendOps.saveCustomerOp(customerOp);

                // Do the actual Update now
                try {
                    switch (opCode) {
                        case DbConstants.OP_NEW_CARD:
                            if (cardId == null || cardId.isEmpty() || cardId.length() != CommonConstants.CUSTOMER_CARDID_LEN) {
                                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Invalid new Card ID");
                            }
                            changeCustomerCard(customer, cardId, opParam);
                            break;

                        case DbConstants.OP_CHANGE_MOBILE:
                            if (opParam == null || opParam.isEmpty() || opParam.length() != CommonConstants.MOBILE_NUM_LENGTH) {
                                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Invalid new Mobile value");
                            }
                            changeCustomerMobile(custUser, opParam);
                            break;

                        case DbConstants.OP_RESET_PIN:
                            //resetCustomerPin(customer);
                            positiveException = true;
                            throw new BackendlessException(String.valueOf(ErrorCodes.OP_SCHEDULED), "");

                        case DbConstants.OP_CHANGE_PIN:
                            if (opParam == null || opParam.isEmpty() || opParam.length() != CommonConstants.PIN_LEN) {
                                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Invalid new PIN value");
                            }
                            String pinDb = customer.getTxn_pin();
                            if (pin == null || !pinDb.equals(pin)) {
                                BackendUtils.handleWrongAttempt(custId, customer, DbConstants.USER_TYPE_CUSTOMER, opCode,
                                        DbConstantsBackend.WRONG_PARAM_TYPE_PIN, mEdr, mLogger);
                                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_PIN), "Wrong PIN attempt: " + customer.getMobile_num());
                            }
                            changeCustomerPin(customer, opParam);
                            break;
                    }
                } catch(Exception e) {
                    if(!positiveException) {
                        mLogger.error("execCustomerOp: Exception while customer operation: "+customer.getPrivate_id());
                        // Rollback - delete customer op added
                        try {
                            BackendOps.deleteCustomerOp(customerOp);
                        } catch(Exception ex) {
                            mLogger.fatal("execCustomerOp: Failed to rollback: customer op deletion failed: "+customer.getPrivate_id());
                            // Rollback also failed
                            mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                            throw ex;
                        }
                    }
                    throw e;
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
     * Private helper methods
     */
    private String changeCustomerCard(Customers customer, String newCardId, String reason) {
        // fetch new card record
        CustomerCards newCard = BackendOps.getCustomerCard(newCardId);
        BackendUtils.checkCardForAllocation(newCard);
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
        SmsHelper.sendSMS(smsText, customer.getMobile_num(), mEdr, mLogger);

        return oldCard.getCard_id();
    }

    private String changeCustomerMobile(BackendlessUser custUser, String newMobile) {
        Customers customer = (Customers) custUser.getProperty("customer");
        String oldMobile = customer.getMobile_num();

        // update mobile number
        custUser.setProperty("user_id", newMobile);
        customer.setMobile_num(newMobile);
        // update status to 'restricted access'
        // not using setCustomerStatus() fx. - to avoid two DB operations
        customer.setAdmin_status(DbConstants.USER_STATUS_MOB_CHANGE_RECENT);
        customer.setStatus_reason("Mobile number changed");
        customer.setStatus_update_time(new Date());

        custUser.setProperty("customer", customer);
        BackendOps.updateUser(custUser);

        // Send message to customer informing the same - ignore sent status
        String smsText = SmsHelper.buildMobileChangeSMS( oldMobile, newMobile );
        SmsHelper.sendSMS(smsText, oldMobile+","+newMobile, mEdr, mLogger);

        return oldMobile;
    }

    private void resetCustomerPin(Customers customer) {
        // generate pin
        String newPin = BackendUtils.generateCustomerPIN();

        // update user account for the PIN
        //TODO: encode PIN
        customer.setTxn_pin(newPin);
        BackendOps.updateCustomer(customer);

        // Send SMS through HTTP
        String smsText = SmsHelper.buildCustPinResetSMS(customer.getMobile_num(), newPin);
        SmsHelper.sendSMS(smsText, customer.getMobile_num(), mEdr, mLogger);
    }

    private void changeCustomerPin(Customers customer, String newPin) {
        // update user account for the PIN
        //TODO: encode PIN
        customer.setTxn_pin(newPin);
        BackendOps.updateCustomer(customer);

        // Send SMS through HTTP
        String smsText = SmsHelper.buildPinChangeSMS(customer.getMobile_num());
        SmsHelper.sendSMS(smsText, customer.getMobile_num(), mEdr, mLogger);
    }
}
