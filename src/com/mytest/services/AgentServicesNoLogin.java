package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.DbConstants;
import com.mytest.database.Agents;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class AgentServicesNoLogin implements IBackendlessService {

    private Logger mLogger;

    /*
     * Public methods: Backend REST APIs
     */
    public void resetPassword(String userId, String secret1) {
        initCommon();
        try {
            mLogger.debug("In resetPassword: " + userId);

            // fetch user with the given id with related agent object
            BackendlessUser user = BackendOps.fetchUser(userId, DbConstants.USER_TYPE_AGENT);
            Agents agent = (Agents) user.getProperty("agent");

            // check admin status
            CommonUtils.checkAgentStatus(agent);

            // check for 'extra verification'
            String dob = agent.getDob();
            if (dob == null || !dob.equalsIgnoreCase(secret1)) {
                CommonUtils.handleWrongAttempt(agent, DbConstants.USER_TYPE_AGENT, DbConstants.ATTEMPT_TYPE_PASSWORD_RESET);
                throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
            }

            handlePasswdResetImmediate(user, agent);
            mLogger.debug("Processed passwd reset op for: " + agent.getMobile_num());

            //Backendless.Logging.flush();

        } catch (Exception e) {
            mLogger.error("Exception in resetPassword: "+e.toString());
            Backendless.Logging.flush();
            throw e;
        }
    }


    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.AgentServicesNoLogin");
        CommonUtils.initTableToClassMappings();
    }

    private void handlePasswdResetImmediate(BackendlessUser user, Agents agent) {
        // generate password
        String passwd = CommonUtils.generateTempPassword();
        // update user account for the password
        user.setPassword(passwd);
        user = BackendOps.updateUser(user);
        mLogger.debug("Updated agent for password reset: "+agent.getMobile_num());

        // Send SMS through HTTP
        String smsText = buildAgentPwdResetSMS(agent.getMobile_num(), passwd);
        if( !SmsHelper.sendSMS(smsText, agent.getMobile_num()) )
        {
            throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
        }
        mLogger.debug("Sent first password reset SMS: "+agent.getMobile_num());
    }

    private String buildAgentPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_PASSWD,userId,password);
    }
}
