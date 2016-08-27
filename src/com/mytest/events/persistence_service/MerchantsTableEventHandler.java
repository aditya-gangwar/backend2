package com.mytest.events.persistence_service;

import com.backendless.Backendless;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.backendless.servercode.annotation.Async;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.database.Merchants;
import com.mytest.utilities.CommonUtils;
import com.mytest.utilities.MyLogger;

import java.util.Map;

/**
 * MerchantsTableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "Merchants" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */

@Asset( "Merchants" )
public class MerchantsTableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<Merchants>
{
    private MyLogger mLogger = new MyLogger("events.MerchantsTableEventHandler");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    @Override
    public void beforeFirst( RunnerContext context ) throws Exception
    {
        // block for not-authenticated user
        // this event handler does not get called, if find done from servercode
        if(context.getUserToken()==null) {
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "Merchants-beforeFirst";
            CommonUtils.writeEdr(mLogger, mEdr);
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,"");
        }
    }

    @Override
    public void beforeFind( RunnerContext context, BackendlessDataQuery query ) throws Exception
    {
        // block for not-authenticated user
        // this event handler does not get called, if find done from servercode
        if(context.getUserToken()==null) {
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "Merchants-beforeFind";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = query.getWhereClause();
            CommonUtils.writeEdr(mLogger, mEdr);
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,"");
        }
    }

    @Override
    public void beforeLast( RunnerContext context ) throws Exception
    {
        // block for not-authenticated user
        // this event handler does not get called, if find done from servercode
        if(context.getUserToken()==null) {
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "Merchants-beforeFirst";
            CommonUtils.writeEdr(mLogger, mEdr);
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,"");
        }
    }
}