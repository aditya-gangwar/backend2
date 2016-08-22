package com.mytest.messaging;

import com.backendless.Backendless;
import com.mytest.constants.BackendConstants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import com.backendless.logging.Logger;
import com.mytest.utilities.MyLogger;


/**
 * Created by adgangwa on 12-05-2016.
 */
public class SmsHelper {
    private static MyLogger mLogger;


    public static boolean sendSMS(String message, String recipients) {

        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.mytest.messaging.SmsHelper");
        mLogger = new MyLogger(logger);

        mLogger.debug("SMS: " + message);
        if(BackendConstants.TESTING_MODE) {
            return true;
        }

        HttpURLConnection uc = null;
        try {

            StringBuffer requestUrl = new StringBuffer(SmsConstants.SMSGW_MSG91_BASE_URL)
                    .append("authkey=").append(URLEncoder.encode(SmsConstants.SMSGW_MSG91_AUTHKEY, SmsConstants.SMSGW_URL_ENCODING))
                    .append("&mobiles=").append(URLEncoder.encode(recipients, SmsConstants.SMSGW_URL_ENCODING))
                    .append("&message=").append(URLEncoder.encode(message, SmsConstants.SMSGW_URL_ENCODING))
                    .append("&sender=").append(URLEncoder.encode(SmsConstants.SMSGW_SENDER_ID, SmsConstants.SMSGW_URL_ENCODING))
                    .append("&route=").append(URLEncoder.encode(SmsConstants.SMSGW_MSG91_ROUTE_TXN, SmsConstants.SMSGW_URL_ENCODING))
                    .append("&country=").append(URLEncoder.encode(SmsConstants.COUNTRY_CODE, SmsConstants.SMSGW_URL_ENCODING));

            URL url = new URL(requestUrl.toString());
            mLogger.debug("SMS URL: " + url.toString());

            uc = (HttpURLConnection) url.openConnection();

            if (uc.getResponseCode() != HttpURLConnection.HTTP_OK) {
                mLogger.error("Failed to send SMS ("+message+") to "+recipients+". HTTP response: "+uc.getResponseCode());
                return false;
            }
            mLogger.debug(uc.getResponseMessage());

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (uc.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {
                mLogger.debug("SMS server response: " + output);
            }
        } catch (Exception e) {
            mLogger.error("Failed to send SMS ("+message+") to "+recipients);
            mLogger.error("Failed to send SMS:"+e.toString());
            return false;

        } finally {
            if (uc != null) {
                uc.disconnect();
            }
        }

        return true;
    }
}

//http://txtguru.in/imobile/api.php?username=aditya_gang&password=50375135&source=UPDATE&dmobile=918800191535&message=TEST+SMS+GATEWAY
            /*String requestUrl = SmsConstants.SMSGW_BASE_URL +
                    "username=" + URLEncoder.encode(SmsConstants.SMSGW_USERNAME, SmsConstants.SMSGW_URL_ENCODING) +
                    "&password=" + URLEncoder.encode(SmsConstants.SMSGW_PASSWORD, SmsConstants.SMSGW_URL_ENCODING) +
                    "&source=" + URLEncoder.encode(SmsConstants.SMSGW_SENDER_ID, SmsConstants.SMSGW_URL_ENCODING) +
                    "&dmobile=" + URLEncoder.encode(SmsConstants.COUNTRY_CODE + recipient, SmsConstants.SMSGW_URL_ENCODING) +
                    "&message=" + URLEncoder.encode(message, SmsConstants.SMSGW_URL_ENCODING);*/

            /*
            String requestUrl = SmsConstants.SMSGW_MSG91_BASE_URL +
                    "authkey=" + URLEncoder.encode(SmsConstants.SMSGW_MSG91_AUTHKEY, SmsConstants.SMSGW_URL_ENCODING) +
                    "&mobiles=" + URLEncoder.encode(recipients, SmsConstants.SMSGW_URL_ENCODING) +
                    "&message=" + URLEncoder.encode(message, SmsConstants.SMSGW_URL_ENCODING) +
                    "&sender=" + URLEncoder.encode(SmsConstants.SMSGW_SENDER_ID, SmsConstants.SMSGW_URL_ENCODING) +
                    "&route=" + URLEncoder.encode(SmsConstants.SMSGW_MSG91_ROUTE_TXN, SmsConstants.SMSGW_URL_ENCODING) +
                    "&country=" + URLEncoder.encode(SmsConstants.COUNTRY_CODE, SmsConstants.SMSGW_URL_ENCODING);*/

