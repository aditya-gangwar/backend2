package com.mytest.events.persistence_service;

import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.backendless.servercode.annotation.Async;

import com.mytest.database.*;

/**
* Transaction0TableEventHandler handles events for all entities. This is accomplished
* with the @Asset( "Transaction0" ) annotation. 
* The methods in the class correspond to the events selected in Backendless
* Console.
*/
@Asset( "Transaction0" )
public class Transaction0TableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<Transaction>
{
    @Override
    public void beforeCreate( RunnerContext context, Transaction transaction) throws Exception
    {
        TxnTableEventHelper txnEventHelper = new TxnTableEventHelper();
        txnEventHelper.handleBeforeCreate(transaction, "Cashback0");
    }

  @Async
  @Override
  public void afterCreate( RunnerContext context, Transaction transaction, ExecutionResult<Transaction> result ) throws Exception
  {
      TxnTableEventHelper txnEventHelper = new TxnTableEventHelper();
      txnEventHelper.handleAfterCreate(transaction, result);
  }
}