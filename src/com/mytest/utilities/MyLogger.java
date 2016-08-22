package com.mytest.utilities;

import com.backendless.Backendless;
import com.backendless.logging.Logger;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.CommonConstants;

/**
 * Created by adgangwa on 19-08-2016.
 */
public class MyLogger {
    
    private Logger mLogger;
    private StringBuilder sb;

    public MyLogger(Logger logger) {
        mLogger = logger;
        if(BackendConstants.DEBUG_MODE) {
            sb = new StringBuilder();
        }
    }

    public void debug(String msg) {
        mLogger.debug(msg);
        if(BackendConstants.DEBUG_MODE) {
            System.out.println(msg);
            sb.append("\n"+msg);
        }
    }

    public void error(String msg) {
        mLogger.error(msg);
        if(BackendConstants.DEBUG_MODE) {
            msg = "Error: "+msg;
            System.out.println(msg);
            sb.append("\n"+msg);
        }
    }

    public void fatal(String msg) {
        mLogger.fatal(msg);
        if(BackendConstants.DEBUG_MODE) {
            msg = "Fatal: "+msg;
            System.out.println(msg);
            sb.append("\n"+msg);
        }
    }

    public void warn(String msg) {
        mLogger.warn(msg);
        if(BackendConstants.DEBUG_MODE) {
            msg = "Warning: "+msg;
            System.out.println(msg);
            sb.append("\n"+msg);
        }
    }

    public void flush() {
        Backendless.Logging.flush();
        try {
            if (BackendConstants.DEBUG_MODE) {
                String filePath = CommonConstants.MERCHANT_LOGGING_ROOT_DIR + "myLog.txt";
                Backendless.Files.saveFile(filePath, sb.toString().getBytes("UTF-8"), true);
            }
        } catch(Exception e) {
            //ignore exception
        }
    }
}
