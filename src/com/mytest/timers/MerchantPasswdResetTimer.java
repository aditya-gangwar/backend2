package com.mytest.timers;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.logging.Logger;
import com.backendless.servercode.annotation.BackendlessTimer;
import com.mytest.database.DbConstants;
import com.mytest.database.MerchantOps;
import com.mytest.database.Merchants;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.AppConstants;
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
@BackendlessTimer("{'startDate':1464294360000,'frequency':{'schedule':'custom','repeat':{'every':600}},'timername':'MerchantPasswdReset'}")
public class MerchantPasswdResetTimer extends com.backendless.servercode.extension.TimerExtender
{
    private static int MERCHANT_PASSWORD_RESET_COOL_OFF_MINS = 60;

    private Logger mLogger;
    private BackendOps mBackendOps;

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        // Init logger
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.CustomerOpsTableEventHandler");
        mBackendOps = new BackendOps(mLogger);

        // Fetch all 'pending' merchant password reset operations
        ArrayList<MerchantOps> ops = mBackendOps.fetchMerchantOps(buildWhereClause());
        if(ops!=null) {
            // first lock all to be processed objects
            // this is to avoid any chances of clash with the next run of this timer
            ArrayList<MerchantOps> lockedOps = new ArrayList<>();
            for (int i = 0; i < ops.size(); i++) {
                ops.get(i).setOp_status(DbConstants.MERCHANT_OP_STATUS_LOCKED);
                if(mBackendOps.saveMerchantOp(ops.get(i))!=null) {
                    lockedOps.add(ops.get(i));
                }
            }
            // process locked objects
            for (int k = 0; k < ops.size(); k++) {
                String error = handlePasswdReset(lockedOps.get(k));
                if( error != null) {
                    mLogger.error("Failed to process merchant reset password operation: "+lockedOps.get(k).getMerchant_id()+", "+error);
                }
            }
        }

    }

    private String handlePasswdReset(MerchantOps op) {
        // fetch user with the given id with related merchant object
        BackendlessUser user = mBackendOps.fetchUser(op.getMerchant_id(), DbConstants.USER_TYPE_MERCHANT);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        // TODO: verify merchant admin status

        // generate temporary password
        String passwd = CommonUtils.generateMerchantPassword();
        mLogger.debug("Merchant Password: "+passwd);

        // update user account for the password and time
        // set temp password and time
        user.setPassword(passwd);
        //merchant.setTemp_pswd_time(new Date());
        if(merchant.getPasswd_wrong_attempts() > 0) {
            merchant.setPasswd_wrong_attempts(0);
            user.setProperty("merchant",merchant);
        }

        user = mBackendOps.updateUser(user);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }

        // Send SMS through HTTP
        String smsText = buildPwdResetSMS(op.getMerchant_id(), passwd);
        if( !SmsHelper.sendSMS(smsText, merchant.getMobile_num()) )
        {
            return AppConstants.BL_MYERROR_SEND_SMS_FAILED;
            // dont care about return code - if failed, user can always do 'forget password' again
        }

        // Change merchant op status
        op.setOp_code(DbConstants.MERCHANT_OP_STATUS_COMPLETE);
        mBackendOps.saveMerchantOp(op);

        return null;
    }

    private String buildWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        // for particular merchant
        whereClause.append("op_code = '").append(DbConstants.MERCHANT_OP_RESET_PASSWD).append("'");
        whereClause.append(" AND op_status = '").append(DbConstants.MERCHANT_OP_STATUS_PENDING).append("'");
        // older than configured cool off period
        long time = (new Date().getTime()) - (MERCHANT_PASSWORD_RESET_COOL_OFF_MINS * 60 * 1000);
        // all txns older than today midnight - the timer runs 1 AM each day
        whereClause.append(" AND created < ").append(time);

        return whereClause.toString();
    }

    private String buildPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_TEMPLATE_PASSWD,CommonUtils.getHalfVisibleId(userId),password);
    }
}
