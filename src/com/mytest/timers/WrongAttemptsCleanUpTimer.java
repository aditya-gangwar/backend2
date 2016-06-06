package com.mytest.timers;

import com.backendless.Backendless;
import com.backendless.logging.Logger;
import com.backendless.servercode.annotation.BackendlessTimer;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.CommonConstants;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * WrongAttemptsCleanUpTimer is a timer.
 * It is executed according to the schedule defined in Backendless Console. The
 * class becomes a timer by extending the TimerExtender class. The information
 * about the timer, its name, schedule, expiration date/time is configured in
 * the special annotation - BackendlessTimer. The annotation contains a JSON
 * object which describes all properties of the timer.
 */
@BackendlessTimer("{'startDate':1465326000000,'frequency':{'schedule':'daily','repeat':{'every':1}},'timername':'WrongAttemptsCleanUp'}")
public class WrongAttemptsCleanUpTimer extends com.backendless.servercode.extension.TimerExtender
{

    private Logger mLogger;
    private BackendOps mBackendOps;
    private SimpleDateFormat mSdfOnlyDateBackend;
    private Date mToday;

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        // delete all wrong attempts of time older than midnight
        deleteOldWrongAttempts();
    }

    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.MerchantServices");
        mBackendOps = new BackendOps(mLogger);

        mToday = new Date();
        mSdfOnlyDateBackend = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_BACKEND, CommonConstants.DATE_LOCALE);
        mSdfOnlyDateBackend.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    private int deleteOldWrongAttempts() {
        mLogger.debug( "In deleteOldWrongAttempts: ");
        /*
        * curl
        * -H application-id:application-id-value-from-console
        * -H secret-key:secret-key-value-from-console
        * -X DELETE
        * -v https://api.backendless.com/v1/data/bulk/Person?where=workDays%3D0
        */
        // https://api.backendless.com/v1/data/bulk/WrongAttempts?where=created%20%3C%201465237800000

        try {
            String whereClause = URLEncoder.encode(buildWrongAteemptsWhereClause(), "UTF-8").replaceAll("\\+", "%20");
            mLogger.debug( "Delete wrong attempts where clause: "+whereClause);

            UriBuilder builder = UriBuilder
                    .fromPath("https://api.backendless.com/v1/data/bulk")
                    .path("/{wrongAttemptsTable}")
                    .queryParam("where", whereClause);
            URI uri = builder.build("WrongAttempts");
            mLogger.debug( "WrongAttempts delete URL: "+uri.toString());
            URL url = new URL(uri.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("application-id", BackendConstants.APP_ID);
            conn.setRequestProperty("secret-key", BackendConstants.SECRET_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            /*
            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();*/

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                mLogger.error("Error HTTP response: "+conn.getResponseCode());
                return -1;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            int recordsUpdated = 0;
            String output;
            while ((output = br.readLine()) != null) {
                mLogger.debug("Output from server: "+output);
                recordsUpdated = Integer.parseInt(output.replaceAll("\\p{C}", "?"));
            }

            conn.disconnect();
            mLogger.debug("Updated delete status of wrong attempts: "+recordsUpdated);
            return recordsUpdated;

        } catch (Exception e) {
            mLogger.error("Exception in deleteOldWrongAttempts: "+e.toString());
            return -1;
        }
    }

    private String buildWrongAteemptsWhereClause() {
        String today = mSdfOnlyDateBackend.format(mToday);
        // all txns older than today midnight - the timer runs 12:30 AM each day
        return "created < '"+today+"'";
    }
}