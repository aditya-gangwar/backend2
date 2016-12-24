package in.myecash.utilities;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Cashback;
import in.myecash.database.AllOtp;
import in.myecash.database.MycKeys;
import in.myecash.security.SimpleAES;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Created by adgangwa on 23-12-2016.
 */
public class SecurityHelper {

    private static final String KEYADMIN_LOGINID = "keyadmin";
    private static final String MEMBERCARD_KEY_COL_NAME = "memberCardKey";
    //length of the AES key in bits (128, 192, or 256)
    private static final int KEY_LENGTH = 128;

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
            String strEncoded = SimpleAES.encrypt(KEY_LENGTH, key.toCharArray(), cardNum);
            //logger.debug("Encoded CardId: " + strEncoded);
            return strEncoded;

        } catch (Exception e) {
            logger.error("Exception while encrypting text: "+cardNum,e);
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Encryption of text failed: "+cardNum);
        }
    }

    public static String getCardNumFromId(String cardId, String key, MyLogger logger) {
        //logger.debug("In getDecrypted: "+cardId);

        try {
            String strDecoded = SimpleAES.decrypt(key.toCharArray(), cardId);
            //logger.debug("Decoded CardNum: " + strDecoded);
            return strDecoded;

        } catch (Exception e) {
            logger.error("Exception while decrypting text: "+cardId,e);
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Encryption of text failed: "+cardId);
        }
    }
}
