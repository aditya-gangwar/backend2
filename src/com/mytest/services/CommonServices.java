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
import com.mytest.utilities.MyLogger;

/**
 * Created by adgangwa on 19-07-2016.
 */

public class CommonServices implements IBackendlessService {

    private MyLogger mLogger;

    public void changePassword(String userId, String oldPasswd, String newPasswd) {
        initCommon();
        try {
            mLogger.debug("In changePassword: "+userId);

            // Login to verify the old password
            // Note: afterLogin event handler will not get called - so 'trusted device' check will not happen
            // As event handlers are not called - for API calls made from server code.
            // In normal situation, this is not an issue - as user can call 'change password' only after login
            // However, in hacked situation, 'trusted device' check wont happen - ignoring this for now.
            BackendlessUser user = BackendOps.loginUser(userId,oldPasswd);

            // Find mobile number
            String mobileNum = null;
            switch((Integer)user.getProperty("user_type")) {
                case DbConstants.USER_TYPE_MERCHANT:
                    mLogger.debug("Usertype is Merchant");
                    BackendOps.loadMerchant(user);
                    mobileNum = ((Merchants)user.getProperty("merchant")).getMobile_num();
                    break;

                case DbConstants.USER_TYPE_AGENT:
                    mLogger.debug("Usertype is Agent");
                    BackendOps.loadAgent(user);
                    mobileNum = ((Agents)user.getProperty("agent")).getMobile_num();
                    break;
            }
            mLogger.debug("changePassword: User mobile number: "+mobileNum);

            // Change password
            user.setPassword(newPasswd);
            user = BackendOps.updateUser(user);

            // Send SMS through HTTP
            if(mobileNum!=null) {
                String smsText = buildPwdChangeSMS(userId);
                if( !SmsHelper.sendSMS(smsText, mobileNum) )
                {
                    // ignore failure to send SMS
                    //TODO: add in alarms table
                }
            } else {
                //TODO: raise alarm
                mLogger.error("In changePassword: mobile number is null");
            }

            BackendOps.logoutUser();

        } catch (Exception e) {
            mLogger.error("Exception in changePassword: "+e.toString());
            mLogger.flush();
            throw e;
        }
    }


    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.mytest.services.CommonServices");
        mLogger = new MyLogger(logger);
        CommonUtils.initTableToClassMappings();
    }

    private String buildPwdChangeSMS(String userId) {
        return String.format(SmsConstants.SMS_PASSWD_CHANGED, CommonUtils.getHalfVisibleId(userId));
    }


}
