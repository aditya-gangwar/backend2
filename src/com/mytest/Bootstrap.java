
package com.mytest;

import com.backendless.Backendless;
import com.backendless.servercode.IBackendlessBootstrap;
import com.mytest.models.Cashback;
import com.mytest.models.Cashback0;
import com.mytest.models.Transaction;
import com.mytest.models.Transaction0;


public class Bootstrap implements IBackendlessBootstrap
{
            
  @Override
  public void onStart()
  {
    Backendless.Data.mapTableToClass( "Transaction0", Transaction0.class );
    Backendless.Data.mapTableToClass( "Cashback0", Cashback0.class );
    // add your code here
  }
    
  @Override
  public void onStop()
  {
    // add your code here
  }
    
}
        