package com.mytest;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.logging.Logger;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by adgangwa on 15-05-2016.
 */
public class BackendOps {

    public String mLastOpStatus;

    /*
     * BackendlessUser operations
     */
    public BackendlessUser loginUser(String userId, String password) {
        mLastOpStatus = "";

        try {
            return Backendless.UserService.login(userId, password, false);
        } catch (BackendlessException e) {
            // Handle backend error codes
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public BackendlessUser updateUser(BackendlessUser user) {
        mLastOpStatus = "";

        // Save merchant object
        try {
            return Backendless.UserService.update( user );
        } catch(BackendlessException e) {
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public BackendlessUser fetchUser(String userid, int userType) {
        mLastOpStatus = "";

        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            query.setWhereClause("user_id = '"+userid+"'");

            QueryOptions queryOptions = new QueryOptions();
            if(userType == DbConstants.USER_TYPE_CUSTOMER) {
                queryOptions.addRelated( "customer");
            } else if(userType == DbConstants.USER_TYPE_MERCHANT) {
                queryOptions.addRelated( "merchant");
            }
            query.setQueryOptions( queryOptions );

            BackendlessCollection<BackendlessUser> user = Backendless.Data.of( BackendlessUser.class ).find(query);
            return user.getData().get(0);
        } catch (BackendlessException e) {
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Customer operations
     */
    public Customers getCustomer(String custId, boolean mobileAsId) {
        mLastOpStatus = "";
        try {
            BackendlessDataQuery query = new BackendlessDataQuery();
            if(mobileAsId) {
                query.setWhereClause("mobile_num = '"+custId+"'");
            } else {
                query.setWhereClause("qr_card.qrcode = '"+custId+"'");
            }

            QueryOptions queryOptions = new QueryOptions();
            queryOptions.addRelated("qr_card");
            query.setQueryOptions(queryOptions);

            BackendlessCollection<Customers> user = Backendless.Data.of( Customers.class ).find(query);
            return user.getData().get(0);
        } catch (BackendlessException e) {
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public Customers updateCustomer(Customers customer) {
        try {
            return Backendless.Persistence.save(customer);

        } catch(BackendlessException e) {
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Customer card operations
     */
    public CustomerCards getCustomerCard(String qrCode) {
        mLastOpStatus = "";
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("qrcode = '" + qrCode + "'");

            BackendlessCollection<CustomerCards> collection = Backendless.Data.of(CustomerCards.class).find(dataQuery);
            //int size = collection.getTotalObjects();
            return collection.getData().get(0);
        } catch (BackendlessException e) {
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public CustomerCards saveQrCard(CustomerCards card) {
        mLastOpStatus = "";
        try
        {
            Backendless.Persistence.save( card );
        }
        catch( BackendlessException e )
        {
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Cashback operations
     */
    public ArrayList<Cashback> fetchCashback(String whereClause) {
        mLastOpStatus = "";
        // fetch cashback objects from DB
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setPageSize( AppConstants.dbQueryMaxPageSize );

            // TODO: check if putting index on cust_private_id improves performance
            // or using rowid_qr in where clause improves performance
            dataQuery.setWhereClause(whereClause);

            BackendlessCollection<Cashback> collection = Backendless.Data.of(Cashback.class).find(dataQuery);

            ArrayList<Cashback> objects = new ArrayList<>(collection.getTotalObjects());
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
        } catch (BackendlessException e) {
            mLastOpStatus = e.getCode();
        }

        return null;
    }

    public Cashback saveCashback(Cashback cb) {
        mLastOpStatus = "";
        try
        {
            return Backendless.Persistence.save( cb );
        }
        catch( BackendlessException e )
        {
            mLastOpStatus = e.getCode();
        }
        return null;
    }


    /*
     * OTP operations
     */
    public AllOtp generateOtp(AllOtp otp) {
        mLastOpStatus = "";
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
                newOtp = Backendless.Persistence.save( otp );
            }

            // Send SMS through HTTP
            String smsText = String.format(SmsConstants.SMS_TEMPLATE_OTP,
                    newOtp.getOpcode(),
                    CommonUtils.getHalfVisibleId(newOtp.getUser_id()),
                    newOtp.getOtp_value(),
                    AppConstants.OTP_VALID_MINS);

            if( !SmsHelper.sendSMS(smsText, newOtp.getMobile_num()) )
            {
                mLastOpStatus = AppConstants.BL_MYERROR_SEND_SMS_FAILED;
            } else {
                return newOtp;
            }

        }
        catch( BackendlessException e )
        {
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    public boolean deleteOtp(AllOtp otp) {
        mLastOpStatus = "";
        try
        {
            Backendless.Persistence.of( AllOtp.class ).remove( otp );
            return true;
        }
        catch( BackendlessException e )
        {
            mLastOpStatus = e.getCode();
        }
        return false;
    }

    public boolean validateOtp(AllOtp otp, String rcvdOtp) {
        Date otpTime = otp.getUpdated()==null?otp.getCreated():otp.getUpdated();
        Date currTime = new Date();

        return ( (currTime.getTime() - otpTime.getTime()) < (AppConstants.OTP_VALID_MINS*60*1000) &&
                rcvdOtp.equals(otp.getOtp_value()) );
    }

    public AllOtp fetchOtp(String userId) {
        mLastOpStatus = "";
        try
        {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            dataQuery.setWhereClause("user_id = '" + userId + "'");

            BackendlessCollection<AllOtp> collection = Backendless.Data.of(AllOtp.class).find(dataQuery);
            return collection.getData().get(0);
        }
        catch( BackendlessException e )
        {
            mLastOpStatus = e.getCode();
        }
        return null;
    }

    /*
     * Global settings operations
     */
    public GlobalSettings fetchGlobalSettings() {
        mLastOpStatus = "";
        try {
            return Backendless.Persistence.of( GlobalSettings.class).findLast();
        } catch (BackendlessException e) {
            mLastOpStatus = e.getCode();
        }
        return null;
    }

}
