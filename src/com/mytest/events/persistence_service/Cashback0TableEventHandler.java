package com.mytest.events.persistence_service;

import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.property.ObjectProperty;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.backendless.servercode.annotation.Async;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.database.Cashback;
import com.mytest.utilities.CommonUtils;
import com.mytest.utilities.MyLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Cashback0TableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "Cashback0" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */

@Asset( "Cashback0" )
public class Cashback0TableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<Cashback>
{
    @Override
    public void beforeUpdate( RunnerContext context, Cashback cashback ) throws Exception
    {
        MyLogger mLogger = new MyLogger("events.CashbackEventHandler");
        String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

        // update not allowed from app - return exception
        // beforeUpdate is not called, if update is done from server code
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "cashback-beforeUpdate";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = cashback.getRowid();
        CommonUtils.writeEdr(mLogger, mEdr);
        throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "");
    }
}