package com.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.servercode.IBackendlessService;
import com.myecash.constants.BackendConstants;
import com.myecash.constants.DbConstants;
import com.myecash.database.Agents;
import com.myecash.database.Merchants;
import com.myecash.messaging.SmsHelper;
import com.myecash.utilities.BackendOps;
import com.myecash.utilities.CommonUtils;
import com.myecash.utilities.MyLogger;

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
                    mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();
                    mobileNum = merchant.getMobile_num();
                    break;

                case DbConstants.USER_TYPE_AGENT:
                    mLogger.debug("Usertype is Agent");
                    BackendOps.loadAgent(user);
                    Agents agent = (Agents)user.getProperty("agent");
                    mEdr[BackendConstants.EDR_AGENT_ID_IDX] = agent.getId();
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


    /*
     * Private helper methods
     */
}
