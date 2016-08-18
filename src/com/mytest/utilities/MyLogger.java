package com.mytest.utilities;

import com.backendless.logging.Logger;
import com.mytest.constants.BackendConstants;

/**
 * Created by adgangwa on 19-08-2016.
 */
public class MyLogger {
    
    private Logger mLogger;

    public MyLogger(Logger logger) {
        mLogger = logger;    
    }

    public void debug(String msg) {
        if(BackendConstants.DEBUG_MODE) {
            System.out.println(msg);
        } else {
            mLogger.debug(msg);
        }
    }

    public void error(String msg) {
        if(BackendConstants.DEBUG_MODE) {
            System.out.println("Error: "+msg);
        } else {
            mLogger.error(msg);
        }
    }

    public void fatal(String msg) {
        if(BackendConstants.DEBUG_MODE) {
            System.out.println("Fatal: "+msg);
        } else {
            mLogger.fatal(msg);
        }
    }

    public void warn(String msg) {
        if(BackendConstants.DEBUG_MODE) {
            System.out.println("Warning: "+msg);
        } else {
            mLogger.warn(msg);
        }
    }
}
