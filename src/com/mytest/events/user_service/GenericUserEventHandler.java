package com.mytest.events.user_service;

import com.backendless.Backendless;
import com.backendless.commons.DeviceType;
import com.backendless.logging.Logger;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.InvocationContext;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Async;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.AppConstants;
import com.mytest.database.Customers;
import com.mytest.database.DbConstants;

import java.io.IOException;
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
  private Logger mLogger;

  @Async
  @Override
  public void afterRegister( RunnerContext context, HashMap userValue, ExecutionResult<HashMap> result ) throws Exception
  {
      Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
      mLogger = Backendless.Logging.getLogger("com.mytest.events.afterRegister");

      // send password in SMS, if registration is successful
      if(result.getException()==null) {
          String userId = (String) userValue.get("user_id");
          Integer userType = (Integer) userValue.get("user_type");

          if(userType == DbConstants.USER_TYPE_CUSTOMER) {
              Customers customer = (Customers) userValue.get("customer");
              if(customer != null) {
                  // Send sms to the customer with PIN
                  String pin = customer.getTxn_pin();
                  String smsText = String.format(SmsConstants.SMS_TEMPLATE_PIN,userId,pin);
                  // Send SMS through HTTP
                  mLogger.debug("SMS to send: "+smsText+" : "+smsText.length());
                  if( !SmsHelper.sendSMS(smsText,customer.getMobile_num()) ) {
                      // TODO: write to alarm table for retry later
                  }
              } else {
                  mLogger.error("Customer object is null: "+userId);
              }
          }
      } else {
          System.out.println("User registration was not successful: "+result.getException().toString());
      }
  }
    
}
        