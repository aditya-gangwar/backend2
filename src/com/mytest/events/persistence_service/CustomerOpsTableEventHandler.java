package com.mytest.events.persistence_service;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.geo.GeoPoint;
import com.backendless.logging.Logger;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.property.ObjectProperty;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.backendless.servercode.annotation.Async;
import com.mytest.AppConstants;
import com.mytest.BackendOps;
import com.mytest.CommonUtils;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * CustomerOpsTableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "CustomerOps" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */

@Asset( "CustomerOps" )
public class CustomerOpsTableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<CustomerOps>
{
    private Logger mLogger;
    private BackendOps mBackendOps;

    @Override
    public void beforeCreate( RunnerContext context, CustomerOps customerops) throws Exception
    {
        // Init logger
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.CustomerOpsTableEventHandler");
        BackendOps backendOps = new BackendOps();

        String otp = customerops.getOtp();
        if(otp==null || otp.isEmpty()) {
            // First run, generate OTP if all fine
            String errorCode = null;
            String errorMsg = null;

            String mobileNum = customerops.getMobile_num();
            Customers customer = backendOps.getCustomer(mobileNum, true);
            if(customer==null) {
                errorCode = backendOps.mLastOpStatus;
                errorMsg = "Backend error";
                if(errorCode.equals(AppConstants.BL_ERROR_NO_DATA_FOUND)) {
                    errorCode = AppConstants.BL_MYERROR_NO_SUCH_CUSTOMER;
                    errorMsg = "No customer with given mobile number";
                }
                BackendlessFault fault = new BackendlessFault(errorCode,errorMsg);
                throw new BackendlessException(fault);
            }

            // Don't verify QR card# for 'new card' operation
            String custOp = customerops.getOp_code();
            if( !custOp.equals(DbConstants.CUSTOMER_OP_NEW_CARD) &&
                    !customer.getQr_card().getQrcode().equals(customerops.getQr_card()) ) {
                BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_WRONG_QR_CARD, "Wrong Customer card");
                throw new BackendlessException(fault);
            }

            // Don't verify PIN for 'reset PIN' operation
            String pin = customerops.getPin();
            if( !custOp.equals(DbConstants.CUSTOMER_OP_RESET_PIN) &&
                    !customer.getTxn_pin().equals(pin) ) {
                BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_WRONG_PIN, "Wrong Customer PIN");
                throw new BackendlessException(fault);
            }

            // Generate OTP
            AllOtp newOtp = new AllOtp();
            newOtp.setUser_id(mobileNum);
            newOtp.setMobile_num(mobileNum);
            newOtp.setOpcode(custOp);
            newOtp = backendOps.generateOtp(newOtp);
            if(newOtp == null) {
                // failed to generate otp
                BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_OTP_GENERATE_FAILED,"Failed to generate OTP");
                throw new BackendlessException(fault);
            }

            // OTP generated successfully - return exception to indicate so
            BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_OTP_GENERATED,"OTP not provided");
            throw new BackendlessException(fault);

        } else {
            // Second run, as OTP available
            AllOtp fetchedOtp = backendOps.fetchOtp(customerops.getMobile_num());
            if( fetchedOtp == null ||
                    !backendOps.validateOtp(fetchedOtp, otp) ) {
                BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_WRONG_OTP,"Wrong OTP value");
                throw new BackendlessException(fault);
            }
        }
    }

    @Override
    public void afterCreate( RunnerContext context, CustomerOps customerops, ExecutionResult<CustomerOps> result ) throws Exception
    {
        // Init logger
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.CustomerOpsTableEventHandler");
        mBackendOps = new BackendOps();

        String errorCode = null;
        String opcode = customerops.getOp_code();
        switch(opcode) {
            case DbConstants.CUSTOMER_OP_NEW_CARD:
                errorCode = changeCustomerCardOrMobile(customerops, true);
                break;
            case DbConstants.CUSTOMER_OP_CHANGE_MOBILE:
                errorCode = changeCustomerCardOrMobile(customerops, false);
                break;
            case DbConstants.CUSTOMER_OP_RESET_PIN:
                errorCode = resetCustomerPin(customerops.getMobile_num());
                break;
        }

        if(errorCode != null) {
            BackendlessFault fault = new BackendlessFault(errorCode,"Customer operation failed");
            throw new BackendlessException(fault);
        }
    }

    private String changeCustomerCardOrMobile(CustomerOps custOp, boolean isNewCardCase) {
        String errorCode = null;

        // not required, as operation invoked by merchant
        // this will ensure that backend operations are executed, as logged-in user who called this api using generated SDK
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        /*
         * fetch new card record
         * if ok, fetch customer record
         * if ok, disable customer record temporarily, till all records are updated
         * if ok, update rowid_qr for all cashback records for this customer
         * if ok, save new card in the customer record & enable the customer again
         * if ok, update old card record status
         */

        String newQrCode = custOp.getQr_card();
        String newMobile = custOp.getExtra_op_params();

        CustomerCards newCard = null;
        if(isNewCardCase) {
            // fetch new card record
            newCard = mBackendOps.getCustomerCard(newQrCode);
            if(newCard == null) {
                return mBackendOps.mLastOpStatus.equals(AppConstants.BL_ERROR_NO_DATA_FOUND)?
                        AppConstants.BL_MYERROR_WRONG_QR_CARD:
                        mBackendOps.mLastOpStatus;
            }
            mLogger.debug("Fetched customer card: "+newCard.getQrcode()+", "+newCard.getStatus());
            if(newCard.getStatus() != DbConstants.CUSTOMER_CARD_STATUS_WITH_MERCHANT) {
                return AppConstants.BL_MYERROR_QR_CARD_INUSE;
            }
            /*
            //TODO: enable this in final testing
            if(!newCard.getMerchantId().equals(merchantId)) {
                return ResponseCodes.RESPONSE_CODE_QR_WRONG_MERCHANT;
            }*/
        }

        // fetch user with the given id with related customer object
        String custMobile = custOp.getMobile_num();
        BackendlessUser user = mBackendOps.fetchUser(custMobile, DbConstants.USER_TYPE_CUSTOMER);
        if(user==null) {
            return mBackendOps.mLastOpStatus.equals(AppConstants.BL_ERROR_NO_DATA_FOUND)?
                    AppConstants.BL_MYERROR_NO_SUCH_CUSTOMER:
                    mBackendOps.mLastOpStatus;
        }
        Customers customer = (Customers) user.getProperty("customer");
        // check if customer is enabled
        errorCode = processAdminStatus(customer);
        if( errorCode != null) {
            return errorCode;
        }

        CustomerCards oldCard = null;
        if(isNewCardCase) {
            // current QR card object
            oldCard = customer.getQr_card();
            if (oldCard == null) {
                mLogger.error("QR card data not available: " + custMobile);
                return AppConstants.BL_MYERROR_GENERAL;
            }
        }

        // get cashback tables for this customer
        String cbTablesRow = customer.getCashback_table();
        String[] cbTables = cbTablesRow.split(",");
        if(cbTables.length <= 0) {
            mLogger.error("Empty cashback table names: "+custMobile);
            return AppConstants.BL_MYERROR_GENERAL;
        }

        // loop on all CB tables for this customer
        String priv_id = customer.getPrivate_id();
        for(int i=0; i<cbTables.length; i++) {
            // fetch all CB records for this customer from this CB table
            Backendless.Data.mapTableToClass(cbTables[i], Cashback.class);
            ArrayList<Cashback> data = mBackendOps.fetchCashback("cust_private_id = '"+priv_id+"'");
            if(data!=null) {
                // disable account temporarily till we complete the update
                // do it only once for first cashback table (i==0)
                if(i==0) {
                    customer.setAdmin_status(DbConstants.USER_STATUS_DISABLED);
                    customer.setAdmin_remarks("Processing Mobile or QR card change");
                    user = mBackendOps.updateUser(user);
                    if(user==null) {
                        return mBackendOps.mLastOpStatus;
                    }
                    /*
                    customer = mBackendOps.updateCustomer(customer);
                    if(customer==null) {
                        return AppConstants.BL_MYERROR_GENERAL;
                    }*/
                }

                // loop on all cashback objects and update rowid_qr
                for (int k = 0; k < data.size(); k++) {
                    if(isNewCardCase) {
                        errorCode = updateCashbackForQrCode(data.get(k), oldCard.getQrcode(), newQrCode);
                    } else {
                        errorCode = updateCashbackForMobile(data.get(k), customer.getMobile_num(), newMobile);
                    }
                    if( errorCode != null) {
                        // TODO: add in alarms table for manual correction later
                        return errorCode;
                    }
                }
            } else {
                mLogger.error("No cashback data found for "+custMobile+" in "+cbTables[i]);
                return mBackendOps.mLastOpStatus;
            }
        }

        // all cashback objects updated successfully, in all CB tables
        // enable back the user, and set the new QR card / mobile num too
        // update status of new card too
        customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
        customer.setAdmin_remarks(null);

        if(isNewCardCase) {
            newCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
            newCard.setAllocationTime(new Date());
            customer.setQr_card(newCard);
        } else {
            user.setProperty("user_id", newMobile);
            customer.setMobile_num(newMobile);
        }

        user = mBackendOps.updateUser(user);
        if(user==null) {
            // TODO: add in alarms table for manual correction later
            return AppConstants.BL_MYERROR_SERVER_ERROR_ACC_DISABLED;
        }
        /*
        customer = mBackendOps.updateCustomer(customer);
        if(customer==null) {
            return AppConstants.BL_MYERROR_SERVER_ERROR_ACC_DISABLED;
        }*/

        // update old qr card status
        if(isNewCardCase) {
            oldCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_REMOVED);
            oldCard.setRemovedReason(custOp.getExtra_op_params());
            oldCard.setRemovedTime(new Date());
            mBackendOps.saveQrCard(oldCard);
        }

        return null;
    }

    private String processAdminStatus(Customers customer) {

        if(customer.getAdmin_status() == DbConstants.USER_STATUS_DISABLED) {
            return AppConstants.BL_MYERROR_CUSTOMER_ACC_DISABLED;

        } else if(customer.getAdmin_status() == DbConstants.USER_STATUS_DISABLED_WRONG_PIN) {
            mLogger.debug("Customer disabled due to wrong PIN: "+customer.getMobile_num());

            // Check if temporary blocked duration is over
            Date tempPasswdTime = customer.getTemp_blocked_time();
            if(tempPasswdTime!=null && tempPasswdTime.getTime() > 0) {
                // check for temp blocking duration expiry
                Date now = new Date();
                long timeDiff = now.getTime() - tempPasswdTime.getTime();

                // fetch global settings
                GlobalSettings settings = mBackendOps.fetchGlobalSettings();
                if(settings == null) {
                    return mBackendOps.mLastOpStatus;
                }

                long allowedDuration = settings.getTemp_card_block_duration_hrs() * 24 * 60 * 1000;

                if(timeDiff > allowedDuration) {
                    mLogger.debug("Customer usage after blocked duration expiry: " + tempPasswdTime.getTime());
                    // reset blocked time to null and update the status
                    // persisted to DB in calling function along with card change
                    customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
                    customer.setTemp_blocked_time(null);
                } else {
                    return AppConstants.BL_MYERROR_CUSTOMER_ACC_DISABLED;
                }
            } else {
                mLogger.error("Status is USER_STATUS_DISABLED_WRONG_PIN, but time is null");
                customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
            }
        }

        return null;
    }

    private String updateCashbackForMobile(Cashback cb, String oldMobile, String newMobile) {
        String rowid = cb.getRowid();
        if(rowid.startsWith(oldMobile)) {
            String newRowid = rowid.replace(oldMobile, newMobile);
            cb.setRowid_qr(newRowid);
            // save updated cashback object
            if(mBackendOps.saveCashback(cb) == null) {
                return mBackendOps.mLastOpStatus;
            }
        } else {
            mLogger.error("Cashback with non-matching mobile num: "+rowid+","+oldMobile);
            return AppConstants.BL_MYERROR_GENERAL;
        }
        return null;
    }

    private String updateCashbackForQrCode(Cashback cb, String oldQrCode, String newQrCode) {
        String rowid_qr = cb.getRowid_qr();
        if(rowid_qr.startsWith(oldQrCode)) {
            String newRowid = rowid_qr.replace(oldQrCode, newQrCode);
            cb.setRowid_qr(newRowid);
            // save updated cashback object
            if(mBackendOps.saveCashback(cb) == null) {
                return mBackendOps.mLastOpStatus;
            }
        } else {
            mLogger.error("Cashback with non-matching qr code: "+cb.getRowid_qr()+","+oldQrCode);
            return AppConstants.BL_MYERROR_GENERAL;
        }
        return null;
    }

    private String resetCustomerPin(String mobileNum) {
        String errorCode = null;

        // fetch user with the given id with related customer object
        BackendlessUser user = mBackendOps.fetchUser(mobileNum, DbConstants.USER_TYPE_CUSTOMER);
        if(user==null) {
            return mBackendOps.mLastOpStatus.equals(AppConstants.BL_ERROR_NO_DATA_FOUND)?
                    AppConstants.BL_MYERROR_NO_SUCH_CUSTOMER:
                    mBackendOps.mLastOpStatus;
        }
        Customers customer = (Customers) user.getProperty("customer");
        // check if customer is enabled
        errorCode = processAdminStatus(customer);
        if( errorCode != null) {
            return errorCode;
        }

        // generate pin
        String newPin = CommonUtils.generateCustomerPIN();

        // update user account for the PIN
        user.setPassword(newPin);
        //TODO: encode PIN
        customer.setTxn_pin(newPin);
        user.setProperty("customer",customer);

        user = mBackendOps.updateUser(user);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }

        // Send SMS through HTTP
        String smsText = buildPwdResetSMS(mobileNum, newPin);
        if( !SmsHelper.sendSMS(smsText, mobileNum) )
        {
            return AppConstants.BL_MYERROR_SEND_SMS_FAILED;
            // dont care about return code - if failed, user can always reset pin again
        }

        return null;
    }

    private String buildPwdResetSMS(String userId, String pin) {
        return String.format(SmsConstants.SMS_TEMPLATE_PIN,CommonUtils.getHalfVisibleId(userId),pin);
    }
}
