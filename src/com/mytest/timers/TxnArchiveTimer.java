package com.mytest.timers;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.backendless.servercode.annotation.BackendlessTimer;
import com.mytest.models.AppConstants;
import com.mytest.models.Transaction;

import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
* TxnArchiveTimer is a timer.
* It is executed according to the schedule defined in Backendless Console. The
* class becomes a timer by extending the TimerExtender class. The information
* about the timer, its name, schedule, expiration date/time is configured in
* the special annotation - BackendlessTimer. The annotation contains a JSON
* object which describes all properties of the timer.
*/
@BackendlessTimer("{'startDate':1460316600000,'frequency':{'schedule':'daily','repeat':{'every':1}},'timername':'TxnArchive'}")
public class TxnArchiveTimer extends com.backendless.servercode.extension.TimerExtender
{
    private static final String MERCHANT_ID_PREFIX = "0";
    //transid(10),time(20),merchantid(6),merchantname(50),customermobile(10),customerprivid(5),amts(6x4=24),rate(2) = 115 chars = 115x2 = 250 bytes
    private static final int CSV_RECORD_MAX_CHARS = 250;

    private List<Transaction> mLastFetchTransactions;
    private BackendlessUser mLastFetchMerchant;
    // map of 'txn date' -> 'csv file url'
    HashMap<String,String> mCsvFiles = new HashMap<>();
    // map of 'txn date' -> 'csv string of all txns in dat date'
    HashMap<String,StringBuilder> mCsvDataMap = new HashMap<>();
    // All required date formatters
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(AppConstants.DATE_FORMAT_WITH_TIME, AppConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfOnlyDateBackend = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_BACKEND, AppConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfOnlyDateFilename = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_FILENAME, AppConstants.DATE_LOCALE);
    private Date mToday = new Date();

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        long startTime = System.currentTimeMillis();
        System.out.println( "Running custom code");

        mSdfDateWithTime.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        mSdfOnlyDateBackend.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        mSdfOnlyDateFilename.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        // Fetch next not processed merchant
        mLastFetchMerchant = null;
        mLastFetchMerchant = fetchNextMerchant();

        if(mLastFetchMerchant!=null) {
            String merchantId = (String)mLastFetchMerchant.getProperty("user_id");
            System.out.println( "Fetched merchant id: "+merchantId);

            String txnTableName = (String)mLastFetchMerchant.getProperty("transaction_table");
            Backendless.Data.mapTableToClass(txnTableName, Transaction.class);

            mLastFetchTransactions = null;
            int errorCode = fetchTransactionsSync(buildTxnWhereClause(merchantId));
            if(errorCode == AppConstants.NO_ERROR) {
                if(mLastFetchTransactions.size() > 0) {
                    // convert txns into csv strings
                    buildCsvString();

                    // TODO: encrypt CSV string

                    // store CSV file in merchant directory
                    if( createCsvFiles(merchantId) ) {
                        int recordsUpdated = updateTxnArchiveStatus(txnTableName,merchantId,true);
                        if(recordsUpdated == -1) {
                            // rollback
                            deleteCsvFiles();
                        } else if(recordsUpdated != mLastFetchTransactions.size()) {
                            System.out.println( "Count of txns updated for status does not match.");
                            // rollback
                            updateTxnArchiveStatus(txnTableName,merchantId,false);
                            deleteCsvFiles();
                        } else {
                            if(updateMerchantArchiveTime()) {
                                System.out.println("Archived records successfully: "+recordsUpdated);
                            } else {
                                // rollback
                                updateTxnArchiveStatus(txnTableName,merchantId,false);
                                deleteCsvFiles();
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println( "No more merchants available: "+MERCHANT_ID_PREFIX);
        }

        long endTime = System.currentTimeMillis();
        System.out.println( "Exiting custom code: "+(endTime-startTime));
    }

    private int updateTxnArchiveStatus(String txnTableName, String merchantId, boolean status) {
        /*
        * curl
        * -H application-id:09667F8B-98A7-E6B9-FFEB-B2B6EE831A00
        * -H secret-key:95971CBD-BADD-C61D-FF32-559664AE4F00
        * -H Content-Type:application/json
        * -X PUT
        * -d "{\"archived\":true}"
        * -v https://api.backendless.com/v1/data/bulk/<tablename>?where=workDays%3E10
        */
        try {
            String whereClause = URLEncoder.encode(buildTxnWhereClause(merchantId), "UTF-8").replaceAll("\\+", "%20");
            System.out.println( "Where clause: "+whereClause);

            UriBuilder builder = UriBuilder
                    .fromPath("https://api.backendless.com/v1/data/bulk")
                    .path("/{txntable}")
                    .queryParam("where", whereClause);
            URI uri = builder.build(txnTableName);
            System.out.println( "Txn update URL: "+uri.toString());
            URL url = new URL(uri.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("application-id", "09667F8B-98A7-E6B9-FFEB-B2B6EE831A00");
            conn.setRequestProperty("secret-key", "95971CBD-BADD-C61D-FF32-559664AE4F00");
            conn.setRequestProperty("Content-Type", "application/json");

            String input;
            if(status) {
                input = "{\"archived\":true}";
            } else {
                input = "{\"archived\":false}";
            }

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.out.println("Error HTTP response: "+conn.getResponseCode());
                //throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
                return -1;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            int recordsUpdated = 0;
            String output;
            System.out.println("Output from Server ....");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
                recordsUpdated = Integer.parseInt(output.replaceAll("\\p{C}", "?"));
            }

            conn.disconnect();
            System.out.println("Updated archive status of txns: "+recordsUpdated);
            return recordsUpdated;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private boolean updateMerchantArchiveTime() {
        // update archive date in merchant record
        // set to today 00:00 hrs
        mLastFetchMerchant.setProperty("last_archive",mSdfOnlyDateBackend.format(mToday));

        // Save merchant object
        try {
            mLastFetchMerchant = Backendless.UserService.update( mLastFetchMerchant );
        } catch(BackendlessException e) {
            System.out.println("Merchant update failed: " + e.toString());
            return false;
        }
        return true;
    }

    private boolean createCsvFiles(String merchantId) {
        //SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_FILENAME, AppConstants.DATE_LOCALE);
        /*
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mToday);
        calendar.add(Calendar.DATE, -1);
        String yesterday = mSdfOnlyDateFilename.format(calendar.getTime()); */

        for (Map.Entry<String, StringBuilder> entry : mCsvDataMap.entrySet()) {
            String txnDate = entry.getKey();
            StringBuilder sb = entry.getValue();

            // File name: txns_<merchant_id>_<ddMMMyy>.csv
            String fileName = AppConstants.MERCHANT_TXN_FILE_PREFIX + merchantId + "_" + txnDate + AppConstants.CSV_FILE_EXT;
            String filepath = getMerchantTxnDir(merchantId) + fileName;
            System.out.println("Complete path: " + filepath);

            try {
                String fileUrl = Backendless.Files.saveFile(filepath, sb.toString().getBytes("UTF-8"), true);
                System.out.println("CSV file uploaded successfully at :" + fileUrl);
                mCsvFiles.put(txnDate,filepath);
            } catch (Exception e) {
                System.out.println("CSV file upload failed: " + e.toString());
                // For multiple days, single failure will be considered failure for all days
                return false;
            }
        }
        return true;
    }

    private boolean deleteCsvFiles() {
        try {
            for (String filePath : mCsvFiles.values()) {
                Backendless.Files.remove(filePath);
                System.out.println("Deleted CSV file: " + filePath);
            }
        } catch(Exception e) {
            System.out.println("CSV file delete failed: " + e.toString());
            return false;
        }
        return true;
    }

    private String getMerchantTxnDir(String merchantId) {
        // merchant directory: merchants/<first 2 chars of merchant id>/<next 2 chars of merchant id>/<merchant id>/txns
        return AppConstants.MERCHANT_ROOT_DIR +
                merchantId.substring(0,2) + AppConstants.FILE_PATH_SEPERATOR +
                merchantId.substring(2,4) + AppConstants.FILE_PATH_SEPERATOR +
                merchantId + AppConstants.FILE_PATH_SEPERATOR +
                AppConstants.MERCHANT_TXN_DIR;
    }

    // Assumes mLastFetchTransactions can contain txns prior to yesterday too
    // Creates HashMap with one entry for each day
    private void buildCsvString() {
        int size = mLastFetchTransactions.size();

        for(int i=0; i<size; i++) {
            Transaction txn = mLastFetchTransactions.get(i);
            Date txnDate = txn.getCreate_time();

            // get 'date' for this transaction - in the filename format - to be used as key in map
            String txnDateStr = mSdfOnlyDateFilename.format(txnDate);
            StringBuilder sb = mCsvDataMap.get(txnDateStr);
            if(sb==null) {
                System.out.println("buildCsvString, new day : "+txnDateStr);
                sb = new StringBuilder(CSV_RECORD_MAX_CHARS*size);
                // new file - write first line as header
                sb.append("trans_id,time,merchant_id,merchant_name,customer_id,cust_private_id,total_billed,cb_billed,cl_debit,cl_credit,cb_debit,cb_credit,cb_percent");
                sb.append(AppConstants.CSV_NEWLINE);
                mCsvDataMap.put(txnDateStr,sb);
            }

            // trans_id,time,merchant_id,merchant_name,customer_id,cust_private_id,
            // total_billed,cb_billed,cl_debit,cl_credit,cb_debit,cb_credit,cb_percent\n
            sb.append(txn.getTrans_id()).append(AppConstants.CSV_DELIMETER);
            //SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT_WITH_TIME, AppConstants.DATE_LOCALE);
            sb.append(mSdfDateWithTime.format(txnDate)).append(AppConstants.CSV_DELIMETER);
            sb.append(mLastFetchMerchant.getProperty("user_id")).append(AppConstants.CSV_DELIMETER);
            sb.append(mLastFetchMerchant.getProperty("name")).append(AppConstants.CSV_DELIMETER);
            sb.append(txn.getCustomer_id()).append(AppConstants.CSV_DELIMETER);
            sb.append(txn.getCust_private_id()).append(AppConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getTotal_billed())).append(AppConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_billed())).append(AppConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCl_debit())).append(AppConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCl_credit())).append(AppConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_debit())).append(AppConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_credit())).append(AppConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_percent())).append(AppConstants.CSV_DELIMETER);
            sb.append(AppConstants.CSV_NEWLINE);
        }
    }

    private BackendlessUser fetchNextMerchant() {
        BackendlessDataQuery query = new BackendlessDataQuery();
        // fetch only 1 record sorted by 'created'
        QueryOptions queryOptions = new QueryOptions("created");
        query.setQueryOptions(queryOptions);
        query.setPageSize(1);

        query.setWhereClause(buildMerchantWhereClause());

        BackendlessCollection<BackendlessUser> users = Backendless.Data.of( BackendlessUser.class ).find(query);
        int cnt = users.getTotalObjects();
        if( cnt == 0) {
            // No unprocessed merchant left with this prefix
            System.out.println("No merchant available for archive: "+MERCHANT_ID_PREFIX);
        } else {
            // just return the first merchant
            // in the next timer run, this merchant will not be processed
            // as it will have the archive time set to now
            return users.getData().get(0);
        }
        return null;
    }

    private String buildMerchantWhereClause() {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("user_type = ").append(AppConstants.USER_TYPE_MERCHANT);
        //whereClause.append("AND user_id LIKE '%").append(MERCHANT_ID_PREFIX).append("'");

        //SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_BACKEND, AppConstants.DATE_LOCALE);
        String today = mSdfOnlyDateBackend.format(mToday);
        // all txns older than today 00:00 hrs - the timer runs 1 AM each day
        whereClause.append(" AND (last_archive < '").append(today).append("'").append(" OR last_archive is null)");

        System.out.println("Merchant where clause: " + whereClause.toString());
        return whereClause.toString();
    }

    private String buildTxnWhereClause(String merchantId) {
        StringBuilder whereClause = new StringBuilder();

        // for particular merchant
        whereClause.append("cashback.merchant.user_id = '").append(merchantId).append("'");

        //SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_BACKEND, AppConstants.DATE_LOCALE);
        String today = mSdfOnlyDateBackend.format(mToday);
        // all txns older than today midnight - the timer runs 1 AM each day
        whereClause.append(" AND create_time < '").append(today).append("'");
        whereClause.append(" AND archived=").append("false");

        return whereClause.toString();
    }

    public int fetchTransactionsSync(String whereClause) {
        System.out.println("In fetchTransactionsSync: " + whereClause);
        // init values
        int errorCode = AppConstants.NO_ERROR;
        mLastFetchTransactions = null;

        // fetch cashback object from DB
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            // sort by time created
            QueryOptions queryOptions = new QueryOptions("create_time");
            dataQuery.setQueryOptions(queryOptions);
            dataQuery.setPageSize(AppConstants.dbQueryMaxPageSize);
            if(whereClause!=null) {
                dataQuery.setWhereClause(whereClause);
            }

            BackendlessCollection<Transaction> collection = Backendless.Data.of(Transaction.class).find(dataQuery);

            int size = collection.getTotalObjects();
            System.out.println("Got transactions from DB: " + size+", "+collection.getData().size());
            if (size == 0) {
                errorCode = AppConstants.NO_DATA_FOUND;
            } else {
                mLastFetchTransactions = collection.getData();
                System.out.println("mLastFetchTransactions size: "+mLastFetchTransactions.size());

                while(collection.getCurrentPage().size() > 0) {
                    collection = collection.nextPage();
                    System.out.println("nextPage size: "+collection.getData().size()+", "+collection.getTotalObjects());
                    mLastFetchTransactions.addAll(collection.getData());
                }
                System.out.println( "mLastFetchTransactions final size: " + mLastFetchTransactions.size());
            }
        } catch (BackendlessException e) {
            System.out.println("Failed to fetch transactions: "+e.toString());
            errorCode = AppConstants.GENERAL_ERROR;
        }

        return errorCode;
    }

}
        