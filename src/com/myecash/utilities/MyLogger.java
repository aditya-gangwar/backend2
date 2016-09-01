package com.myecash.utilities;

import com.backendless.Backendless;
import com.backendless.logging.Logger;
import com.myecash.constants.BackendConstants;
import com.myecash.constants.CommonConstants;
import com.myecash.constants.DbConstants;

/**
 * Created by adgangwa on 19-08-2016.
 */
public class MyLogger {
    
    private Logger mLogger;
    private Logger mEdrLogger;
    private StringBuilder mSb;

    private String logId;
    private String userId="";
    private String userType="";
    private boolean debugLogs = false;

    public MyLogger(String loggerName) {
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);

        mLogger = Backendless.Logging.getLogger(loggerName);
        mEdrLogger = Backendless.Logging.getLogger("utilities.edr");
        logId = CommonUtils.generateLogId();
        if(BackendConstants.DEBUG_MODE) {
            mSb = new StringBuilder();
        }
    }

    public void setProperties(String userId, int userType, boolean argDebugLogs) {
        this.userId = userId;
        this.userType = DbConstants.userTypeDesc[userType];

        if(BackendConstants.FORCED_DEBUG_LOG_MERCHANTS && userType==DbConstants.USER_TYPE_MERCHANT) {
            this.debugLogs = true;
        } else if (BackendConstants.FORCED_DEBUG_LOG_AGENTS && userType==DbConstants.USER_TYPE_AGENT) {
            this.debugLogs = true;
        } else {
            this.debugLogs = argDebugLogs;
        }
    }

    public void debug(String msg) {
        msg = logId+" | "+userId+" | "+userType+" | "+msg;
        if(debugLogs) {
            mLogger.debug(msg);
        }

        if(BackendConstants.DEBUG_MODE) {
            msg = "Debug | "+msg;
            //System.out.println(msg);
            mSb.append("\n").append(msg);
        }
    }

    public void error(String msg) {
        msg = logId+" | "+userId+" | "+userType+" | "+msg;
        mLogger.error(msg);
        if(BackendConstants.DEBUG_MODE) {
            msg = "Error | "+msg;
            //System.out.println(msg);
            mSb.append("\n").append(msg);
        }
    }

    public void fatal(String msg) {
        msg = logId+" | "+userId+" | "+userType+" | "+msg;
        mLogger.fatal(msg);
        if(BackendConstants.DEBUG_MODE) {
            msg = "Fatal | "+msg;
            //System.out.println(msg);
            mSb.append("\n").append(msg);
        }
    }

    public void warn(String msg) {
        msg = logId+" | "+userId+" | "+userType+" | "+msg;
        mLogger.warn(msg);
        if(BackendConstants.DEBUG_MODE) {
            msg = "Warning | "+msg;
            //System.out.println(msg);
            mSb.append("\n").append(msg);
        }
    }

    public void edr(String[] edrData) {
        StringBuilder sbEdr = new StringBuilder(BackendConstants.BACKEND_EDR_MAX_SIZE);
        for (String s: edrData)
        {
            if(s==null) {
                sbEdr.append(BackendConstants.BACKEND_EDR_DELIMETER);
            } else {
                sbEdr.append(s).append(BackendConstants.BACKEND_EDR_DELIMETER);
            }
        }

        String edr = logId+" | EDR | "+sbEdr.toString();
        if( edrData[BackendConstants.EDR_RESULT_IDX].equals(BackendConstants.BACKEND_EDR_RESULT_NOK) ||
                (edrData[BackendConstants.EDR_SPECIAL_FLAG_IDX]!=null && !edrData[BackendConstants.EDR_SPECIAL_FLAG_IDX].isEmpty()) ||
                (edrData[BackendConstants.EDR_IGNORED_ERROR_IDX]!=null && !edrData[BackendConstants.EDR_IGNORED_ERROR_IDX].isEmpty()) ) {
            mEdrLogger.error(edr);
        } else {
            mEdrLogger.info(edr);
        }
        if(BackendConstants.DEBUG_MODE) {
            mSb.append("\n").append(edr);
        }
    }

    public void flush() {
        //Backendless.Logging.flush();
        try {
            if (BackendConstants.DEBUG_MODE) {
                String filePath = CommonConstants.MERCHANT_LOGGING_ROOT_DIR + "myBackendLog.txt";
                Backendless.Files.saveFile(filePath, mSb.toString().getBytes("UTF-8"), true);
            }
        } catch(Exception e) {
            //ignore exception
        }
    }
}
