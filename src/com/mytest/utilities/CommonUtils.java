package com.mytest.utilities;

import com.mytest.AppConstants;

import java.util.Random;

/**
 * Created by adgangwa on 22-05-2016.
 */
public class CommonUtils {

    public static String getHalfVisibleId(String userId) {
        // build half visible userid : XXXXX91535
        StringBuilder halfVisibleUserid = new StringBuilder();
        int halflen = userId.length() / 2;
        for(int i=0; i<halflen; i++) {
            halfVisibleUserid.append("X");
        }
        halfVisibleUserid.append(userId.substring(halflen));
        return halfVisibleUserid.toString();
    }

    public static String generateMerchantPassword() {
        // random alphanumeric string
        Random random = new Random();
        char[] id = new char[AppConstants.PASSWORD_LEN];
        for (int i = 0; i < AppConstants.PASSWORD_LEN; i++) {
            id[i] = AppConstants.pwdChars[random.nextInt(AppConstants.pwdChars.length)];
        }
        return new String(id);
    }

    public static String generateCustomerPIN() {
        // random numeric string
        Random random = new Random();
        char[] id = new char[AppConstants.PIN_LEN];
        for (int i = 0; i < AppConstants.PIN_LEN; i++) {
            id[i] = AppConstants.pinAndOtpChars[random.nextInt(AppConstants.pinAndOtpChars.length)];
        }
        return new String(id);
    }

    public static String generateOTP() {
        // random numeric string
        Random random = new Random();
        char[] id = new char[AppConstants.OTP_LEN];
        for (int i = 0; i < AppConstants.OTP_LEN; i++) {
            id[i] = AppConstants.pinAndOtpChars[random.nextInt(AppConstants.pinAndOtpChars.length)];
        }
        return new String(id);
    }

}
