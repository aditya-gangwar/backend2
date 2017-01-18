package in.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import in.myecash.common.CommonUtils;
import in.myecash.common.MyGlobalSettings;
import in.myecash.database.AllOtp;
import in.myecash.common.database.CustomerOps;
import in.myecash.messaging.SmsConstants;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

import in.myecash.common.database.*;
import in.myecash.common.constants.*;
import in.myecash.constants.*;
import in.myecash.utilities.SecurityHelper;

import java.util.Date;

/**
 * Created by adgangwa on 23-09-2016.
 */
public class CustomerServicesNoLogin implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.CustomerServicesNoLogin");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     */
    public void enableCustAccount(String userId, String passwd, String rcvdOtp, String cardNum, String argPin) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "enableCustAccount";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = userId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                cardNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                rcvdOtp;
        mEdr[BackendConstants.EDR_USER_ID_IDX] = userId;
        mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_CUSTOMER);

        boolean validException = false;
        try {
            mLogger.debug("In enableCustAccount: " + userId);
            //mLogger.debug(InvocationContext.asString());
            //mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

            // Fetch customer
            Customers customer = BackendOps.getCustomer(userId, BackendConstants.ID_TYPE_MOBILE, true);
            String cardNumDb = customer.getMembership_card().getCardNum();
            String mobileNum = customer.getMobile_num();
            mLogger.setProperties(customer.getPrivate_id(), DbConstants.USER_TYPE_CUSTOMER, customer.getDebugLogs());
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getPrivate_id();

            if(!customer.getAdmin_status().equals(DbConstants.USER_STATUS_DISABLED)) {
                // Account not in disabled state
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Account not in Disabled State");
            }

            if (rcvdOtp == null || rcvdOtp.isEmpty()) {
                // first call - verify and generate OTP

                // Atleast one of 'cardNum' or 'PIN' is required
                if( (cardNum==null || cardNum.isEmpty()) && (argPin==null || argPin.isEmpty()) ) {
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "Either of Card Number or PIN is required");
                }

                // Verify PIN - if provided
                if ( argPin!=null && !argPin.isEmpty() &&
                        !SecurityHelper.verifyCustPin(customer, argPin, mLogger)) {

                    validException = true;
                    BackendUtils.handleWrongAttempt(mobileNum, customer, DbConstants.USER_TYPE_CUSTOMER,
                            DbConstantsBackend.WRONG_PARAM_TYPE_PIN, DbConstants.OP_ENABLE_ACC, mEdr, mLogger);
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_PIN), "Wrong PIN attempt: " + mobileNum);
                }

                // verify PIN - if provided
                if ( cardNum!=null && !cardNum.isEmpty() &&
                        !cardNumDb.equals(cardNum)) {

                    validException = true;
                    BackendUtils.handleWrongAttempt(mobileNum, customer, DbConstants.USER_TYPE_CUSTOMER,
                            DbConstantsBackend.WRONG_PARAM_TYPE_CARDID, DbConstants.OP_ENABLE_ACC, mEdr, mLogger);
                    throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED_CARDID), "");
                }

                // Verification successful - Generate OTP
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(mobileNum);
                newOtp.setMobile_num(mobileNum);
                newOtp.setOpcode(DbConstants.OP_ENABLE_ACC);
                BackendOps.generateOtp(newOtp,mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                validException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OTP_GENERATED), "");
            }

            // Second call - verify OTP only

            if(!BackendOps.validateOtp(userId, DbConstants.OP_ENABLE_ACC, rcvdOtp, mEdr, mLogger)) {
                validException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_OTP), "");
            }

            // Login to verify the given password
            // Also it will provide permissions to edit customer object
            // Note: afterLogin event handler will not get called - so 'customer status' check will not happen
            // As event handlers are not called - for API calls made from server code.
            try {
                BackendlessUser user = BackendOps.loginUser(userId, passwd);
            } catch (BackendlessException e) {
                // crude way to check for failure due to wrong password
                if(e.getMessage().contains("password")) {
                    // Handle wrong attempt
                    validException = true;
                    BackendUtils.handleWrongAttempt(userId, customer, DbConstants.USER_TYPE_CUSTOMER,
                            DbConstantsBackend.WRONG_PARAM_TYPE_PASSWD, DbConstants.OP_ENABLE_ACC, mEdr, mLogger);
                    throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED_PASSWD), "");
                } else {
                    throw e;
                }
            }

            // Add customer op first - then update status
            CustomerOps op = new CustomerOps();
            op.setCreateTime(new Date());
            op.setPrivateId(customer.getPrivate_id());
            op.setMobile_num(customer.getMobile_num());
            op.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
            op.setImgFilename("");
            op.setInitiatedBy( DbConstantsBackend.USER_OP_INITBY_CUSTOMER);
            op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_APP);
            op.setOp_code(DbConstants.OP_ENABLE_ACC);
            op = BackendOps.saveCustomerOp(op);

            // Enable account
            // change status - this will save the updated merchant object too
            try {
                BackendUtils.setCustomerStatus(customer, DbConstants.USER_STATUS_ACTIVE, "", mEdr, mLogger);
            } catch(Exception e) {
                mLogger.error("enableCustAccount: Exception while updating status: "+userId);
                // Rollback - delete customer op added
                try {
                    BackendOps.deleteCustomerOp(op);
                } catch(Exception ex) {
                    mLogger.fatal("enableCustAccount: Failed to rollback: customer op deletion failed: "+userId);
                    // Rollback also failed
                    mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw ex;
                }
                throw e;
            }

            // Send SMS
            String text = String.format(SmsConstants.SMS_ACCOUNT_ENABLE,
                    CommonUtils.getPartialVisibleStr(customer.getMobile_num()));
            // ignore SMS sent status
            SmsHelper.sendSMS(text, customer.getMobile_num(), mEdr, mLogger, true);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            BackendOps.logoutUser();

        } catch(Exception e) {
            BackendUtils.handleException(e,validException,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void resetCustomerPassword(String mobileNum, String secret) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "resetInternalUserPassword";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = mobileNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                secret;
       boolean validException = false;

        try {
            mLogger.debug("In resetCustomerPassword: " + mobileNum);

            // check if any request already pending
            if( BackendOps.fetchCustomerOps(custPwdResetWhereClause(mobileNum)) != null) {
                validException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.DUPLICATE_ENTRY), "");
            }

            // fetch user with the given id with related object
            // taking USER_TYPE_AGENT default - doesnt matter as such
            BackendlessUser user = BackendOps.fetchUser(mobileNum, DbConstants.USER_TYPE_CUSTOMER, false);
            int userType = (Integer)user.getProperty("user_type");
            if(userType != DbConstants.USER_TYPE_CUSTOMER) {
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_SECURITY_BREACH;
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED),mobileNum+" is not a customer.");
            }

            Customers customer = (Customers) user.getProperty("customer");
            mLogger.setProperties(customer.getPrivate_id(), userType, customer.getDebugLogs());
            mEdr[BackendConstants.EDR_USER_ID_IDX] = (String)user.getProperty("user_id");
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = ((Integer)user.getProperty("user_type")).toString();
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getPrivate_id();

            // check admin status
            BackendUtils.checkCustomerStatus(customer, mEdr, mLogger);

            // check for 'extra verification'
            String cardNumDb = customer.getMembership_card().getCardNum();
            if (cardNumDb == null || !cardNumDb.equalsIgnoreCase(secret)) {
                BackendUtils.handleWrongAttempt(mobileNum, customer, userType,
                        DbConstantsBackend.WRONG_PARAM_TYPE_CARDID, DbConstants.OP_RESET_PASSWD, mEdr, mLogger);
                validException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED_CARDID), "");
            }

            // For new registered customer - send the password immediately
            if (!customer.getFirst_login_ok()) {
                customerPwdResetImmediate(user, customer);
                mLogger.debug("Processed passwd reset op for: " + customer.getPrivate_id());
            } else {
                // create row in CustomerOps table
                CustomerOps op = new CustomerOps();
                op.setMobile_num(customer.getMobile_num());
                op.setPrivateId(customer.getPrivate_id());
                op.setOp_code(DbConstants.OP_RESET_PASSWD);
                op.setOp_status(DbConstantsBackend.USER_OP_STATUS_PENDING);
                op.setInitiatedBy(DbConstantsBackend.USER_OP_INITBY_CUSTOMER);
                op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_APP);
                op.setCreateTime(new Date());

                BackendOps.saveCustomerOp(op);
                mLogger.debug("Processed passwd reset op for: " + customer.getPrivate_id());

                // Send SMS to inform
                Integer mins = MyGlobalSettings.getCustPasswdResetMins() + GlobalSettingConstants.CUSTOMER_PASSWORD_RESET_TIMER_INTERVAL;
                String smsText = String.format(SmsConstants.SMS_PASSWD_RESET_SCHEDULED,
                        CommonUtils.getPartialVisibleStr(op.getMobile_num()), mins);
                // ignore error
                SmsHelper.sendSMS(smsText, op.getMobile_num(), mEdr, mLogger, true);

                validException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OP_SCHEDULED), "");
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

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
    private void customerPwdResetImmediate(BackendlessUser user, Customers customer) {
        // generate password
        String passwd = BackendUtils.generateTempPassword();
        // update user account for the password
        user.setPassword(passwd);
        user = BackendOps.updateUser(user);
        mLogger.debug("Updated customer for password reset: "+customer.getPrivate_id());

        // Send SMS through HTTP
        String smsText = SmsHelper.buildFirstPwdResetSMS(customer.getMobile_num(), passwd);
        // Dont retry SMS in case of failure
        if( !SmsHelper.sendSMS(smsText, customer.getMobile_num(), mEdr, mLogger, false) ){
            throw new BackendlessException(String.valueOf(ErrorCodes.SEND_SMS_FAILED), "");
        }
        mLogger.debug("Sent first password reset SMS: "+customer.getMobile_num());
    }

    private String custPwdResetWhereClause(String customerMobileNum) {
        StringBuilder whereClause = new StringBuilder();

        whereClause.append("op_code = '").append(DbConstants.OP_RESET_PASSWD).append("'");
        whereClause.append(" AND op_status = '").append(DbConstantsBackend.USER_OP_STATUS_PENDING).append("'");
        whereClause.append("AND mobile_num = '").append(customerMobileNum).append("'");

        // created within last 'cool off' mins
        long time = (new Date().getTime()) - (MyGlobalSettings.getCustPasswdResetMins() * 60 * 1000);
        whereClause.append(" AND createTime > ").append(time);
        return whereClause.toString();
    }

}

