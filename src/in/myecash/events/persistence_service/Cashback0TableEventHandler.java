package in.myecash.events.persistence_service;

import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Cashback;
import in.myecash.constants.BackendConstants;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

/**
 * Cashback0TableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "Cashback0" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */
/*
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
        BackendUtils.writeOpNotAllowedEdr(mLogger, mEdr);
        throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
    }
}*/