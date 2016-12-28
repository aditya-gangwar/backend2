package in.myecash.events.persistence_service;

import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.backendless.servercode.annotation.Async;
import in.myecash.common.database.Transaction;

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
        TxnProcessHelper txnEventHelper = new TxnProcessHelper();
        txnEventHelper.handleTxnCommit(context.getUserToken(), context.getUserId(), transaction, false, false);
        //txnEventHelper.handleTxnCommit(context, transaction, false, false);
    }

    @Async
    @Override
    public void afterCreate( RunnerContext context, Transaction transaction, ExecutionResult<Transaction> result ) throws Exception
    {
        TxnProcessHelper txnEventHelper = new TxnProcessHelper();
        txnEventHelper.handleAfterCreate(context, transaction, result);
    }
}