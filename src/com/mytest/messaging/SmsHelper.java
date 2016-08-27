package com.mytest.messaging;

import com.backendless.Backendless;
import com.mytest.constants.BackendConstants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import com.backendless.logging.Logger;
import com.mytest.utilities.CommonUtils;
import com.mytest.utilities.MyLogger;


/**
 * Created by adgangwa on 12-05-2016.
 */
public class SmsHelper {
    private static MyLogger mLogger;

    public static boolean sendSMS(String message, String recipients) {

        mLogger = new MyLogger("messaging.SmsHelper");
        mLogger.debug("SMS: " + message);

        if(BackendConstants.TESTING_SKIP_SMS) {
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

    /*
     * Methods to build SMS texts
     */
    public static String buildNewCardSMS(String userId, String card_num) {
        return String.format(SmsConstants.SMS_CUSTOMER_NEW_CARD, card_num, CommonUtils.getHalfVisibleId(userId));
    }

    public static String buildCustMobileChangeSMS(String userId, String mobile_num) {
        return String.format(SmsConstants.SMS_MOBILE_CHANGE_CUSTOMER, CommonUtils.getHalfVisibleId(userId), CommonUtils.getHalfVisibleId(mobile_num));
    }

    public static String buildMobileChangeSMS(String userId, String mobile_num) {
        return String.format(SmsConstants.SMS_MOBILE_CHANGE_MERCHANT, CommonUtils.getHalfVisibleId(userId), CommonUtils.getHalfVisibleId(mobile_num));
    }

    public static String buildCustPwdResetSMS(String userId, String pin) {
        return String.format(SmsConstants.SMS_PIN, CommonUtils.getHalfVisibleId(userId),pin);
    }

    public static String buildFirstPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_FIRST_PASSWD,userId,password);
    }

    public static String buildUserIdSMS(String userId) {
        return String.format(SmsConstants.SMS_MERCHANT_ID,userId);
    }

    public static String buildPwdChangeSMS(String userId) {
        return String.format(SmsConstants.SMS_PASSWD_CHANGED, CommonUtils.getHalfVisibleId(userId));
    }

    public static String buildAgentPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_PASSWD,userId,password);
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

