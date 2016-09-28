
package in.myecash;

import com.backendless.servercode.IBackendlessBootstrap;
import in.myecash.utilities.CommonUtils;


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
        