package com.myecash.events.persistence_service;

/**
 * Created by adgangwa on 27-08-2016.
 */

import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.myecash.constants.BackendConstants;
import com.myecash.constants.BackendResponseCodes;
import com.myecash.database.MerchantDevice;
import com.myecash.utilities.CommonUtils;
import com.myecash.utilities.MyLogger;

/**
 * MerchantDeviceTableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "MerchantDevice" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */

@Asset( "MerchantDevice" )
public class MerchantDeviceTableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<MerchantDevice>
{

    @Override
    public void beforeUpdate(RunnerContext context, MerchantDevice merchantdevice ) throws Exception
    {
        MyLogger mLogger = new MyLogger("events.TransactionEventHandler");
        String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

        // update not allowed from app - return exception
        // beforeUpdate is not called, if update is done from server code
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "txn-beforeUpdate";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantdevice.getMerchant_id();
        CommonUtils.writeEdr(mLogger, mEdr);
        throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "");
    }

}