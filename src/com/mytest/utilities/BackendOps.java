package com.mytest.utilities;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.CommonConstants;
import com.mytest.constants.DbConstants;
import com.mytest.constants.GlobalSettingsConstants;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by adgangwa on 15-05-2016.
 */
public class BackendOps {

    public String mLastOpStatus;
    public String mLastOpErrorMsg;
    private Logger mLogger;

    public BackendOps(Logger logger) {
        mLogger = logger;
    }

    /*
     * BackendlessUser operations
     */
    public BackendlessUser registerUser(BackendlessUser user) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            return Backendless.UserService.register(user);
        }
        catch( BackendlessException e )
        {
            mLastOpErrorMsg = "User register failed: "+e.toString();
            ////mLogger.error(mLastOpErrorMsg);
            // Handle backend error codes
            mLastOpStatus = e.getCode();
            if (mLastOpStatus.equals(BackendResponseCodes.BL_ERROR_REGISTER_DUPLICATE)) {
                mLastOpStatus = BackendResponseCodes.BE_ERROR_DUPLICATE_USER;
            }
        }
        return null;
    }

    public boolean assignRole(String userId, String role) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        try {
            Backendless.UserService.assignRole(userId, role);
            return true;
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in loginUser: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return false;
    }

    public BackendlessUser loginUser(String userId, String password) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        try {
            return Backendless.UserService.login(userId, password, false);
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in loginUser: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public boolean logoutUser() {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        try {
            Backendless.UserService.logout();
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in logoutUser: " + e.toString();
            //mLogger.error(mLastOpErrorMsg);
            mLastOpStatus = e.getCode();
            return false;
        }
        return true;
    }

    public BackendlessUser updateUser(BackendlessUser user) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        // Save merchant object
        try {
            return Backendless.UserService.update( user );
        } catch(BackendlessException e) {
            mLastOpErrorMsg = "Exception in updateUser: " + e.toString();
            //mLogger.error(mLastOpErrorMsg);
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public BackendlessUser fetchUser(String userid, int userType) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            query.setWhereClause("user_id = '"+userid+"'");

            QueryOptions queryOptions = new QueryOptions();
            if(userType == DbConstants.USER_TYPE_CUSTOMER) {
                queryOptions.addRelated( "customer");
                queryOptions.addRelated( "customer.membership_card");

            } else if(userType == DbConstants.USER_TYPE_MERCHANT) {
                queryOptions.addRelated( "merchant");
                queryOptions.addRelated("merchant.trusted_devices");
            } else if(userType == DbConstants.USER_TYPE_AGENT) {
                queryOptions.addRelated( "agent");
            }

            query.setQueryOptions( queryOptions );
            BackendlessCollection<BackendlessUser> user = Backendless.Data.of( BackendlessUser.class ).find(query);
            if( user.getTotalObjects() == 0) {
                // no data found
                mLastOpErrorMsg = "No user found: "+userid;
                //mLogger.warn(mLastOpErrorMsg);
                mLastOpStatus = BackendResponseCodes.BE_ERROR_NO_SUCH_USER;
            } else {
                return user.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in fetchUser: " + e.toString();
            //mLogger.error(mLastOpErrorMsg);
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public BackendlessUser getCurrentMerchantUser() {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        try {
            BackendlessUser user = Backendless.UserService.CurrentUser();
            Merchants merchant = (Merchants) user.getProperty("merchant");
            if(merchant == null) {
                // load merchant object
                ArrayList<String> relationProps = new ArrayList<>();
                relationProps.add("merchant");
                Backendless.Data.of( BackendlessUser.class ).loadRelations(user, relationProps);

                return user;
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in getCurrentMerchantUser: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Merchant operations
     */
    private Merchants loadMerchant(BackendlessUser user) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        ArrayList<String> relationProps = new ArrayList<>();
        relationProps.add("merchant");
        relationProps.add("merchant.trusted_devices");
        try {
            Backendless.Data.of( BackendlessUser.class ).loadRelations(user, relationProps);
            return (Merchants)user.getProperty("merchant");
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in loadMerchant: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public Merchants getMerchant(String userId, boolean fetchTrustedDevices) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            query.setWhereClause("auto_id = '"+userId+"'");

            if(fetchTrustedDevices) {
                QueryOptions queryOptions = new QueryOptions();
                queryOptions.addRelated("trusted_devices");
                query.setQueryOptions(queryOptions);
            }

            BackendlessCollection<Merchants> user = Backendless.Data.of( Merchants.class ).find(query);
            if( user.getTotalObjects() == 0) {
                // no data found
                mLastOpErrorMsg = "No merchant found: "+userId;
                //mLogger.warn(mLastOpErrorMsg);
                mLastOpStatus = BackendResponseCodes.BE_ERROR_NO_SUCH_USER;
            } else {
                return user.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in getMerchant: " + e.toString();
            //mLogger.error(mLastOpErrorMsg);
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public Merchants getMerchantByMobile(String mobileNum) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            query.setWhereClause("mobile_num = '"+mobileNum+"'");

            BackendlessCollection<Merchants> user = Backendless.Data.of( Merchants.class ).find(query);
            if( user.getTotalObjects() == 0) {
                // no data found
                mLastOpErrorMsg = "No merchant found by mobile num: "+mobileNum;
                mLastOpStatus = BackendResponseCodes.BE_ERROR_NO_SUCH_USER;
            } else {
                return user.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in getMerchant: " + e.toString();
            //mLogger.error(mLastOpErrorMsg);
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public ArrayList<Merchants> fetchMerchants(String whereClause) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            // fetch all merchants, not yet archived
            query.setPageSize(CommonConstants.dbQueryMaxPageSize);
            query.setWhereClause(whereClause);

            BackendlessCollection<Merchants> users = Backendless.Data.of( Merchants.class ).find(query);
            int cnt = users.getTotalObjects();
            if( cnt == 0) {
                // No unprocessed merchant left with this prefix
                mLogger.info("No merchant available for where clause: "+whereClause);
            } else {
                ArrayList<Merchants> objects = new ArrayList<>();
                while (users.getCurrentPage().size() > 0)
                {
                    int size  = users.getCurrentPage().size();
                    System.out.println( "Loaded " + size + " merchants in the current page" );

                    Iterator<Merchants> iterator = users.getCurrentPage().iterator();
                    while( iterator.hasNext() )
                    {
                        objects.add(iterator.next());
                    }
                    users = users.nextPage();
                }
                return objects;
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in fetchMerchants: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public Merchants updateMerchant(Merchants merchant) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try {
            return Backendless.Persistence.save(merchant);
        } catch(BackendlessException e) {
            mLastOpErrorMsg = "Exception in updateMerchant: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Customer operations
     */
    public Customers getCustomer(String custId, boolean mobileAsId) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            if(mobileAsId) {
                query.setWhereClause("mobile_num = '"+custId+"'");
            } else {
                query.setWhereClause("membership_card.card_id = '"+custId+"'");
            }

            QueryOptions queryOptions = new QueryOptions();
            queryOptions.addRelated("membership_card");
            query.setQueryOptions(queryOptions);

            BackendlessCollection<Customers> user = Backendless.Data.of( Customers.class ).find(query);
            if( user.getTotalObjects() == 0) {
                // no data found
                mLastOpErrorMsg = "No customer found: "+custId;
                //mLogger.warn(mLastOpErrorMsg);
                mLastOpStatus = BackendResponseCodes.BE_ERROR_NO_SUCH_USER;
            } else {
                //mLogger.debug("Found customer: "+custId);
                return user.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in getCustomer: " + e.toString();
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public Customers updateCustomer(Customers customer) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try {
            return Backendless.Persistence.save(customer);
        } catch(BackendlessException e) {
            mLastOpErrorMsg = "Exception in updateCustomer: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Customer card operations
     */
    public CustomerCards getCustomerCard(String cardId) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("card_id = '" + cardId + "'");

            BackendlessCollection<CustomerCards> collection = Backendless.Data.of(CustomerCards.class).find(dataQuery);
            if( collection.getTotalObjects() == 0) {
                // no data found
                mLastOpErrorMsg = "No customer card found: "+cardId;
                //mLogger.warn(mLastOpErrorMsg);
                mLastOpStatus = BackendResponseCodes.BE_ERROR_NO_SUCH_CARD;
            } else {
                //mLogger.debug("Fetched customer card: "+cardId);
                return collection.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in getCustomerCard: " + e.toString();
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public CustomerCards saveCustomerCard(CustomerCards card) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            Backendless.Persistence.save( card );
        }
        catch( BackendlessException e ) {
            mLastOpErrorMsg = "Exception in saveQrCard: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Cashback operations
     */
    public ArrayList<Cashback> fetchCashback(String whereClause, String cashbackTable) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        // fetch cashback objects from DB
        try {
            Backendless.Data.mapTableToClass(cashbackTable, Cashback.class);

            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            //dataQuery.setPageSize( CommonConstants.dbQueryMaxPageSize );
            dataQuery.setWhereClause(whereClause);

            /*
            if(fetchCustomer) {
                QueryOptions queryOptions = new QueryOptions();
                queryOptions.addRelated("customer");
                queryOptions.addRelated("customer.membership_card");
                dataQuery.setQueryOptions(queryOptions);
            }*/

            BackendlessCollection<Cashback> collection = Backendless.Data.of(Cashback.class).find(dataQuery);

            int cnt = collection.getTotalObjects();
            if(cnt > 0) {
                ArrayList<Cashback> objects = new ArrayList<>();
                while (collection.getCurrentPage().size() > 0)
                {
                    int size  = collection.getCurrentPage().size();
                    System.out.println( "Loaded " + size + " cashback rows in the current page" );

                    Iterator<Cashback> iterator = collection.getCurrentPage().iterator();
                    while( iterator.hasNext() )
                    {
                        objects.add(iterator.next());
                    }
                    collection = collection.nextPage();
                }
                return objects;
            } else {
                mLastOpErrorMsg = "No cashback object found: "+whereClause;
                mLastOpStatus = BackendResponseCodes.BL_ERROR_NO_DATA_FOUND;
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in fetchCashback: " + e.toString();
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public Cashback saveCashback(Cashback cb) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            return Backendless.Persistence.save( cb );
        }
        catch( BackendlessException e ) {
            mLastOpErrorMsg = "Exception in saveCashback: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }


    /*
     * OTP operations
     */
    public AllOtp generateOtp(AllOtp otp) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        // check if any OTP object already available for this user_id
        // if yes, update the same for new OTP, op and time.
        // If no, create new object
        // Send SMS with OTP
        try
        {
            AllOtp newOtp = null;
            AllOtp oldOtp = fetchOtp(otp.getUser_id());
            if(oldOtp !=  null) {
                // update oldOtp
                oldOtp.setOtp_value(CommonUtils.generateOTP());
                oldOtp.setOpcode(otp.getOpcode());
                oldOtp.setMobile_num(otp.getMobile_num());
                newOtp = Backendless.Persistence.save( oldOtp );
            } else {
                otp.setOtp_value(CommonUtils.generateOTP());
                newOtp = Backendless.Persistence.save( otp );
            }

            // Send SMS through HTTP
            String smsText = String.format(SmsConstants.SMS_OTP,
                    newOtp.getOpcode(),
                    CommonUtils.getHalfVisibleId(newOtp.getUser_id()),
                    newOtp.getOtp_value(),
                    GlobalSettingsConstants.OTP_VALID_MINS);

            if( !SmsHelper.sendSMS(smsText, newOtp.getMobile_num()) )
            {
                mLastOpStatus = BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED;
                mLastOpErrorMsg = "In generateOtp: Failed to send SMS";
            } else {
                //mLogger.debug("OTP generated and SMS sent successfully "+newOtp.getMobile_num());
                return newOtp;
            }
        }
        catch( BackendlessException e )
        {
            mLastOpErrorMsg = "Exception in generateOtp: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public boolean deleteOtp(AllOtp otp) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            Backendless.Persistence.of( AllOtp.class ).remove( otp );
            return true;
        }
        catch( BackendlessException e ) {
            mLastOpErrorMsg = "Exception in deleteOtp: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return false;
    }

    public boolean validateOtp(AllOtp otp, String rcvdOtp) {
        Date otpTime = otp.getUpdated()==null?otp.getCreated():otp.getUpdated();
        Date currTime = new Date();

        if ( ((currTime.getTime() - otpTime.getTime()) < (GlobalSettingsConstants.OTP_VALID_MINS*60*1000)) &&
                rcvdOtp.equals(otp.getOtp_value()) ) {
            // active otp available and matched
            deleteOtp(otp);
            return true;
        } else {
            return false;
        }
    }

    public AllOtp fetchOtp(String userId) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("user_id = '" + userId + "'");

            BackendlessCollection<AllOtp> collection = Backendless.Data.of(AllOtp.class).find(dataQuery);
            if( collection.getTotalObjects() > 0) {
                return collection.getData().get(0);
            } else {
                mLastOpErrorMsg = "Iin fetchOtp: No data found" + userId;
                mLastOpStatus = BackendResponseCodes.BL_ERROR_NO_DATA_FOUND;
            }
        }
        catch( BackendlessException e )
        {
            mLastOpErrorMsg = "Exception in fetchOtp: " + e.toString();
            //mLogger.error(mLastOpErrorMsg);
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Global settings operations
     */
    /*
    public GlobalSettings fetchGlobalSettings() {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        try {
            return Backendless.Persistence.of( GlobalSettings.class).findLast();
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in fetchGlobalSettings: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }*/

    /*
     * Counters operations
     */
    public Double fetchCounterValue(String name) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("name = '" + name + "'");

            BackendlessCollection<Counters> collection = Backendless.Data.of(Counters.class).find(dataQuery);
            if( collection.getTotalObjects() > 0) {
                Counters counter = collection.getData().get(0);

                // increment counter - very important to do
                counter.setValue(counter.getValue()+1);
                counter = Backendless.Persistence.save( counter );
                return counter.getValue();
            } else {
                mLastOpErrorMsg = "In fetchCounter: No data found" + name;
                mLastOpStatus = BackendResponseCodes.BL_ERROR_NO_DATA_FOUND;
            }
        }
        catch( BackendlessException e )
        {
            mLastOpErrorMsg = "Exception in fetchCounter: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Trusted Devices operations
     */
    public MerchantDevice fetchDevice(String deviceId) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("device_id = '" + deviceId + "'");

            BackendlessCollection<MerchantDevice> collection = Backendless.Data.of(MerchantDevice.class).find(dataQuery);
            if( collection.getTotalObjects() > 0) {
                return collection.getData().get(0);
            } else {
                mLastOpErrorMsg = "In fetchDevice: No data found" + deviceId;
                mLastOpStatus = BackendResponseCodes.BL_ERROR_NO_DATA_FOUND;
            }
        }
        catch( BackendlessException e )
        {
            mLastOpErrorMsg = "Exception in fetchDevice: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Merchant operations
     */
    public MerchantOps addMerchantOp(MerchantOps op) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            return Backendless.Persistence.save( op );
        }
        catch( BackendlessException e )
        {
            mLastOpErrorMsg = "Exception in addMerchantOp: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public ArrayList<MerchantOps> fetchMerchantOps(String whereClause) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        // fetch cashback objects from DB
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setPageSize( CommonConstants.dbQueryMaxPageSize );

            // TODO: check if putting index on cust_private_id improves performance
            // or using rowid_qr in where clause improves performance
            dataQuery.setWhereClause(whereClause);

            BackendlessCollection<MerchantOps> collection = Backendless.Data.of(MerchantOps.class).find(dataQuery);

            int cnt = collection.getTotalObjects();
            if(cnt > 0) {
                ArrayList<MerchantOps> objects = new ArrayList<>();
                while (collection.getCurrentPage().size() > 0)
                {
                    int size  = collection.getCurrentPage().size();

                    Iterator<MerchantOps> iterator = collection.getCurrentPage().iterator();
                    while( iterator.hasNext() )
                    {
                        objects.add(iterator.next());
                    }
                    collection = collection.nextPage();
                }
                return objects;
            } else {
                mLogger.debug("No merchantop object found: "+whereClause);
                mLastOpStatus = BackendResponseCodes.BL_ERROR_NO_DATA_FOUND;
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in fetchMerchantOps: " + e.toString();
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public MerchantOps saveMerchantOp(MerchantOps op) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            return Backendless.Persistence.save( op );
        }
        catch( BackendlessException e ) {
            mLastOpErrorMsg = "Exception in saveMerchantOp: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * WrongAttempts operations
     */
    // returns 'null' if not found and new created
    public WrongAttempts fetchOrCreateWrongAttempt(String userId, String type, int userType) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        WrongAttempts attempt = fetchWrongAttempts(userId, type);
        if(attempt==null) {
            // create row
            WrongAttempts newAttempt = new WrongAttempts();
            newAttempt.setUser_id(userId);
            newAttempt.setAttempt_type(type);
            newAttempt.setAttempt_cnt(1);
            newAttempt.setUser_type(userType);
            return saveWrongAttempt(newAttempt);
        }
        return attempt;
    }

    public WrongAttempts fetchWrongAttempts(String userId, String type) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("user_id = '" + userId + "'" + "AND attempt_type = '" + type + "'");

            BackendlessCollection<WrongAttempts> collection = Backendless.Data.of(WrongAttempts.class).find(dataQuery);
            if( collection.getTotalObjects() > 0) {
                return collection.getData().get(0);
            } else {
                mLogger.debug("No WrongAttempts object found: "+userId+type);
                mLastOpStatus = BackendResponseCodes.BL_ERROR_NO_DATA_FOUND;
            }
        }
        catch( BackendlessException e )
        {
            mLastOpErrorMsg = "Exception in fetchDevice: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public WrongAttempts saveWrongAttempt(WrongAttempts attempt) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            return Backendless.Persistence.save( attempt );
        }
        catch( BackendlessException e ) {
            mLastOpErrorMsg = "Exception in saveWrongAttempt: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * MerchantStats operations
     */
    public MerchantStats fetchMerchantStats(String merchantId) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
            dataQuery.setWhereClause("merchant_id = '" + merchantId + "'");

            BackendlessCollection<MerchantStats> collection = Backendless.Data.of(MerchantStats.class).find(dataQuery);
            if( collection.getTotalObjects() > 0) {
                return collection.getData().get(0);
            } else {
                mLogger.debug("No MerchantStats object found: "+merchantId);
                mLastOpStatus = BackendResponseCodes.BL_ERROR_NO_DATA_FOUND;
            }
        }
        catch( BackendlessException e )
        {
            mLastOpErrorMsg = "Exception in fetchMerchantStats: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public MerchantStats saveMerchantStats(MerchantStats stats) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try
        {
            return Backendless.Persistence.save( stats );
        }
        catch( BackendlessException e ) {
            mLastOpErrorMsg = "Exception in saveMerchantStats: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }


    /*
     * Transaction operations
     */
    public List<Transaction> fetchTransactions(String whereClause) {
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
            mLastOpStatus = e.getCode();
            return null;
        }

        return transactions;
    }

    /*
     * Agent operations
     */
    public Agents getAgent(String userId) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            query.setWhereClause("id = '"+userId+"'");

            BackendlessCollection<Agents> user = Backendless.Data.of( Agents.class ).find(query);
            if( user.getTotalObjects() == 0) {
                // no data found
                mLastOpErrorMsg = "No agent found: "+userId;
                mLastOpStatus = BackendResponseCodes.BE_ERROR_NO_SUCH_USER;
            } else {
                return user.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in getAgent: " + e.toString();
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    private Merchants loadAgent(BackendlessUser user) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";

        ArrayList<String> relationProps = new ArrayList<>();
        relationProps.add("agent");
        try {
            Backendless.Data.of( BackendlessUser.class ).loadRelations(user, relationProps);
            return (Merchants)user.getProperty("agent");
        } catch (BackendlessException e) {
            mLastOpErrorMsg = "Exception in loadAgent: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public Agents updateAgent(Agents agent) {
        mLastOpStatus = BackendResponseCodes.BE_RESPONSE_NO_ERROR;
        mLastOpErrorMsg = "";
        try {
            return Backendless.Persistence.save(agent);
        } catch(BackendlessException e) {
            mLastOpErrorMsg = "Exception in updateAgent: " + e.toString();
            mLastOpStatus = e.getCode();
        }
        return null;
    }



    /*
    public List<MerchantDevice> fetchTrustedDevices(String merchantId) {
        mLogger.debug("In fetchTrustedDevices: "+merchantId);

        // fetch txns object from DB
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
            dataQuery.setWhereClause("merchant_id = '" + merchantId + "'");

            BackendlessCollection<MerchantDevice> collection = Backendless.Data.of(MerchantDevice.class).find(dataQuery);
            if( collection.getTotalObjects() > 0) {
                return collection.getData();
            } else {
                mLogger.debug("No Merchant devices found: "+merchantId);
                mLastOpStatus = BackendResponseCodes.BL_ERROR_NO_DATA_FOUND;
            }
        } catch (BackendlessException e) {
            mLogger.error("Failed to fetch trusted devices: "+e.toString());
            mLastOpStatus = e.getCode();
        }

        return null;
    }*/
}
