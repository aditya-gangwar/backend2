package in.myecash.utilities;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Customers;
import in.myecash.constants.BackendConstants;
import in.myecash.database.MycKeys;
import in.myecash.security.SaltedHashService;
import in.myecash.security.SimpleAES;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

/**
 * Created by adgangwa on 23-12-2016.
 */
public class SecurityHelper {

    private static final String KEYADMIN_LOGINID = "keyadmin";
    private static final String MEMBERCARD_KEY_COL_NAME = "memberCardKey";

    public static String generateCustPin(Customers customer, MyLogger logger) {
        String pin = generateCustomerPIN();
        logger.debug("Clear text PIN: "+pin);
        setCustPin(customer, pin, logger);
        return pin;
    }

    public static void setCustPin(Customers customer, String pin, MyLogger logger) {
        try {
            byte[] salt = SaltedHashService.generateSalt();
            byte[] pwd = SaltedHashService.getEncrypted(pin,salt);
            //logger.debug("Salt: "+new String(salt));
            //logger.debug("Encrypted PIN: "+new String(pwd));

            customer.setNamak(Base64.getEncoder().encodeToString(salt));
            customer.setTxn_pin(Base64.getEncoder().encodeToString(pwd));
            //logger.debug("Salt: "+customer.getNamak());
            //logger.debug("PIN: "+customer.getTxn_pin());

        } catch (Exception e) {
            logger.error("Exception while hashing customer pin: "+customer.getMobile_num(),e);
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Hashing of customer PIN failed: "+customer.getMobile_num());
        }
    }

    public static boolean verifyCustPin(Customers customer, String rcvdPin, MyLogger logger) {
        //logger.debug("In verifyCustPin");
        try {
            byte[] salt = Base64.getDecoder().decode(customer.getNamak());
            byte[] pin = Base64.getDecoder().decode(customer.getTxn_pin());
            //logger.debug("Salt: "+new String(salt));
            //logger.debug("Encrypted PIN: "+new String(pin));
            return SaltedHashService.authenticate(rcvdPin, pin, salt);

        } catch (Exception e) {
            logger.error("Exception while verifying pin: "+customer.getMobile_num(),e);
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Customer PIN verification failed: "+customer.getMobile_num());
        }
    }

    public static String getMemberCardKey(String keyadminPwd, MyLogger logger) {
        //logger.debug("In getMemberCardKey");

        // login using 'admin' user
        BackendOps.loginUser(KEYADMIN_LOGINID,keyadminPwd);

        Backendless.Data.mapTableToClass("MycKeys", MycKeys.class);
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause("name = '" + MEMBERCARD_KEY_COL_NAME +"'");

        BackendlessCollection<MycKeys> collection = Backendless.Data.of(MycKeys.class).find(dataQuery);
        if( collection.getTotalObjects() != 1) {
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Invalid number of member card key records: "+collection.getTotalObjects());
        }

        String key = (collection.getData().get(0)).getKey();

        BackendOps.logoutUser();
        //logger.debug("Exiting getMemberCardKey: "+key);
        return key;
    }

    public static String getCardIdFromNum(String cardNum, String key, MyLogger logger) {
        //logger.debug("In getEncryptedCardId: "+cardNum);

        try {
            // Card Number is less than 16 bytes - so using SimpleAES
            // Add fixed prefix - this will help to do some basic validation in the app
            String strEncoded = CommonConstants.MEMBER_CARD_ID_PREFIX + SimpleAES.encrypt(key.toCharArray(), cardNum);
            //logger.debug("Encoded CardNum: " + strEncoded);
            return strEncoded;

        } catch (Exception e) {
            logger.error("Exception while encrypting text: "+cardNum,e);
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Encryption of text failed: "+cardNum);
        }
    }

    public static String getCardNumFromId(String cardId, String key, MyLogger logger) {
        //logger.debug("In getDecrypted: "+cardId);

        try {
            // check and remove prefix
            String toDecode = null;
            if (cardId.startsWith(CommonConstants.MEMBER_CARD_ID_PREFIX)) {
                toDecode = cardId.substring(CommonConstants.MEMBER_CARD_ID_PREFIX.length());
            } else {
                logger.error("Invalid Member card id prefix: "+cardId);
                throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Invalid Member card id prefix: "+cardId);
            }

            String strDecoded = SimpleAES.decrypt(key.toCharArray(), toDecode);
            //logger.debug("Decoded CardNum: " + strDecoded);
            return strDecoded;

        } catch (Exception e) {
            logger.error("Exception while decrypting text: "+cardId,e);
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Encryption of text failed: "+cardId);
        }
    }

    private static String generateCustomerPIN() {
        // random numeric string
        Random random = new Random();
        char[] id = new char[CommonConstants.PIN_LEN];
        for (int i = 0; i < CommonConstants.PIN_LEN; i++) {
            id[i] = BackendConstants.pinAndOtpChars[random.nextInt(BackendConstants.pinAndOtpChars.length)];
        }
        return new String(id);
    }

}
