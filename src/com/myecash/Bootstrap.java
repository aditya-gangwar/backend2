
package com.myecash;

import com.backendless.servercode.IBackendlessBootstrap;
import com.myecash.utilities.CommonUtils;


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
        