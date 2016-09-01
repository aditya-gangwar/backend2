package com.myecash.events.persistence_service;

import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.myecash.constants.BackendConstants;
import com.myecash.constants.BackendResponseCodes;
import com.myecash.database.Cashback;
import com.myecash.utilities.CommonUtils;
import com.myecash.utilities.MyLogger;

/**
 * Cashback1TableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "Cashback1" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */

@Asset( "Cashback1" )
public class Cashback1TableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<Cashback>
{
    @Override
    public void beforeUpdate(RunnerContext context, Cashback cashback ) throws Exception
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