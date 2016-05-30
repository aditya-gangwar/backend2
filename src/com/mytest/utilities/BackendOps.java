package com.mytest.utilities;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by adgangwa on 15-05-2016.
 */
public class BackendOps {

    public String mLastOpStatus;
    private Logger mLogger;

    public BackendOps(Logger logger) {
        mLogger = logger;
    }

    /*
     * BackendlessUser operations
     */
    public BackendlessUser loginUser(String userId, String password) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;

        try {
            return Backendless.UserService.login(userId, password, false);
        } catch (BackendlessException e) {
            mLogger.error("Exception in loginUser: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public boolean logoutUser() {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;

        try {
            Backendless.UserService.logout();
        } catch (BackendlessException e) {
            mLogger.error("Exception in logoutUser: " + e.toString());
            mLastOpStatus = e.getCode();
            return false;
        }
        return true;
    }

    public BackendlessUser updateUser(BackendlessUser user) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;

        // Save merchant object
        try {
            return Backendless.UserService.update( user );
        } catch(BackendlessException e) {
            mLogger.error("Exception in updateUser: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public BackendlessUser fetchUser(String userid, int userType) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;

        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            query.setWhereClause("user_id = '"+userid+"'");

            QueryOptions queryOptions = new QueryOptions();
            if(userType == DbConstants.USER_TYPE_CUSTOMER) {
                queryOptions.addRelated( "customer");
                queryOptions.addRelated( "customer.qr_card");

            } else if(userType == DbConstants.USER_TYPE_MERCHANT) {
                queryOptions.addRelated( "merchant");
                queryOptions.addRelated("merchant.trusted_devices");
            }
            query.setQueryOptions( queryOptions );
            BackendlessCollection<BackendlessUser> user = Backendless.Data.of( BackendlessUser.class ).find(query);
            if( user.getTotalObjects() == 0) {
                // no data found
                mLogger.warn("No user found: "+userid);
                mLastOpStatus = BackendResponseCodes.BL_MYERROR_NO_SUCH_USER;
            } else {
                return user.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLogger.error("Exception in fetchUser: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Customer operations
     */
    public Merchants getMerchant(String userId) {
        mLogger.debug("In getMerchant: "+userId);
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            query.setWhereClause("auto_id = '"+userId+"'");

            QueryOptions queryOptions = new QueryOptions();
            queryOptions.addRelated("trusted_devices");
            query.setQueryOptions(queryOptions);

            BackendlessCollection<Merchants> user = Backendless.Data.of( Merchants.class ).find(query);
            if( user.getTotalObjects() == 0) {
                // no data found
                mLogger.warn("No merchant found: "+userId);
                mLastOpStatus = BackendResponseCodes.BL_MYERROR_NO_SUCH_USER;
            } else {
                return user.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLogger.error("Exception in getMerchant: " + e.toString());
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public Merchants updateMerchant(Merchants merchant) {
        try {
            return Backendless.Persistence.save(merchant);
        } catch(BackendlessException e) {
            mLogger.error("Exception in updateMerchant: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Customer operations
     */
    public Customers getCustomer(String custId, boolean mobileAsId) {
        mLogger.debug("In getCustomer: "+custId);
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            if(mobileAsId) {
                query.setWhereClause("mobile_num = '"+custId+"'");
            } else {
                query.setWhereClause("qr_card.qrcode = '"+custId+"'");
            }

            /*
            QueryOptions queryOptions = new QueryOptions();
            queryOptions.addRelated("qr_card");
            query.setQueryOptions(queryOptions);*/

            BackendlessCollection<Customers> user = Backendless.Data.of( Customers.class ).find(query);
            if( user.getTotalObjects() == 0) {
                // no data found
                mLogger.warn("No customer found: "+custId);
                mLastOpStatus = BackendResponseCodes.BL_MYERROR_NO_SUCH_USER;
            } else {
                return user.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLogger.error("Exception in getCustomer: " + e.toString());
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public Customers updateCustomer(Customers customer) {
        try {
            return Backendless.Persistence.save(customer);
        } catch(BackendlessException e) {
            mLogger.error("Exception in updateCustomer: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Customer card operations
     */
    public CustomerCards getCustomerCard(String qrCode) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("qrcode = '" + qrCode + "'");

            BackendlessCollection<CustomerCards> collection = Backendless.Data.of(CustomerCards.class).find(dataQuery);
            if( collection.getTotalObjects() == 0) {
                // no data found
                mLogger.warn("No customer card found: "+qrCode);
                mLastOpStatus = BackendResponseCodes.BL_MYERROR_NO_SUCH_QR_CARD;
            } else {
                return collection.getData().get(0);
            }
        } catch (BackendlessException e) {
            mLogger.error("Exception in getCustomerCard: " + e.toString());
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public CustomerCards saveQrCard(CustomerCards card) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try
        {
            Backendless.Persistence.save( card );
        }
        catch( BackendlessException e ) {
            mLogger.error("Exception in saveQrCard: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Cashback operations
     */
    public ArrayList<Cashback> fetchCashback(String whereClause) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        // fetch cashback objects from DB
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setPageSize( CommonConstants.dbQueryMaxPageSize );

            // TODO: check if putting index on cust_private_id improves performance
            // or using rowid_qr in where clause improves performance
            dataQuery.setWhereClause(whereClause);

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
                mLogger.warn("No cashback object found: "+whereClause);
                mLastOpStatus = BackendResponseCodes.BL_MYERROR_GENERAL;
            }
        } catch (BackendlessException e) {
            mLogger.error("Exception in fetchCashback: " + e.toString());
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public Cashback saveCashback(Cashback cb) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try
        {
            return Backendless.Persistence.save( cb );
        }
        catch( BackendlessException e ) {
            mLogger.error("Exception in saveCashback: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }


    /*
     * OTP operations
     */
    public AllOtp generateOtp(AllOtp otp) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
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
            String smsText = String.format(SmsConstants.SMS_TEMPLATE_OTP,
                    newOtp.getOpcode(),
                    CommonUtils.getHalfVisibleId(newOtp.getUser_id()),
                    newOtp.getOtp_value(),
                    GlobalSettingsConstants.OTP_VALID_MINS);

            if( !SmsHelper.sendSMS(smsText, newOtp.getMobile_num()) )
            {
                mLastOpStatus = BackendResponseCodes.BL_MYERROR_SEND_SMS_FAILED;
            } else {
                return newOtp;
            }
        }
        catch( BackendlessException e )
        {
            mLogger.error("Exception in generateOtp: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public boolean deleteOtp(AllOtp otp) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try
        {
            Backendless.Persistence.of( AllOtp.class ).remove( otp );
            return true;
        }
        catch( BackendlessException e ) {
            mLogger.error("Exception in deleteOtp: " + e.toString());
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
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try
        {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("user_id = '" + userId + "'");

            BackendlessCollection<AllOtp> collection = Backendless.Data.of(AllOtp.class).find(dataQuery);
            if( collection.getTotalObjects() > 0) {
                return collection.getData().get(0);
            }
        }
        catch( BackendlessException e )
        {
            mLogger.error("Exception in fetchOtp: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Global settings operations
     */
    /*
    public GlobalSettings fetchGlobalSettings() {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try {
            return Backendless.Persistence.of( GlobalSettings.class).findLast();
        } catch (BackendlessException e) {
            mLogger.error("Exception in fetchGlobalSettings: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }*/

    /*
     * Counters operations
     */
    public Double fetchCounterValue(String name) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
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
            }
        }
        catch( BackendlessException e )
        {
            mLogger.error("Exception in fetchCounter: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public MerchantDevice fetchDevice(String deviceId) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try
        {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("device_id = '" + deviceId + "'");

            BackendlessCollection<MerchantDevice> collection = Backendless.Data.of(MerchantDevice.class).find(dataQuery);
            if( collection.getTotalObjects() > 0) {
                return collection.getData().get(0);
            }
        }
        catch( BackendlessException e )
        {
            mLogger.error("Exception in fetchDevice: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public MerchantOps addMerchantOp(String opCode, Merchants merchant) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try
        {
            MerchantOps op = new MerchantOps();
            op.setMerchant_id(merchant.getAuto_id());
            op.setMobile_num(merchant.getMobile_num());
            op.setOp_code(opCode);
            op.setOp_status(DbConstants.MERCHANT_OP_STATUS_PENDING);

            op = Backendless.Persistence.save( op );
            return op;
        }
        catch( BackendlessException e )
        {
            mLogger.error("Exception in addMerchantOp: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public ArrayList<MerchantOps> fetchMerchantOps(String whereClause) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
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
                mLastOpStatus = BackendResponseCodes.BL_MYERROR_GENERAL;
            }
        } catch (BackendlessException e) {
            mLogger.error("Exception in fetchMerchantOps: " + e.toString());
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public MerchantOps saveMerchantOp(MerchantOps op) {
        mLastOpStatus = BackendResponseCodes.BL_MYRESPONSE_NO_ERROR;
        try
        {
            return Backendless.Persistence.save( op );
        }
        catch( BackendlessException e ) {
            mLogger.error("Exception in saveMerchantOp: " + e.toString());
            mLastOpStatus = e.getCode();
        }
        return null;
    }

}
