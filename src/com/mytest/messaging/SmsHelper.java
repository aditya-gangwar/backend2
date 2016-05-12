package com.mytest.messaging;

import com.backendless.Backendless;
import com.mytest.models.AppConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import com.backendless.logging.Logger;


/**
 * Created by adgangwa on 12-05-2016.
 */
public class SmsHelper {
    private static Logger mLogger;

    public static void sendSMS(String message, String recipient) throws IOException {

        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.messaging.SmsHelper");

        //http://txtguru.in/imobile/api.php?username=aditya_gang&password=50375135&source=UPDATE&dmobile=918800191535&message=TEST+SMS+GATEWAY
        String requestUrl  = SmsConstants.SMSGW_BASE_URL +
                "username=" + URLEncoder.encode(SmsConstants.SMSGW_USERNAME, SmsConstants.SMSGW_URL_ENCODING) +
                "&password=" + URLEncoder.encode(SmsConstants.SMSGW_PASSWORD, SmsConstants.SMSGW_URL_ENCODING) +
                "&source=" + URLEncoder.encode(SmsConstants.SMSGW_SENDER_ID, SmsConstants.SMSGW_URL_ENCODING) +
                "&dmobile=" + URLEncoder.encode(SmsConstants.COUNTRY_CODE+recipient, SmsConstants.SMSGW_URL_ENCODING) +
                "&message=" + URLEncoder.encode(message, SmsConstants.SMSGW_URL_ENCODING);

        URL url = new URL(requestUrl);
        mLogger.debug("SMS URL: "+url.toString());

        /*
        HttpURLConnection uc = (HttpURLConnection)url.openConnection();

        try {
            if (uc.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(uc.getResponseMessage() + ": with " + requestUrl);
            }
            mLogger.debug(uc.getResponseMessage());

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (uc.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {
                mLogger.debug("SMS server response: "+output);
            }
        } finally {
            uc.disconnect();
        }*/
    }
}
