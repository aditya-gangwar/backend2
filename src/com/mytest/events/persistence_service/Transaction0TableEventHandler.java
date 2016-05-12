package com.mytest.events.persistence_service;

import com.backendless.Backendless;
import com.backendless.logging.Logger;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.backendless.servercode.annotation.Async;

import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.models.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
* Transaction0TableEventHandler handles events for all entities. This is accomplished
* with the @Asset( "Transaction0" ) annotation. 
* The methods in the class correspond to the events selected in Backendless
* Console.
*/
    
@Asset( "Transaction0" )
public class Transaction0TableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<Transaction>
{
  @Async
  @Override
  public void afterCreate( RunnerContext context, Transaction transaction, ExecutionResult<Transaction> result ) throws Exception
  {
      TxnTableEventHelper txnEventHelper = new TxnTableEventHelper();
      txnEventHelper.buildAndSendTxnSMS(transaction, result);
  }
}
        