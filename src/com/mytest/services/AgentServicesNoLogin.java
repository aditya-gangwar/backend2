package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.DbConstants;
import com.mytest.constants.DbConstantsBackend;
import com.mytest.database.Agents;
import com.mytest.database.InternalUserDevice;
import com.mytest.database.Merchants;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;
import com.mytest.utilities.MyLogger;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class AgentServicesNoLogin implements IBackendlessService {

    private MyLogger mLogger;

    /*
     * Public methods: Backend REST APIs
     */
    public void setDeviceForLogin(String loginId, String deviceId) {
        initCommon();

        try {
            if (deviceId == null || deviceId.isEmpty()) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA, "");
            }

            mLogger.debug("In setDeviceForLogin: " + loginId + ": " + deviceId);
            //mLogger.debug(InvocationContext.asString());
            //mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

            // fetch internal user device data
            InternalUserDevice deviceData = BackendOps.fetchInternalUserDevice(loginId);
            if(deviceData==null) {
                deviceData = new InternalUserDevice();
                deviceData.setUserId(loginId);
                deviceData.setInstanceId("");
            }

            // Update device Info in merchant object
            deviceData.setTempId(deviceId);
            BackendOps.saveInternalUserDevice(deviceData);

        } catch(Exception e) {
            mLogger.error("Exception in setDeviceForLogin: "+e.toString());
            throw e;
        }
    }

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
                CommonUtils.handleWrongAttempt(userId, agent, DbConstants.USER_TYPE_AGENT, DbConstantsBackend.ATTEMPT_TYPE_PASSWORD_RESET);
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
            }

            handlePasswdResetImmediate(user, agent);
            mLogger.debug("Processed passwd reset op for: " + agent.getMobile_num());

            //mLogger.flush();

        } catch (Exception e) {
            mLogger.error("Exception in resetPassword: "+e.toString());
            mLogger.flush();
            throw e;
        }
    }


    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.mytest.services.AgentServicesNoLogin");
        mLogger = new MyLogger(logger);
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
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
        }
        mLogger.debug("Sent first password reset SMS: "+agent.getMobile_num());
    }

    private String buildAgentPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_PASSWD,userId,password);
    }
}
