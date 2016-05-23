
package com.mytest;

import com.backendless.Backendless;
import com.backendless.servercode.IBackendlessBootstrap;
import com.mytest.database.*;


public class Bootstrap implements IBackendlessBootstrap
{
            
  @Override
  public void onStart()
  {
    Backendless.Data.mapTableToClass("CustomerCards", CustomerCards.class);
    Backendless.Data.mapTableToClass("Customers", Customers.class);
    Backendless.Data.mapTableToClass("Merchants", Merchants.class);
    Backendless.Data.mapTableToClass("CustomerOps", CustomerOps.class);
    Backendless.Data.mapTableToClass("AllOtp", AllOtp.class);

    Backendless.Data.mapTableToClass( "Transaction0", Transaction.class );
    Backendless.Data.mapTableToClass( "Cashback0", Cashback.class );

    Backendless.Data.mapTableToClass( "Transaction1", Transaction.class );
    Backendless.Data.mapTableToClass( "Cashback1", Cashback.class );

    /*
    Backendless.Data.mapTableToClass( "Transaction2", Transaction.class );
    Backendless.Data.mapTableToClass( "Cashback2", Cashback.class );

    Backendless.Data.mapTableToClass( "Transaction3", Transaction.class );
    Backendless.Data.mapTableToClass( "Cashback3", Cashback.class );

    Backendless.Data.mapTableToClass( "Transaction4", Transaction.class );
    Backendless.Data.mapTableToClass( "Cashback4", Cashback.class );
    */
  }
    
  @Override
  public void onStop()
  {
    // add your code here
  }
    
}
        