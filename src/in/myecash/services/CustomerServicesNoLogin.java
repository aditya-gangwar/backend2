package in.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import in.myecash.common.MyGlobalSettings;
import in.myecash.database.CustomerOps;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

import in.myecash.common.database.*;
import in.myecash.common.constants.*;
import in.myecash.constants.*;

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
   public void resetCustomerPassword(String mobileNum, String secret) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "resetInternalUserPassword";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = mobileNum+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                secret;
       boolean positiveException = false;

        try {
            mLogger.debug("In resetCustomerPassword: " + mobileNum);

            // check if any request already pending
            if( BackendOps.fetchMerchantOps(custPwdResetWhereClause(mobileNum)) != null) {
                throw new BackendlessException(String.valueOf(ErrorCodes.DUPLICATE_ENTRY), "");
            }

            // fetch user with the given id with related object
            // taking USER_TYPE_AGENT default - doesnt matter as such
            BackendlessUser user = BackendOps.fetchUser(mobileNum, DbConstants.USER_TYPE_CUSTOMER, false);
            int userType = (Integer)user.getProperty("user_type");
            if(userType != DbConstants.USER_TYPE_CUSTOMER) {
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
            String cardId = customer.getCardId();
            if (cardId == null || !cardId.equalsIgnoreCase(secret)) {
                BackendUtils.handleWrongAttempt(mobileNum, customer, userType,
                        DbConstantsBackend.WRONG_PARAM_TYPE_VERIFICATION, DbConstants.OP_RESET_PASSWD, mEdr, mLogger);
                throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
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

                BackendOps.saveCustomerOp(op);
                mLogger.debug("Processed passwd reset op for: " + customer.getPrivate_id());

                positiveException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OP_SCHEDULED), "");
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
    private void customerPwdResetImmediate(BackendlessUser user, Customers customer) {
        // generate password
        String passwd = BackendUtils.generateTempPassword();
        // update user account for the password
        user.setPassword(passwd);
        user = BackendOps.updateUser(user);
        mLogger.debug("Updated customer for password reset: "+customer.getPrivate_id());

        // Send SMS through HTTP
        String smsText = SmsHelper.buildFirstPwdResetSMS(customer.getMobile_num(), passwd);
        if( !SmsHelper.sendSMS(smsText, customer.getMobile_num(), mEdr, mLogger) ){
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
        whereClause.append(" AND created > ").append(time);
        return whereClause.toString();
    }

}

