package com.myecash.events.persistence_service;

import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.myecash.constants.BackendConstants;
import com.myecash.constants.BackendResponseCodes;
import com.myecash.database.Merchants;
import com.myecash.utilities.CommonUtils;
import com.myecash.utilities.MyLogger;

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