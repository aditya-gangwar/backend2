package in.myecash.timers;

import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.annotation.BackendlessTimer;
import in.myecash.constants.*;
import in.myecash.database.CustomerOps;
import in.myecash.database.Customers;
import in.myecash.messaging.SmsConstants;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.CommonUtils;
import in.myecash.utilities.MyLogger;

import java.util.ArrayList;
import java.util.Date;

/**
 * CustomerPasswdResetTimer is a timer.
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
@BackendlessTimer("{'startDate':1464294360000,'frequency':{'schedule':'custom','repeat':{'every':600}},'timername':'CustomerPasswdReset'}")
public class CustomerPasswdResetTimer extends com.backendless.servercode.extension.TimerExtender
{
    private MyLogger mLogger = new MyLogger("services.CustomerPasswdResetTimer");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "CustomerPasswdResetTimer";

        try {
            mLogger.debug("In CustomerPasswdResetTimer execute");
            mLogger.debug("Before: " + HeadersManager.getInstance().getHeaders().toString());

            // Fetch all 'pending' merchant password reset operations
            ArrayList<CustomerOps> ops = BackendOps.fetchCustomerOps(custPwdResetWhereClause());
            if (ops != null) {
                mLogger.debug("Fetched password reset ops: " + ops.size());
                mEdr[BackendConstants.EDR_API_PARAMS_IDX] = ops.size()+BackendConstants.BACKEND_EDR_SUB_DELIMETER;

                for (CustomerOps op:ops) {
                    handlePasswdReset(op);
                    mLogger.debug("Processed passwd reset op for: " + op.getMobile_num());
                }
            }
        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    private void handlePasswdReset(CustomerOps op) {
        try {
            // fetch user with the given id with related merchant object
            BackendlessUser user = BackendOps.fetchUser(op.getMobile_num(), DbConstants.USER_TYPE_CUSTOMER, false);
            Customers customer = (Customers) user.getProperty("customer");
            // check admin status
            CommonUtils.checkCustomerStatus(customer, mLogger);

            // generate password
            String passwd = CommonUtils.generateTempPassword();

            // update user account for the password
            user.setPassword(passwd);
            BackendOps.updateUser(user);
            mLogger.debug("Updated customer for password reset: " + customer.getMobile_num());

            // Send SMS through HTTP
            String smsText = buildPwdResetSMS(op.getMobile_num(), passwd);
            if (!SmsHelper.sendSMS(smsText, customer.getMobile_num(), mLogger)) {
                mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
            }
            mLogger.debug("Sent password reset SMS: " + customer.getMobile_num());

            // Change merchant op status
            op.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
            BackendOps.saveCustomerOp(op);

        } catch(Exception e) {
            // ignore exception - mark op as failed
            mLogger.error("Exception in handlePasswdReset: "+e.toString(),e);
            op.setOp_status(DbConstantsBackend.USER_OP_STATUS_ERROR);
            BackendOps.saveCustomerOp(op);
        }
    }

    private String custPwdResetWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        whereClause.append("op_code = '").append(DbConstants.CUSTOMER_OP_RESET_PASSWORD).append("'");
        whereClause.append(" AND op_status = '").append(DbConstantsBackend.USER_OP_STATUS_PENDING).append("'");

        // Records between last (cool off mins - timer duration) to (cool off mins)
        // So, if 'cool off mins = 60' and thus timer duration being 1/6th of it is 10 mins
        // and say if timer runs at 2:00 PM, then this run should process all requests
        // created between (2:00 PM - 60) to (2:00 PM - (60+10))
        // i.e. between 1:00 PM - 1:10 PM
        // accordingly, next timer run at 2:10 shall pick records created between 1:10 PM and 1:20 PM
        // Thus, no two consecutive runs will pick same records and thus never clash.

        long now = new Date().getTime();
        long startTime = now - GlobalSettingsConstants.CUSTOMER_PASSWORD_RESET_COOL_OFF_MINS;
        long endTime = startTime + GlobalSettingsConstants.CUSTOMER_PASSWORD_RESET_TIMER_INTERVAL;

        whereClause.append(" AND created >= ").append(startTime);
        whereClause.append(" AND created < ").append(endTime);

        mLogger.debug("whereClause: "+whereClause.toString());
        return whereClause.toString();
    }

    private String buildPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_PASSWD,CommonUtils.getHalfVisibleId(userId),password);
    }
}
