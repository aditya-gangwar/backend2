package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.DbConstants;
import com.mytest.database.Agents;
import com.mytest.database.Merchants;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

/**
 * Created by adgangwa on 19-07-2016.
 */

public class CommonServices implements IBackendlessService {

    private Logger mLogger;
    private BackendOps mBackendOps;

    public void changePassword(String userId, String oldPasswd, String newPasswd) {
        initCommon();
        try {
            mLogger.debug("In changePassword: "+userId);

            //mLogger.debug("Before: "+ InvocationContext.asString());
            //mLogger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            // we are anyways gonna login next, so dont need this
            //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

            // Login to verify the old password
            // Note: afterLogin event handler will not get called - so 'trusted device' check will not happen
            // As event handlers are not called - for API calls made from server code.
            // In normal situation, this is not an issue - as user can call 'change password' only after login
            // However, in hacked situation, 'trusted device' check wont happen - ignoring this for now.
            BackendlessUser user = mBackendOps.loginUser(userId,oldPasswd);
            if(user==null) {
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            }

            // Find mobile number
            String mobileNum = null;
            switch((Integer)user.getProperty("user_type")) {
                case DbConstants.USER_TYPE_MERCHANT:
                    mLogger.debug("Usertype is Merchant");
                    Merchants merchant = mBackendOps.loadMerchant(user);
                    if(merchant==null) {
                        //TODO: raise alarm
                        CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
                    } else {
                        mobileNum = merchant.getMobile_num();
                    }
                    break;

                case DbConstants.USER_TYPE_AGENT:
                    mLogger.debug("Usertype is Agent");
                    Agents agent = mBackendOps.loadAgent(user);
                    if(agent==null) {
                        //TODO: raise alarm
                        CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
                    } else {
                        mobileNum = agent.getMobile_num();
                    }
                    break;
            }
            mLogger.debug("User mobile num: "+mobileNum);

            // Change password
            user.setPassword(newPasswd);
            user = mBackendOps.updateUser(user);
            if(user==null) {
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            }

            // Send SMS through HTTP
            if(mobileNum!=null) {
                String smsText = buildPwdChangeSMS(userId);
                if( !SmsHelper.sendSMS(smsText, mobileNum) )
                {
                    //TODO: add in alarms table
                    CommonUtils.throwException(mLogger, BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "Failed to send password reset SMS: "+userId, false);
                }
            } else {
                //TODO: raise alarm
                mLogger.error("In changePassword: mobile number is null");
            }

            mBackendOps.logoutUser();
        } catch (Exception e) {
            mLogger.error("Exception in changePassword: "+e.toString());
            Backendless.Logging.flush();
            throw e;
        }
    }


    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.CommonServices");
        mBackendOps = new BackendOps(mLogger);
        CommonUtils.initTableToClassMappings();
    }

    private String buildPwdChangeSMS(String userId) {
        return String.format(SmsConstants.SMS_PASSWD_CHANGED, CommonUtils.getHalfVisibleId(userId));
    }


}
