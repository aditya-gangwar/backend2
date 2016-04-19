package com.mytest.events.user_service;

import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.property.UserProperty;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Async;

import java.util.HashMap;
        
/**
* GenericUserEventHandler handles the User Service events.
* The event handlers are the individual methods implemented in the class.
* The "before" and "after" prefix determines if the handler is executed before
* or after the default handling logic provided by Backendless.
* The part after the prefix identifies the actual event.
* For example, the "beforeLogin" method is the "Login" event handler and will
* be called before Backendless applies the default login logic. The event
* handling pipeline looks like this:

* Client Request ---> Before Handler ---> Default Logic ---> After Handler --->
* Return Response
*/
public class GenericUserEventHandler extends com.backendless.servercode.extension.UserExtender
{
    
  @Async
  @Override
  public void afterRegister( RunnerContext context, HashMap userValue, ExecutionResult<HashMap> result ) throws Exception
  {
      // send password in SMS, if registration is successful
      if(result.getException()==null) {
          Integer userType = (Integer) userValue.get("user_type");
          String userId = (String) userValue.get("user_id");
          String mobileNum = (String) userValue.get("mobile_no");
          String passwd = (String) userValue.get("password");

          System.out.println("User registration was successful: "+userType+","+userId+","+mobileNum+","+passwd);

      } else {
          System.out.println("User registration was not successful: "+result.getException().toString());
      }
  }
    
}
        