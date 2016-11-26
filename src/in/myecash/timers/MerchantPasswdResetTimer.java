package in.myecash.timers;

import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.annotation.BackendlessTimer;
import in.myecash.common.MyGlobalSettings;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

import in.myecash.common.database.*;
import in.myecash.common.constants.*;
import in.myecash.constants.*;

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
/*
 * Timer duration should be 1/6th of the configured merchant passwd reset cool off mins value
 * So, if cool off mins is 60, then timer should run every 10 mins
 */
@BackendlessTimer("{'startDate':1464294360000,'frequency':{'schedule':'custom','repeat':{'every':600}},'timername':'MerchantPasswdReset'}")
public class MerchantPasswdResetTimer extends com.backendless.servercode.extension.TimerExtender
{
    private MyLogger mLogger = new MyLogger("services.MerchantPasswdResetTimer");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "MerchantPasswdResetTimer";
        mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(DbConstants.USER_TYPE_MERCHANT);

        try {
            mLogger.debug("In MerchantPasswdResetTimer execute");
            mLogger.debug("Before: " + HeadersManager.getInstance().getHeaders().toString());

            // Fetch all 'pending' merchant password reset operations
            ArrayList<MerchantOps> ops = BackendOps.fetchMerchantOps(mchntPwdResetWhereClause());
            if (ops != null) {
                mLogger.debug("Fetched password reset ops: " + ops.size());
                mEdr[BackendConstants.EDR_API_PARAMS_IDX] = ops.size()+BackendConstants.BACKEND_EDR_SUB_DELIMETER;

                // first lock all to be processed objects
                // this is to avoid any chances of clash with the next run of this timer
                /*
                ArrayList<Integer> lockFailedOps = null;
                for (int i = 0; i < ops.size(); i++) {
                    ops.get(i).setOp_status(DbConstantsBackend.USER_OP_STATUS_LOCKED);
                    try {
                        BackendOps.saveMerchantOp(ops.get(i));
                        mLogger.debug("Locked passwd reset op for: " + ops.get(i).getMerchant_id());
                        //lockedOps.add(ops.get(i));
                    } catch(Exception e) {
                        // ignore exception
                        mLogger.error("Exception while locking merchant passwd reset record: "+e.toString(),e);
                        if(lockFailedOps==null) {
                            lockFailedOps = new ArrayList<>(ops.size());
                            mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.BACKEND_ERROR_PWD_RESET_LOCK_FAILED;
                        }
                        lockFailedOps.add(i);
                    }
                }
                // remove failed to lock objects from the list
                if(lockFailedOps!=null && lockFailedOps.size() > 0) {
                    for (Integer i:lockFailedOps) {
                        ops.remove(i.intValue());
                    }
                }

                // process locked objects
                mLogger.debug("Locked password reset ops: " + ops.size());*/
                for (MerchantOps op:ops) {
                    handlePasswdReset(op);
                    mLogger.debug("Processed passwd reset op for: " + op.getMerchant_id());
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    private void handlePasswdReset(MerchantOps op) {
        try {
            // fetch user with the given id with related merchant object
            BackendlessUser user = BackendOps.fetchUser(op.getMerchant_id(), DbConstants.USER_TYPE_MERCHANT, false);
            Merchants merchant = (Merchants) user.getProperty("merchant");
            // check admin status
            BackendUtils.checkMerchantStatus(merchant, mEdr, mLogger);

            // generate password
            String passwd = BackendUtils.generateTempPassword();

            // update user account for the password
            user.setPassword(passwd);
            BackendOps.updateUser(user);
            mLogger.debug("Updated merchant for password reset: " + merchant.getAuto_id());

            // Send SMS through HTTP
            String smsText = SmsHelper.buildPwdResetSMS(op.getMerchant_id(), passwd);
            // set Retry flag ON - however raise exception so as next resetTimer in queue dont try
            if (!SmsHelper.sendSMS(smsText, merchant.getMobile_num(), mEdr, mLogger, true)) {
                throw new BackendlessException(String.valueOf(ErrorCodes.SEND_SMS_FAILED), "");
            }
            mLogger.debug("Sent password reset SMS: " + merchant.getAuto_id());

            // Change merchant op status
            op.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
            BackendOps.saveMerchantOp(op);

        } catch(Exception e) {
            // ignore exception - mark op as failed
            mLogger.error("Exception in handlePasswdReset: "+e.toString(),e);
            mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.IGNORED_ERROR_MCHNT_PASSWD_RESET_FAILED;

            try {
                op.setOp_status(DbConstantsBackend.USER_OP_STATUS_ERROR);
                BackendOps.saveMerchantOp(op);
            } catch(Exception ex) {
                // ignore
                mLogger.error("Exception in handlePasswdReset: Rollback Failed: "+e.toString(),ex);
                mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
            }
        }
    }

    private String mchntPwdResetWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        whereClause.append("op_code = '").append(DbConstants.OP_RESET_PASSWD).append("'");
        whereClause.append(" AND op_status = '").append(DbConstantsBackend.USER_OP_STATUS_PENDING).append("'");

        // Records between last (cool off mins - timer duration) to (cool off mins)
        // So, if 'cool off mins = 60' and thus timer duration being 1/6th of it is 10 mins
        // and say if timer runs at 2:00 PM, then this run should process all requests
        // created between (2:00 PM - 60) to (2:00 PM - (60+10))
        // i.e. between 1:00 PM - 1:10 PM
        // accordingly, next timer run at 2:10 shall pick records created between 1:10 PM and 1:20 PM
        // Thus, no two consecutive runs will pick same records and thus never clash.

        long now = new Date().getTime();
        long startTime = now - MyGlobalSettings.getMchntPasswdResetMins();
        long endTime = startTime + MyGlobalSettings.MERCHANT_PASSWORD_RESET_TIMER_INTERVAL;

        whereClause.append(" AND createTime >= ").append(startTime);
        whereClause.append(" AND createTime < ").append(endTime);

        mLogger.debug("whereClasue: "+whereClause.toString());
        return whereClause.toString();
    }

}

    /*
    private String buildWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        // for particular merchant
        whereClause.append("op_code = '").append(DbConstantsBackend.OP_RESET_PASSWD).append("'");
        whereClause.append(" AND op_status = '").append(DbConstantsBackend.USER_OP_STATUS_PENDING).append("'");
        // older than configured cool off period
        long time = (new Date().getTime()) - (MyGlobalSettings.MERCHANT_PASSWORD_RESET_COOL_OFF_MINS * 60 * 1000);

        whereClause.append(" AND created < ").append(time);

        mLogger.debug("where clause: "+whereClause.toString());
        return whereClause.toString();
    }*/

