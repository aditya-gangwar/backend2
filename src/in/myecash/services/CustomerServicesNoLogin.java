package in.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import in.myecash.constants.*;
import in.myecash.database.CustomerOps;
import in.myecash.database.Customers;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.CommonUtils;
import in.myecash.utilities.MyLogger;

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
        CommonUtils.initTableToClassMappings();
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
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_DUPLICATE_REQUEST, "");
            }

            // fetch user with the given id with related object
            // taking USER_TYPE_AGENT default - doesnt matter as such
            BackendlessUser user = BackendOps.fetchUser(mobileNum, DbConstants.USER_TYPE_CUSTOMER, false);
            int userType = (Integer)user.getProperty("user_type");
            if(userType != DbConstants.USER_TYPE_CUSTOMER) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,mobileNum+" is not a customer.");
            }

            Customers customer = (Customers) user.getProperty("customer");
            mLogger.setProperties(customer.getPrivate_id(), userType, customer.getDebugLogs());
            mEdr[BackendConstants.EDR_USER_ID_IDX] = (String)user.getProperty("user_id");
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = ((Integer)user.getProperty("user_type")).toString();
            mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getPrivate_id();

            // check admin status
            CommonUtils.checkCustomerStatus(customer, mLogger);

            // check for 'extra verification'
            String cardId = customer.getCardId();
            if (cardId == null || !cardId.equalsIgnoreCase(secret)) {
                CommonUtils.handleWrongAttempt(mobileNum, customer, userType, DbConstantsBackend.ATTEMPT_TYPE_PASSWORD_RESET, mLogger);
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
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
                op.setOp_code(DbConstants.CUSTOMER_OP_RESET_PASSWORD);
                op.setOp_status(DbConstantsBackend.USER_OP_STATUS_PENDING);
                op.setInitiatedBy(DbConstantsBackend.USER_OP_INITBY_CUSTOMER);
                op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_APP);

                BackendOps.saveCustomerOp(op);
                mLogger.debug("Processed passwd reset op for: " + customer.getPrivate_id());

                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.BE_RESPONSE_OP_SCHEDULED, "");
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }


    /*
     * Private helper methods
     */
    private void customerPwdResetImmediate(BackendlessUser user, Customers customer) {
        // generate password
        String passwd = CommonUtils.generateTempPassword();
        // update user account for the password
        user.setPassword(passwd);
        user = BackendOps.updateUser(user);
        mLogger.debug("Updated customer for password reset: "+customer.getPrivate_id());

        // Send SMS through HTTP
        String smsText = SmsHelper.buildFirstPwdResetSMS(customer.getMobile_num(), passwd);
        if( SmsHelper.sendSMS(smsText, customer.getMobile_num(), mLogger) ){
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
        }
        mLogger.debug("Sent first password reset SMS: "+customer.getMobile_num());
    }

    private String custPwdResetWhereClause(String customerMobileNum) {
        StringBuilder whereClause = new StringBuilder();

        whereClause.append("op_code = '").append(DbConstants.CUSTOMER_OP_RESET_PASSWORD).append("'");
        whereClause.append(" AND op_status = '").append(DbConstantsBackend.USER_OP_STATUS_PENDING).append("'");
        whereClause.append("AND mobile_num = '").append(customerMobileNum).append("'");

        // created within last 'cool off' mins
        long time = (new Date().getTime()) - (GlobalSettingsConstants.CUSTOMER_PASSWORD_RESET_COOL_OFF_MINS * 60 * 1000);
        whereClause.append(" AND created > ").append(time);
        return whereClause.toString();
    }

}

