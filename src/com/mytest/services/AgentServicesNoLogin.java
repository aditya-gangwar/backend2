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
    private BackendOps mBackendOps;

    /*
     * Public methods: Backend REST APIs
     */
    public void resetPassword(String userId, String secret1) {
        initCommon();
        mLogger.debug("In resetPassword: "+userId);

        // not required, as supposed to be called by user without logging in (forget password case)
        // this will ensure that backend operations are executed, as logged-in user who called this api using generated SDK
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        // fetch user with the given id with related agent object
        BackendlessUser user = mBackendOps.fetchUser(userId, DbConstants.USER_TYPE_AGENT);
        if(user==null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
        Agents agent = (Agents) user.getProperty("agent");

        // check admin status
        String status = CommonUtils.checkAgentStatus(agent);
        if(status != null) {
            //return status;
            CommonUtils.throwException(mLogger,status, "Agent account not active", false);
        }

        // check for 'extra verification'
        String dob = agent.getDob();
        if(dob==null || !dob.equalsIgnoreCase(secret1)) {

            if( CommonUtils.handleAgentWrongAttempt(mBackendOps, agent, DbConstants.ATTEMPT_TYPE_PASSWORD_RESET) ) {

                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD,
                        "Merchant wrong 'password reset' attempt limit reached"+agent.getMobile_num(), false);
            } else {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED,
                        "Merchant password reset verification failed"+agent.getMobile_num(), false);
            }
        }

        String error = handlePasswdResetImmediate(user, agent);
        if( error != null) {
            mLogger.error("Failed to process agent reset password operation: "+agent.getMobile_num()+", "+error);
            //return error;
            CommonUtils.throwException(mLogger,error, "Error in handlePasswdResetImmediate", false);
        } else {
            mLogger.debug("Processed passwd reset op for: "+agent.getMobile_num());
        }

        //Backendless.Logging.flush();
        //return BackendResponseCodes.BE_RESPONSE_NO_ERROR;
    }


    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.AgentServicesNoLogin");
        mBackendOps = new BackendOps(mLogger);
        CommonUtils.initTableToClassMappings();
    }

    private String handlePasswdResetImmediate(BackendlessUser user, Agents agent) {

        // generate password
        String passwd = CommonUtils.generateTempPassword();
        mLogger.debug("Agent Password: "+passwd);

        // update user account for the password
        user.setPassword(passwd);
        user = mBackendOps.updateUser(user);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }
        mLogger.debug("Updated agent for password reset: "+agent.getMobile_num());

        // Send SMS through HTTP
        String smsText = buildAgentPwdResetSMS(agent.getMobile_num(), passwd);
        if( !SmsHelper.sendSMS(smsText, agent.getMobile_num()) )
        {
            return BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED;
        }
        mLogger.debug("Sent first password reset SMS: "+agent.getMobile_num());

        return null;
    }

    private String buildAgentPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_PASSWD,userId,password);
    }


}
