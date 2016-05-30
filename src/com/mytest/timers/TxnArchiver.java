package com.mytest.timers;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.mytest.utilities.BackendConstants;
import com.mytest.utilities.CommonConstants;
import com.mytest.database.Merchants;
import com.mytest.database.Transaction;

import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TxnArchiver
{
    //transid(10),time(20),merchantid(6),merchantname(50),customermobile(10),customerprivid(5),amts(6x4=24),rate(2) = 115 chars = 115x2 = 250 bytes
    private static final int CSV_RECORD_MAX_CHARS = 250;

    private List<Transaction> mLastFetchTransactions;
    private Merchants mLastFetchMerchant;
    private String mMerchantIdSuffix;
    private Logger mLogger;

    // map of 'txn date' -> 'csv file url'
    private HashMap<String,String> mCsvFiles;
    // map of 'txn date' -> 'csv string of all txns in dat date'
    private HashMap<String,StringBuilder> mCsvDataMap;

    // All required date formatters
    private SimpleDateFormat mSdfDateWithTime;
    private SimpleDateFormat mSdfOnlyDateBackend;
    private SimpleDateFormat mSdfOnlyDateFilename;
    private Date mToday;

    public TxnArchiver(String merchantIdSuffix) {
        mMerchantIdSuffix = merchantIdSuffix;

        mToday = new Date();
        mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
        mSdfOnlyDateBackend = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_BACKEND, CommonConstants.DATE_LOCALE);
        mSdfOnlyDateFilename = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_FILENAME, CommonConstants.DATE_LOCALE);

        mCsvFiles = new HashMap<>();
        mCsvDataMap = new HashMap<>();
    }

    public void execute() throws Exception
    {
        long startTime = System.currentTimeMillis();
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.timers.TxnArchiver");

        mLogger.debug("Running TxnArchiver"+mMerchantIdSuffix);

        mSdfDateWithTime.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        mSdfOnlyDateBackend.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        mSdfOnlyDateFilename.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        // Fetch next not processed merchant
        mLastFetchMerchant = null;
        mLastFetchMerchant = fetchNextMerchant();

        if(mLastFetchMerchant!=null) {
            String merchantId = mLastFetchMerchant.getAuto_id();
            mLogger.debug("Fetched merchant id: "+merchantId);

            String txnTableName = mLastFetchMerchant.getTxn_table();
            Backendless.Data.mapTableToClass(txnTableName, Transaction.class);

            mLastFetchTransactions = null;
            mLastFetchTransactions = fetchTransactions(buildTxnWhereClause(merchantId));
            if(mLastFetchTransactions != null) {
                mLogger.info("Fetched "+mLastFetchTransactions.size()+" transactions for "+merchantId);
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
                            mLogger.error( "Count of txns updated for status does not match.");
                            // rollback
                            updateTxnArchiveStatus(txnTableName,merchantId,false);
                            deleteCsvFiles();
                        } else {
                            if(updateMerchantArchiveTime()) {
                                mLogger.info("Txns archived successfully: "+recordsUpdated+", "+merchantId);
                            } else {
                                // rollback
                                updateTxnArchiveStatus(txnTableName,merchantId,false);
                                deleteCsvFiles();
                            }
                        }
                    }
                }
            }
        }
        long endTime = System.currentTimeMillis();
        mLogger.info( "Exiting TxnArchiver: "+((endTime-startTime)/1000));
    }

    private int updateTxnArchiveStatus(String txnTableName, String merchantId, boolean status) {
        mLogger.debug( "In updateTxnArchiveStatus: "+txnTableName+", "+merchantId+", "+status);
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
            mLogger.debug( "Txn update where clause: "+whereClause);

            UriBuilder builder = UriBuilder
                    .fromPath("https://api.backendless.com/v1/data/bulk")
                    .path("/{txntable}")
                    .queryParam("where", whereClause);
            URI uri = builder.build(txnTableName);
            mLogger.debug( "Txn update URL: "+uri.toString());
            URL url = new URL(uri.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("application-id", BackendConstants.APP_ID);
            conn.setRequestProperty("secret-key", BackendConstants.SECRET_KEY);
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
            mLogger.debug("Updated archive status of txns: "+recordsUpdated);
            return recordsUpdated;

        } catch (MalformedURLException e) {
            mLogger.error("Failed to update txn status: "+e.toString());
            return -1;
        } catch (IOException e) {
            mLogger.error("Failed to update txn status: "+e.toString());
            return -1;
        } catch (NumberFormatException e) {
            mLogger.error("Failed to update txn status: "+e.toString());
            return -1;
        }
    }

    private boolean updateMerchantArchiveTime() {
        // update archive date in merchant record
        // set to current time
        mLastFetchMerchant.setLast_archive(mToday);

        // Save merchant object
        try {
            mLastFetchMerchant = Backendless.Persistence.save( mLastFetchMerchant );
        } catch(BackendlessException e) {
            System.out.println("Merchant update failed: " + e.toString());
            return false;
        }
        return true;
    }

    private boolean createCsvFiles(String merchantId) {
        // one file for each day i.e. each entry in mCsvDataMap
        for (Map.Entry<String, StringBuilder> entry : mCsvDataMap.entrySet()) {
            String txnDate = entry.getKey();
            StringBuilder sb = entry.getValue();

            // File name: txns_<merchant_id>_<ddMMMyy>.csv
            String filepath = getMerchantTxnDir(merchantId) + getTxnCsvFilename(txnDate,merchantId);

            try {
                String fileUrl = Backendless.Files.saveFile(filepath, sb.toString().getBytes("UTF-8"), true);
                mLogger.info("Txn CSV file uploaded: " + fileUrl);
                mCsvFiles.put(txnDate,filepath);
            } catch (Exception e) {
                mLogger.error("Txn CSV file upload failed: "+ filepath + e.toString());
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
                mLogger.debug("Deleted CSV file: " + filePath);
            }
        } catch(Exception e) {
            mLogger.debug("CSV file delete failed: " + e.toString());
            return false;
        }
        return true;
    }

    private String getMerchantTxnDir(String merchantId) {
        // directory: merchants/txn_files/<first 2 chars of merchant id>/<next 2 chars of merchant id>/<merchant id>/
        return CommonConstants.MERCHANT_TXN_ROOT_DIR +
                merchantId.substring(0,2) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId.substring(2,4) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId + CommonConstants.FILE_PATH_SEPERATOR;
    }

    private String getTxnCsvFilename(String date, String merchantId) {
        // File name: txns_<merchant_id>_<ddMMMyy>.csv
        return CommonConstants.MERCHANT_TXN_FILE_PREFIX + merchantId + "_" + date + CommonConstants.CSV_FILE_EXT;
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
                mLogger.debug("buildCsvString, new day : "+txnDateStr);
                sb = new StringBuilder(CSV_RECORD_MAX_CHARS*size);
                // new file - write first line as header
                sb.append("trans_id,time,merchant_id,merchant_name,customer_id,cust_private_id,total_billed,cb_billed,cl_debit,cl_credit,cb_debit,cb_credit,cb_percent");
                sb.append(CommonConstants.CSV_NEWLINE);
                mCsvDataMap.put(txnDateStr,sb);
            }

            // trans_id,time,merchant_id,merchant_name,customer_id,cust_private_id,
            // total_billed,cb_billed,cl_debit,cl_credit,cb_debit,cb_credit,cb_percent\n
            sb.append(txn.getTrans_id()).append(CommonConstants.CSV_DELIMETER);
            //SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT_WITH_TIME, AppConstants.DATE_LOCALE);
            sb.append(mSdfDateWithTime.format(txnDate)).append(CommonConstants.CSV_DELIMETER);
            sb.append(mLastFetchMerchant.getAuto_id()).append(CommonConstants.CSV_DELIMETER);
            sb.append(mLastFetchMerchant.getName()).append(CommonConstants.CSV_DELIMETER);
            sb.append(txn.getCustomer_id()).append(CommonConstants.CSV_DELIMETER);
            sb.append(txn.getCust_private_id()).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getTotal_billed())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_billed())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCl_debit())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCl_credit())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_debit())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_credit())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_percent())).append(CommonConstants.CSV_DELIMETER);
            sb.append(CommonConstants.CSV_NEWLINE);
        }
    }

    private Merchants fetchNextMerchant() {
        BackendlessDataQuery query = new BackendlessDataQuery();
        // fetch only 1 record sorted by 'created'
        QueryOptions queryOptions = new QueryOptions("created");
        query.setQueryOptions(queryOptions);
        // TODO: to be set to AppConstants.dbQueryMaxPageSize in production
        query.setPageSize(1);

        query.setWhereClause(buildMerchantWhereClause());

        BackendlessCollection<Merchants> users = Backendless.Data.of( Merchants.class ).find(query);
        int cnt = users.getTotalObjects();
        if( cnt == 0) {
            // No unprocessed merchant left with this prefix
            mLogger.info("No merchant available for archive: "+mMerchantIdSuffix);
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
        whereClause.append("auto_id LIKE '%").append(mMerchantIdSuffix).append("'");

        String today = mSdfOnlyDateBackend.format(mToday);
        // merchants with last_archive time before today midnight
        whereClause.append(" AND (last_archive < '").append(today).append("'").append(" OR last_archive is null)");

        mLogger.debug("Merchant where clause: " + whereClause.toString());
        return whereClause.toString();
    }

    private String buildTxnWhereClause(String merchantId) {
        StringBuilder whereClause = new StringBuilder();

        // for particular merchant
        whereClause.append("merchant_id = '").append(merchantId).append("'");

        String today = mSdfOnlyDateBackend.format(mToday);
        // all txns older than today midnight - the timer runs 1 AM each day
        whereClause.append(" AND create_time < '").append(today).append("'");
        whereClause.append(" AND archived=").append("false");

        return whereClause.toString();
    }

    private List<Transaction> fetchTransactions(String whereClause) {
        mLogger.debug("In fetchTransactions: "+whereClause);
        // init values
        List<Transaction> transactions = null;

        // fetch txns object from DB
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            // sorted by create time
            QueryOptions queryOptions = new QueryOptions("create_time");
            dataQuery.setQueryOptions(queryOptions);
            dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
            dataQuery.setWhereClause(whereClause);

            BackendlessCollection<Transaction> collection = Backendless.Data.of(Transaction.class).find(dataQuery);

            int size = collection.getTotalObjects();
            mLogger.debug("Total txns from DB: " + size+", "+collection.getData().size());
            transactions = collection.getData();
            mLogger.debug("First page size: "+transactions.size());

            while(collection.getCurrentPage().size() > 0) {
                collection = collection.nextPage();
                mLogger.debug("nextPage size: "+collection.getData().size()+", "+collection.getTotalObjects());
                transactions.addAll(collection.getData());
            }
        } catch (BackendlessException e) {
            mLogger.error("Failed to fetch transactions: "+e.toString());
            return null;
        }

        return transactions;
    }

}
        