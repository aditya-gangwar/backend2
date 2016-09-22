import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.myecash.constants.BackendConstants;
import com.myecash.constants.BackendResponseCodes;
import com.myecash.constants.DbConstants;
import com.myecash.constants.DbConstantsBackend;
import com.myecash.database.Customers;
import com.myecash.messaging.SmsHelper;
import com.myecash.utilities.BackendOps;
import com.myecash.utilities.CommonUtils;
import com.myecash.utilities.MyLogger;

/**
 * Created by adgangwa on 23-09-2016.
 */
public class CustomerServicesNoLogin implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.CustomerServicesNoLogin");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     */
   public void resetCustomerPassword(String userId, String secret1) {
        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "resetInternalUserPassword";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = userId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                secret1;

        try {
            mLogger.debug("In resetCustomerPassword: " + userId);

            // fetch user with the given id with related object
            // taking USER_TYPE_AGENT default - doesnt matter as such
            BackendlessUser user = BackendOps.fetchUser(userId, DbConstants.USER_TYPE_CUSTOMER, false);
            int userType = (Integer)user.getProperty("user_type");
            if(userType != DbConstants.USER_TYPE_CUSTOMER) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,userId+" is not a customer.");
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
            if (cardId == null || !cardId.equalsIgnoreCase(secret1)) {
                CommonUtils.handleWrongAttempt(userId, customer, userType, DbConstantsBackend.ATTEMPT_TYPE_PASSWORD_RESET, mLogger);
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
            }

            customerPwdResetImmediate(user, customer);
            mLogger.debug("Processed passwd reset op for: " + customer.getMobile_num());

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
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
        mLogger.debug("Updated internal user for password reset: "+customer.getPrivate_id());

        // Send SMS through HTTP
        String smsText = SmsHelper.buildPwdResetSMS(customer.getMobile_num(), passwd);
        if( SmsHelper.sendSMS(smsText, customer.getMobile_num(), mLogger) ){
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
        };
        mLogger.debug("Sent first password reset SMS: "+customer.getMobile_num());
    }
}

