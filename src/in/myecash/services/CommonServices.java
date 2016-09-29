package in.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import in.myecash.constants.BackendConstants;
import in.myecash.database.InternalUser;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.CommonUtils;
import in.myecash.utilities.MyLogger;
import in.myecash.common.database.*;
import in.myecash.common.constants.*;

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
        CommonUtils.initTableToClassMappings();
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
            BackendlessUser user = BackendOps.loginUser(userId,oldPasswd);
            mEdr[BackendConstants.EDR_USER_ID_IDX] = (String)user.getProperty("user_id");
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = ((Integer)user.getProperty("user_type")).toString();

            // Find mobile number
            String mobileNum = null;
            switch((Integer)user.getProperty("user_type")) {
                case DbConstants.USER_TYPE_MERCHANT:
                    mLogger.debug("Usertype is Merchant");
                    BackendOps.loadMerchant(user);
                    Merchants merchant = (Merchants)user.getProperty("merchant");
                    mLogger.setProperties(merchant.getAuto_id(), DbConstants.USER_TYPE_MERCHANT, merchant.getDebugLogs());

                    mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();
                    mobileNum = merchant.getMobile_num();
                    break;

                case DbConstants.USER_TYPE_CUSTOMER:
                    mLogger.debug("Usertype is Customer");
                    BackendOps.loadCustomer(user);
                    Customers customer = (Customers)user.getProperty("customer");
                    mLogger.setProperties(customer.getPrivate_id(), DbConstants.USER_TYPE_CUSTOMER, customer.getDebugLogs());

                    mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getPrivate_id();
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
                if( SmsHelper.sendSMS(smsText, mobileNum, mLogger) ){
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
                } else {
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                };
            } else {
                //TODO: raise alarm
                mLogger.error("In changePassword: mobile number is null");
                mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.BACKEND_ERROR_MOBILE_NUM_NA;
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            BackendOps.logoutUser();

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public Merchants getMerchant(String merchantId) {
        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getMerchant";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantId;

        try {
            mLogger.debug("In getMerchant");

            // Send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediatly after
            Object userObj = CommonUtils.fetchCurrentUser(InvocationContext.getUserId(), null, mEdr, mLogger, true);
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
            CommonUtils.handleException(e, false, mLogger, mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime, mLogger, mEdr);
        }
    }

    public Customers getCustomer(String custId) {
        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getCustomer";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = custId;

        try {
            mLogger.debug("In getCustomer");

            // Send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediatly after
            Object userObj = CommonUtils.fetchCurrentUser(InvocationContext.getUserId(), null, mEdr, mLogger, true);
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
                customer = BackendOps.getCustomer(custId, CommonUtils.getCustomerIdType(custId), true);
                mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getPrivate_id();
            } else {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return customer;
        } catch (Exception e) {
            CommonUtils.handleException(e, false, mLogger, mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime, mLogger, mEdr);
        }
    }

    /*
     * Private helper methods
     */
}
