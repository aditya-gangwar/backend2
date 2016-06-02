package com.mytest.utilities;

import com.mytest.constants.*;
import com.mytest.database.CustomerCards;
import com.mytest.database.Customers;
import com.mytest.database.Merchants;

import java.util.Date;
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

    public static String checkMerchantStatus(Merchants merchant) {
        switch (merchant.getAdmin_status()) {
            case DbConstants.USER_STATUS_DEFAULT:
            case DbConstants.USER_STATUS_DISABLED:
                return BackendResponseCodes.BL_MYERROR_ACC_DISABLED;

            case DbConstants.USER_STATUS_LOCKED:
                // Check if temporary blocked duration is over
                Date blockedTime = merchant.getStatus_update_time();
                if (blockedTime != null && blockedTime.getTime() > 0) {
                    // check for temp blocking duration expiry
                    Date now = new Date();
                    long timeDiff = now.getTime() - blockedTime.getTime();
                    long allowedDuration = GlobalSettingsConstants.MERCHANT_ACCOUNT_BLOCKED_HOURS * 60 * 60 * 1000;

                    if (timeDiff > allowedDuration) {
                        // reset blocked time to null and update the status
                        // do not persist now in DB - will be done, if and when called in afterCreate()
                        // where customer object is any way saved - against the given customer operation
                        merchant.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
                        merchant.setStatus_update_time(new Date());
                    } else {
                        return BackendResponseCodes.BL_MYERROR_ACC_LOCKED;
                    }
                }
                break;
        }
        return null;
    }

    public static String checkCustomerStatus(Customers customer) {
        switch(customer.getAdmin_status()) {
            case DbConstants.USER_STATUS_DEFAULT:
            case DbConstants.USER_STATUS_DISABLED:
                return BackendResponseCodes.BL_MYERROR_ACC_DISABLED;

            case DbConstants.USER_STATUS_LOCKED:
                // Check if temporary blocked duration is over
                Date blockedTime = customer.getStatus_update_time();
                if (blockedTime != null && blockedTime.getTime() > 0) {
                    // check for temp blocking duration expiry
                    Date now = new Date();
                    long timeDiff = now.getTime() - blockedTime.getTime();
                    long allowedDuration = GlobalSettingsConstants.CUSTOMER_ACCOUNT_BLOCKED_HOURS * 60 * 60 * 1000;

                    if (timeDiff > allowedDuration) {
                        // reset blocked time to null and update the status
                        // do not persist now in DB - will be done, if and when called in afterCreate()
                        // where customer object is any way saved - against the given customer operation
                        customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
                        customer.setStatus_update_time(new Date());
                    } else {
                        return BackendResponseCodes.BL_MYERROR_ACC_LOCKED;
                    }
                }
                break;
        }
        return null;
    }

    public static String checkCustomerCardStatus(CustomerCards card) {
        switch(card.getStatus()) {
            case DbConstants.CUSTOMER_CARD_STATUS_WITH_MERCHANT:
                return null;
            case DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED:
                return BackendResponseCodes.BL_MYERROR_CARD_INUSE;
            case DbConstants.CUSTOMER_CARD_STATUS_BLOCKED:
                return BackendResponseCodes.BL_MYERROR_CARD_BLOCKED;
            case DbConstants.CUSTOMER_CARD_STATUS_REMOVED:
            case DbConstants.CUSTOMER_CARD_STATUS_NEW:
                return BackendResponseCodes.BL_MYERROR_WRONG_CARD;
        }
        return BackendResponseCodes.BL_MYERROR_WRONG_CARD;
    }
}
