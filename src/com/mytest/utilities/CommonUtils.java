package com.mytest.utilities;

import com.mytest.database.DbConstants;

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
        char[] id = new char[BackendConstants.PASSWORD_LEN];
        for (int i = 0; i < BackendConstants.PASSWORD_LEN; i++) {
            id[i] = BackendConstants.pwdChars[random.nextInt(BackendConstants.pwdChars.length)];
        }
        return new String(id);
    }

    public static String generateCustomerPIN() {
        // random numeric string
        Random random = new Random();
        char[] id = new char[CommonConstants.PIN_LEN];
        for (int i = 0; i < CommonConstants.PIN_LEN; i++) {
            id[i] = BackendConstants.pinAndOtpChars[random.nextInt(BackendConstants.pinAndOtpChars.length)];
        }
        return new String(id);
    }

    public static String generateOTP() {
        // random numeric string
        Random random = new Random();
        char[] id = new char[CommonConstants.OTP_LEN];
        for (int i = 0; i < CommonConstants.OTP_LEN; i++) {
            id[i] = BackendConstants.pinAndOtpChars[random.nextInt(BackendConstants.pinAndOtpChars.length)];
        }
        return new String(id);
    }

    public static String checkMerchantStatus(int status) {
        switch(status) {
            case DbConstants.USER_STATUS_DEFAULT:
            case DbConstants.USER_STATUS_DISABLED:
            case DbConstants.USER_STATUS_DISABLED_WRONG_PIN:
                return BackendResponseCodes.BL_MYERROR_CUSTOMER_ACC_DISABLED;
        }
        return null;
    }
}
