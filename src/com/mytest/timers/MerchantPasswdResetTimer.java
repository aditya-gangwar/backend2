package com.mytest.timers;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.logging.Logger;
import com.backendless.servercode.annotation.BackendlessTimer;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.DbConstants;
import com.mytest.constants.GlobalSettingsConstants;
import com.mytest.database.MerchantOps;
import com.mytest.database.Merchants;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

import java.util.ArrayList;
import java.util.Date;

/**
 * MerchantPasswdResetTimer is a timer.
 * It is executed according to the schedule defined in Backendless Console. The
 * class becomes a timer by extending the TimerExtender class. The information
 * about the timer, its name, schedule, expiration date/time is configured in
 * the special annotation - BackendlessTimer. The annotation contains a JSON
 * object which describes all properties of the timer.
 */

@BackendlessTimer("{'startDate':1464294360000,'frequency':{'schedule':'custom','repeat':{'every':900}},'timername':'MerchantPasswdReset'}")
public class MerchantPasswdResetTimer extends com.backendless.servercode.extension.TimerExtender
{
    private Logger mLogger;

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        // Init logger
        initCommon();

        try {
            mLogger.debug("In MerchantPasswdResetTimer execute");
            mLogger.debug("Before: " + HeadersManager.getInstance().getHeaders().toString());

            // Fetch all 'pending' merchant password reset operations
            ArrayList<MerchantOps> ops = BackendOps.fetchMerchantOps(buildWhereClause());
            if (ops != null) {
                mLogger.info("Fetched password reset ops: " + ops.size());
                // first lock all to be processed objects
                // this is to avoid any chances of clash with the next run of this timer
                ArrayList<MerchantOps> lockedOps = new ArrayList<>();
                for (int i = 0; i < ops.size(); i++) {
                    ops.get(i).setOp_status(DbConstants.MERCHANT_OP_STATUS_LOCKED);
                    try {
                        BackendOps.saveMerchantOp(ops.get(i));
                        mLogger.debug("Locked passwd reset op for: " + ops.get(i).getMerchant_id());
                        lockedOps.add(ops.get(i));
                    } catch(Exception e) {
                        // ignore exception
                        mLogger.error("Exception while locking merchant passwd reset record: "+e.toString());
                    }
                }

                // process locked objects
                mLogger.info("Locked password reset ops: " + lockedOps.size());
                for (int k = 0; k < ops.size(); k++) {
                    handlePasswdReset(lockedOps.get(k));
                    mLogger.debug("Processed passwd reset op for: " + lockedOps.get(k).getMerchant_id());
                }
            }
        } catch (Exception e) {
            //TODO: raise alarm
            mLogger.error("Exception in MerchantPasswdResetTimer: "+e.toString());
            Backendless.Logging.flush();
            throw e;
        }
    }

    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.MerchantPasswdResetTimer");
        CommonUtils.initTableToClassMappings();
    }

    private void handlePasswdReset(MerchantOps op) {
        // fetch user with the given id with related merchant object
        BackendlessUser user = BackendOps.fetchUser(op.getMerchant_id(), DbConstants.USER_TYPE_MERCHANT);
        Merchants merchant = (Merchants) user.getProperty("merchant");
        // check admin status
        CommonUtils.checkMerchantStatus(merchant);

        // generate password
        String passwd = CommonUtils.generateTempPassword();
        mLogger.debug("Merchant Password: "+passwd);

        // update user account for the password
        user.setPassword(passwd);
        BackendOps.updateUser(user);
        mLogger.debug("Updated merchant for password reset: "+merchant.getAuto_id());

        // Send SMS through HTTP
        String smsText = buildPwdResetSMS(op.getMerchant_id(), passwd);
        if( !SmsHelper.sendSMS(smsText, merchant.getMobile_num()) )
        {
            throw CommonUtils.getException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
        }
        mLogger.debug("Sent password reset SMS: "+merchant.getAuto_id());

        // Change merchant op status
        op.setOp_status(DbConstants.MERCHANT_OP_STATUS_COMPLETE);
        BackendOps.saveMerchantOp(op);
    }

    private String buildWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        // for particular merchant
        whereClause.append("op_code = '").append(DbConstants.MERCHANT_OP_RESET_PASSWD).append("'");
        whereClause.append(" AND op_status = '").append(DbConstants.MERCHANT_OP_STATUS_PENDING).append("'");
        // older than configured cool off period
        long time = (new Date().getTime()) - (GlobalSettingsConstants.MERCHANT_PASSWORD_RESET_COOL_OFF_MINS * 60 * 1000);

        whereClause.append(" AND created < ").append(time);

        mLogger.debug("where clause: "+whereClause.toString());
        return whereClause.toString();
    }

    private String buildPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_PASSWD,CommonUtils.getHalfVisibleId(userId),password);
    }
}