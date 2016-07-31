package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.UserService;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.DbConstants;
import com.mytest.database.Agents;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

import java.util.List;

/**
 * Created by adgangwa on 19-07-2016.
 */
public class AdminServices implements IBackendlessService {

    private Logger mLogger;
    private BackendOps mBackendOps;

    /*
     * Public methods: Backend REST APIs
     */
    public void registerAgent(String userId, String mobile, String name, String dob, String pwd) {
        initCommon();
        try {
            mLogger.debug("In registerAgent: "+userId+": "+mobile);

            mLogger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            BackendlessUser user = mBackendOps.loginUser("admin",pwd);
            if(user==null) {
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            }

            mLogger.debug("Before2: "+ HeadersManager.getInstance().getHeaders().toString());

            //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );
            //mLogger.debug("After: "+ HeadersManager.getInstance().getHeaders().toString());

            // Create agent object and register
            Agents agent = new Agents();
            agent.setId(userId);
            agent.setMobile_num(mobile);
            agent.setDob(dob);
            agent.setName(name);
            agent.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
            agent.setStatus_reason(DbConstants.ENABLED_ACTIVE);

            BackendlessUser agentUser = new BackendlessUser();
            agentUser.setProperty("user_id", userId);
            agentUser.setPassword(dob);
            agentUser.setProperty("user_type", DbConstants.USER_TYPE_AGENT);
            agentUser.setProperty("agent",agent);

            List<String> roles = Backendless.UserService.getUserRoles();
            mLogger.debug("Roles: "+roles.toString());

            agentUser = mBackendOps.registerUser(agentUser);
            if(agentUser == null) {
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            }
            mLogger.debug("Registration successful");
            //agent = (Agents) agentUser.getProperty("agent");

            // assign role
            if(!mBackendOps.assignRole(userId, BackendConstants.ROLE_AGENT)) {
                String errorCode = mBackendOps.mLastOpStatus;
                String errorMsg = mBackendOps.mLastOpErrorMsg;

                // TODO: add as 'Major' alarm - user to be removed later manually

                CommonUtils.throwException(mLogger,errorCode,errorMsg,false);
            }

            // Send sms to the customer with PIN
            String smsText = String.format(SmsConstants.SMS_REG_AGENT, userId);
            // Send SMS through HTTP
            if (!SmsHelper.sendSMS(smsText, mobile)) {
                // TODO: write to alarm table for retry later
            }

            mBackendOps.logoutUser();
        } catch (Exception e) {
            mLogger.error("Exception in registerAgent: "+e.toString());
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
        mLogger = Backendless.Logging.getLogger("com.mytest.services.AdminServices");
        mBackendOps = new BackendOps(mLogger);
        CommonUtils.initTableToClassMappings();
    }


}
