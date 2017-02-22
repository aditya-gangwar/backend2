package in.myecash.timers;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.FilePermission;
import com.backendless.HeadersManager;
import com.backendless.servercode.annotation.BackendlessTimer;
import in.myecash.common.CommonUtils;
import in.myecash.common.DateUtil;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.CommonConstants;
import in.myecash.constants.BackendConstants;
import in.myecash.constants.DbConstantsBackend;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

import java.util.Date;

/**
 * EndOfDayTimer is a timer.
 * It is executed according to the schedule defined in Backendless Console. The
 * class becomes a timer by extending the TimerExtender class. The information
 * about the timer, its name, schedule, expiration date/time is configured in
 * the special annotation - BackendlessTimer. The annotation contains a JSON
 * object which describes all properties of the timer.
 */

// TODO: Daily EBS backup should be completed this
// TODO: No connections from Merchant/Customers should be allowed, when this is running.

// 19:30 in GMT = 01:00 in IST
@BackendlessTimer("{'startDate':1486582200000,'frequency':{'schedule':'daily','repeat':{'every':19:30}},'timername':'EndOfDay'}")
public class EndOfDayTimer extends com.backendless.servercode.extension.TimerExtender
{
    private MyLogger mLogger = new MyLogger("services.CustomerPasswdPinResetTimer");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "EndOfDayTimer";

        try {
            // First login
            BackendlessUser user = BackendOps.loginUser("autoAdmin", "autoAdmin@123");
            BackendUtils.printCtxtInfo(mLogger, null);
            String userToken = HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY);

            // Create Txn Image directory for today
            createTxnImgDir();

            // Delete wrong attempts rows
            delWrongAttempts(userToken);

            // Delete wrong attempts rows
            delMchntOps(userToken);

            // Delete wrong attempts rows
            delCustOps(userToken);

            // Delete archived Txns
            delArchivedTxns(userToken);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendOps.logoutUser();
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    private void delCustOps(String userToken) {
        // Build where clause
        DateUtil now = new DateUtil(CommonConstants.TIMEZONE);
        now.removeDays(MyGlobalSettings.getOpsKeepDays()+BackendConstants.RECORDS_DEL_BUFFER_DAYS);
        Date delDate = now.toMidnight().getTime();

        String whereClause = "created < '"+delDate.getTime()+"'";
        try {
            int recDel = BackendOps.doBulkRequest(DbConstantsBackend.CUST_OPS_TABLE_NAME, whereClause, "DELETE",
                    null, userToken, mLogger);
            mLogger.debug("delCustOps: "+recDel);

        } catch (Exception e) {
            // log and ignore
            BackendUtils.handleException(e,false,mLogger,mEdr);
        }
    }

    private void delMchntOps(String userToken) {
        // Build where clause
        DateUtil now = new DateUtil(CommonConstants.TIMEZONE);
        now.removeDays(MyGlobalSettings.getOpsKeepDays()+BackendConstants.RECORDS_DEL_BUFFER_DAYS);
        Date delDate = now.toMidnight().getTime();

        String whereClause = "created < '"+delDate.getTime()+"'";
        try {
            int recDel = BackendOps.doBulkRequest(DbConstantsBackend.MCHNT_OPS_TABLE_NAME, whereClause, "DELETE",
                    null, userToken, mLogger);
            mLogger.debug("delMchntOps: "+recDel);

        } catch (Exception e) {
            // log and ignore
            BackendUtils.handleException(e,false,mLogger,mEdr);
        }
    }

    private void delWrongAttempts(String userToken) {
        // Build where clause
        DateUtil now = new DateUtil(CommonConstants.TIMEZONE);
        now.removeDays(BackendConstants.WRONG_ATTEMPTS_DEL_DAYS);
        Date delDate = now.toMidnight().getTime();

        String whereClause = "created < '"+delDate.getTime()+"'";
        try {
            int recDel = BackendOps.doBulkRequest(DbConstantsBackend.WRONG_ATTEMPTS_TABLE_NAME, whereClause, "DELETE",
                    null, userToken, mLogger);
            mLogger.debug("delWrongAttempts: "+recDel);

        } catch (Exception e) {
            // log and ignore
            BackendUtils.handleException(e,false,mLogger,mEdr);
        }
    }

    private void delArchivedTxns(String userToken) {

        String whereClause = buildTxnWhereClause();
        for(int i=1; i<= DbConstantsBackend.TRANSACTION_TABLE_CNT; i++) {

            String tableName = DbConstantsBackend.TRANSACTION_TABLE_NAME+String.valueOf(i);
            try {
                int recDel = BackendOps.doBulkRequest(tableName, whereClause, "DELETE",
                        null, userToken, mLogger);
                mLogger.debug("Deleted archived Txns: "+tableName+" "+recDel);

            } catch (Exception e) {
                // log and ignore
                BackendUtils.handleException(e,false,mLogger,mEdr);
            }
        }

    }

    private String buildTxnWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        DateUtil now = new DateUtil(CommonConstants.TIMEZONE);
        // 3 days as buffer
        now.removeDays(MyGlobalSettings.getTxnsIntableKeepDays()+BackendConstants.RECORDS_DEL_BUFFER_DAYS);
        Date txnInDbFrom = now.toMidnight().getTime();

        whereClause.append("archived=").append("true");
        whereClause.append(" AND create_time < '").append(txnInDbFrom.getTime()).append("'");

        return whereClause.toString();
    }

    private void createTxnImgDir() {
        try {
            DateUtil now = new DateUtil(CommonConstants.TIMEZONE);
            String fileDir = CommonUtils.getTxnImgDir(now.getTime());
            String filePath = fileDir + CommonConstants.FILE_PATH_SEPERATOR + BackendConstants.DUMMY_FILENAME;
            Backendless.Files.saveFile(filePath, BackendConstants.DUMMY_DATA.getBytes("UTF-8"), true);
            // Give write access to Merchants to this directory
            //FilePermission.WRITE.grantForRole("Merchant", fileDir);
            mLogger.debug("Saved dummy txn image file: " + filePath);

        } catch (Exception e) {
            // log and ignore
            BackendUtils.handleException(e,false,mLogger,mEdr);
        }
    }

}
