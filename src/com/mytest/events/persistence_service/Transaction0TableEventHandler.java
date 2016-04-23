package com.mytest.events.persistence_service;

import com.backendless.BackendlessCollection;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.property.ObjectProperty;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.backendless.servercode.annotation.Async;

import com.mytest.models.AppConstants;
import com.mytest.models.Cashback0;
import com.mytest.models.SmsTemplates;
import com.mytest.models.Transaction0;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
* Transaction0TableEventHandler handles events for all entities. This is accomplished
* with the @Asset( "Transaction0" ) annotation. 
* The methods in the class correspond to the events selected in Backendless
* Console.
*/
    
@Asset( "Transaction0" )
public class Transaction0TableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<Transaction0>
{
    int cl_debit;
    int cl_credit;
    int cb_debit;
    int cb_credit;
    int cl_balance;
    int cb_balance;
    String merchantName;
    String txnDate;
    
  @Async
  @Override
  public void afterCreate( RunnerContext context, Transaction0 transaction0, ExecutionResult<Transaction0> result ) throws Exception
  {
      System.out.println("Running server code");

      // If transaction creation successful send SMS to customer
      if(result.getException()==null) {
          String custMobile = transaction0.getCustomer_id();
          String txnId = transaction0.getTrans_id();
          System.out.println("Transaction successful: "+custMobile+", "+txnId);

          // Send SMS only in cases of 'redeem > INR 10' and 'add cash in account'
          cl_debit = transaction0.getCl_debit();
          cl_credit = transaction0.getCl_credit();
          cb_debit = transaction0.getCb_debit();
          cb_credit = transaction0.getCb_credit();

          if( cl_debit > AppConstants.SEND_TXN_SMS_MIN_AMOUNT ||
                  cb_debit > AppConstants.SEND_TXN_SMS_MIN_AMOUNT ||
                  cl_credit > 0) {
              Cashback0 cashback = transaction0.getCashback();
              if(cashback==null) {
                  System.out.println("Cashback object is not available.");
              } else {
                  merchantName = cashback.getMerchant_name().toUpperCase(Locale.ENGLISH);
                  cb_balance = cashback.getCb_credit() - cashback.getCb_debit();
                  cl_balance = cashback.getCl_credit() - cashback.getCl_debit();

                  SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_BACKEND, AppConstants.DATE_LOCALE);
                  sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                  txnDate = sdf.format(transaction0.getCreate_time());

                  // Build SMS
                  String smsText = buildSMS();
                  if(smsText==null) {
                      System.out.println("Failed to build transaction SMS");
                  } else {
                      // Send SMS through HTTP
                      System.out.println("SMS to send: "+smsText+" : "+smsText.length());
                      try {
                          sendSMS(smsText);
                      } catch (IOException e) {
                          System.out.println("Failed to send SMS: "+e.toString());
                      }
                  }
              }
          }
      }
  }

    private void sendSMS(String message) throws IOException {
        String recipient = "918800191535";
        String username = "aditya_gang";
        String password = "50375135";
        String originator = "UPDATE";

        //http://txtguru.in/imobile/api.php?username=aditya_gang&password=50375135&source=UPDATE&dmobile=918800191535&message=TEST+SMS+GATEWAY
        String requestUrl  = "https://www.txtguru.in/imobile/api.php?" +
                "username=" + URLEncoder.encode(username, "UTF-8") +
                "&password=" + URLEncoder.encode(password, "UTF-8") +
                "&source=" + URLEncoder.encode(originator, "UTF-8") +
                "&dmobile=" + URLEncoder.encode(recipient, "UTF-8") +
                "&message=" + URLEncoder.encode(message, "UTF-8");

        System.out.println("SMS URL: "+requestUrl);

        URL url = new URL(requestUrl);

        HttpURLConnection uc = (HttpURLConnection)url.openConnection();

        try {
            if (uc.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(uc.getResponseMessage() + ": with " + requestUrl);
            }
            System.out.println(uc.getResponseMessage());

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (uc.getInputStream())));

            String output;
            System.out.println("SMS server response ...");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
        } finally {
            uc.disconnect();
        }
    }

    private String buildSMS() {
        String sms=null;

        if(cl_debit>0 && cb_debit>0) {
            sms = String.format(SmsTemplates.SMS_TXN_DEBIT_CL_CB,merchantName,cl_debit,cb_debit,txnDate,cl_balance,cb_balance);
        } else if(cl_credit>AppConstants.SEND_TXN_SMS_MIN_AMOUNT && cb_debit>0) {
            sms = String.format(SmsTemplates.SMS_TXN_CREDIT_CL_DEBIT_CB,merchantName,cl_credit,cb_debit,txnDate,cl_balance,cb_balance);
        } else if(cl_credit>0) {
            sms = String.format(SmsTemplates.SMS_TXN_CREDIT_CL,merchantName,cl_credit,txnDate,cl_balance,cb_balance);
        } else if(cl_debit>AppConstants.SEND_TXN_SMS_MIN_AMOUNT) {
            sms = String.format(SmsTemplates.SMS_TXN_DEBIT_CL,merchantName,cl_debit,txnDate,cl_balance,cb_balance);
        } else if(cb_debit>AppConstants.SEND_TXN_SMS_MIN_AMOUNT) {
            sms = String.format(SmsTemplates.SMS_TXN_DEBIT_CB,merchantName,cb_debit,txnDate,cl_balance,cb_balance);
        }
        return sms;
    }
}
        