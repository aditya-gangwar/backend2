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
import com.mytest.database.Transaction;
import com.mytest.utilities.CommonUtils;
import com.mytest.utilities.MyLogger;

/**
* Transaction1TableEventHandler handles events for all entities. This is accomplished
* with the @Asset( "Transaction1" ) annotation.
* The methods in the class correspond to the events selected in Backendless
* Console.
*/
@Asset( "Transaction1" )
public class Transaction1TableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<Transaction>
{
    @Override
    public void beforeCreate( RunnerContext context, Transaction transaction) throws Exception
    {
        TxnTableEventHelper txnEventHelper = new TxnTableEventHelper();
        txnEventHelper.handleBeforeCreate(context, transaction);
    }
    
    @Async
    @Override
    public void afterCreate( RunnerContext context, Transaction transaction, ExecutionResult<Transaction> result ) throws Exception
    {
      TxnTableEventHelper txnEventHelper = new TxnTableEventHelper();
      txnEventHelper.handleAfterCreate(context, transaction, result);
    }

    @Override
    public void beforeUpdate( RunnerContext context, Transaction transaction ) throws Exception
    {
        MyLogger mLogger = new MyLogger("events.TransactionEventHandler");
        String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

        // update not allowed from app - return exception
        // beforeUpdate is not called, if update is done from server code
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "txn-beforeUpdate";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = transaction.getMerchant_id()+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                transaction.getCustomer_id()+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                transaction.getTrans_id();
        CommonUtils.writeEdr(mLogger, mEdr);
        throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "");
    }
    /*
    @Override
    public void beforeLast( RunnerContext context ) throws Exception
    {
        // block for not-authenticated user
        // this event handler does not get called, if find done from servercode
        if(context.getUserToken()==null) {
            Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
            Logger logger = Backendless.Logging.getLogger("com.mytest.services.Transaction1TableEventHandler");
            logger.error("In beforeLast: find attempt by not-authenticated user.");
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,"");
        }
    }

    @Override
    public void beforeFind( RunnerContext context, BackendlessDataQuery query ) throws Exception
    {
        // block for not-authenticated user
        // this event handler does not get called, if find done from servercode
        if(context.getUserToken()==null) {
            Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
            Logger logger = Backendless.Logging.getLogger("com.mytest.services.Transaction1TableEventHandler");
            logger.error("In beforeLast: find attempt by not-authenticated user.");
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,"");
        }
    }

    @Override
    public void beforeFirst( RunnerContext context ) throws Exception
    {
        // block for not-authenticated user
        // this event handler does not get called, if find done from servercode
        if(context.getUserToken()==null) {
            Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
            Logger logger = Backendless.Logging.getLogger("com.mytest.services.Transaction1TableEventHandler");
            logger.error("In beforeLast: find attempt by not-authenticated user.");
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,"");
        }
    }
    */
    
}