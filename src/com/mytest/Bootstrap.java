
package com.mytest;

import com.backendless.Backendless;
import com.backendless.servercode.IBackendlessBootstrap;
import com.mytest.database.*;
import com.mytest.utilities.CommonUtils;


public class Bootstrap implements IBackendlessBootstrap
{
            
  @Override
  public void onStart()
  {
    CommonUtils.initTableToClassMappings();
  }
    
  @Override
  public void onStop()
  {
    // add your code here
  }
    
}
        