package com.mytest.events.persistence_service;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.AbstractContext;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.DbConstants;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.*;

import java.util.ArrayList;
import java.util.Date;

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
    public void beforeCreate(RunnerContext context, CustomerOps customerops) throws Exception
    {
        initCommon();
        mLogger.debug("In CustomerOpsTableEventHandler: beforeCreate: "+context.getUserRoles());
        mLogger.debug(context.toString());

        /*
        Backendless.Logging.flush();
        throw new BackendlessException( "123", "testing");*/

        String otp = customerops.getOtp();
        if(otp==null || otp.isEmpty()) {
            // First run, generate OTP if all fine
            String errorMsg = "";

            // Fetch customer
            String mobileNum = customerops.getMobile_num();
            Customers customer = mBackendOps.getCustomer(mobileNum, true);
            if(customer==null) {
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            }

            // check if customer is enabled
            String status = CommonUtils.checkCustomerStatus(customer);
            if( status != null) {
                CommonUtils.throwException(mLogger,status, "Customer account is not active", false);
            }

            // Don't verify QR card# for 'new card' operation
            String custOp = customerops.getOp_code();
            if( !custOp.equals(DbConstants.CUSTOMER_OP_NEW_CARD) &&
                    !customer.getMembership_card().getCard_id().equals(customerops.getQr_card()) ) {

                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_WRONG_CARD, "Wrong membership card", false);
            }

            // Don't verify PIN for 'reset PIN' operation
            String pin = customerops.getPin();
            if( !custOp.equals(DbConstants.CUSTOMER_OP_RESET_PIN) &&
                    !customer.getTxn_pin().equals(pin) ) {

                if( CommonUtils.handleCustomerWrongAttempt(mBackendOps, customer, DbConstants.ATTEMPT_TYPE_USER_PIN) ) {

                    CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD,
                            "Wrong PIN attempt limit reached: "+customer.getMobile_num(), false);
                } else {
                    CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_WRONG_PIN, "Wrong PIN attempt: "+customer.getMobile_num(), false);
                }
            }

            // Generate OTP and send SMS
            AllOtp newOtp = new AllOtp();
            newOtp.setUser_id(mobileNum);
            if(custOp.equals(DbConstants.CUSTOMER_OP_CHANGE_MOBILE)) {
                newOtp.setMobile_num(customerops.getExtra_op_params());
            } else {
                newOtp.setMobile_num(mobileNum);
            }

            newOtp.setOpcode(custOp);
            newOtp = mBackendOps.generateOtp(newOtp);
            if(newOtp == null) {
                // failed to generate otp
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_OTP_GENERATE_FAILED, "OTP generate failed", false);
            }

            // OTP generated successfully - return exception to indicate so
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "OTP generated successfully", true);

        } else {
            // Second run, as OTP available
            AllOtp fetchedOtp = mBackendOps.fetchOtp(customerops.getMobile_num());
            if( fetchedOtp == null ||
                    !mBackendOps.validateOtp(fetchedOtp, otp) ) {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_WRONG_OTP, "", false);
            }
            // remove PIN and OTP from the object
            customerops.setPin(null);
            customerops.setOtp(null);
            customerops.setOp_status(DbConstants.CUSTOMER_OP_STATUS_OTP_MATCHED);

            mLogger.debug("OTP matched for given customer operation: "+customerops.getMobile_num()+", "+customerops.getOp_code());
        }

        //Backendless.Logging.flush();
    }

    @Override
    public void afterCreate( RunnerContext context, CustomerOps customerops, ExecutionResult<CustomerOps> result ) throws Exception
    {
        initCommon();
        mLogger.debug("In CustomerOpsTableEventHandler: afterCreate"+((AbstractContext)context).toString());
        mLogger.debug(context.toString());

        // not required, as operation invoked by merchant
        // this will ensure that backend operations are executed, as logged-in user who called this api using generated SDK
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

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
            CommonUtils.throwException(mLogger,errorCode, "", false);
        }
        //Backendless.Logging.flush();
    }

    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.CustomerOpsTableEventHandler");
        mBackendOps = new BackendOps(mLogger);
    }

    private String changeCustomerCardOrMobile(CustomerOps custOp, boolean isNewCardCase) {
        String errorCode = null;

        // fetch new card record
        // if ok, fetch customer record
        // if ok, disable customer record temporarily, till all records are updated
        // if ok, update rowid_qr for all cashback records for this customer
        // if ok, save new card in the customer record & enable the customer again
        // if ok, update old card record status

        String newCardId = custOp.getQr_card();
        String oldMobile = custOp.getMobile_num();
        String newMobile = custOp.getExtra_op_params();

        CustomerCards newCard = null;
        if(isNewCardCase) {
            // fetch new card record
            newCard = mBackendOps.getCustomerCard(newCardId);
            if(newCard == null) {
                return mBackendOps.mLastOpStatus;
            }
            if(newCard.getStatus() != DbConstants.CUSTOMER_CARD_STATUS_WITH_MERCHANT) {
                return BackendResponseCodes.BE_ERROR_CARD_INUSE;
            }
            //TODO: enable this in final testing
            //if(!newCard.getMerchantId().equals(merchantId)) {
            //  return ResponseCodes.RESPONSE_CODE_QR_WRONG_MERCHANT;
            //}
        }

        // fetch user with the given id with related customer object
        String custMobile = custOp.getMobile_num();
        BackendlessUser user = mBackendOps.fetchUser(custMobile, DbConstants.USER_TYPE_CUSTOMER);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }
        Customers customer = (Customers) user.getProperty("customer");
        // check if customer is enabled
        errorCode = CommonUtils.checkCustomerStatus(customer);
        if( errorCode != null) {
            return errorCode;
        }

        CustomerCards oldCard = null;
        if(isNewCardCase) {
            // current QR card object
            oldCard = customer.getMembership_card();
            if (oldCard == null) {
                mLogger.error("QR card data not available: " + custMobile);
                return BackendResponseCodes.BE_ERROR_GENERAL;
            }
        }

        // get cashback tables for this customer
        String cbTablesRow = customer.getCashback_table();
        String[] cbTables = cbTablesRow.split(",");
        if(cbTables.length <= 0) {
            mLogger.error("Empty cashback table names: "+custMobile);
            return BackendResponseCodes.BE_ERROR_GENERAL;
        }

        // loop on all CB tables for this customer
        String priv_id = customer.getPrivate_id();
        for(int i=0; i<cbTables.length; i++) {
            // fetch all CB records for this customer from this CB table
            //Backendless.Data.mapTableToClass(cbTables[i], Cashback.class);
            ArrayList<Cashback> data = mBackendOps.fetchCashback("cust_private_id = '"+priv_id+"'", cbTables[i]);
            if(data!=null) {
                // disable account temporarily till we complete the update
                // do it only once for first cashback table (i==0)
                if(i==0) {
                    customer.setAdmin_status(DbConstants.USER_STATUS_DISABLED);
                    customer.setAdmin_remarks("Done by system. Processing Mobile or QR card change.");
                    user = mBackendOps.updateUser(user);
                    if(user==null) {
                        return mBackendOps.mLastOpStatus;
                    }
                }

                // loop on all cashback objects and update rowid_qr
                mLogger.debug("Fetched cashback records: "+custMobile+", "+priv_id+", "+cbTables[i]+", "+data.size());
                for (int k = 0; k < data.size(); k++) {
                    if(isNewCardCase) {
                        errorCode = updateCashbackForQrCode(data.get(k), oldCard.getCard_id(), newCardId);
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
            newCard.setStatus_update_time(new Date());
            customer.setMembership_card(newCard);
        } else {
            user.setProperty("user_id", newMobile);
            customer.setMobile_num(newMobile);
        }

        user.setProperty("customer",customer);
        user = mBackendOps.updateUser(user);
        if(user==null) {
            // TODO: add in alarms table for manual correction later
            return BackendResponseCodes.BE_ERROR_SERVER_ERROR_ACC_DISABLED;
        }

        // update old qr card status
        if(isNewCardCase) {
            oldCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_REMOVED);
            oldCard.setStatus_reason(custOp.getExtra_op_params());
            oldCard.setStatus_update_time(new Date());
            mBackendOps.saveQrCard(oldCard);
        }

        // Send message to customer informing the same - ignore sent status
        String smsText = null;
        if(isNewCardCase) {
            smsText = buildNewCardSMS(customer.getMobile_num(), newCard.getCard_id());
            SmsHelper.sendSMS(smsText, customer.getMobile_num());
        } else {
            smsText = buildMobileChangeSMS(oldMobile, customer.getMobile_num());
            SmsHelper.sendSMS(smsText, oldMobile+","+customer.getMobile_num());
        }

        return null;
    }

    private String updateCashbackForMobile(Cashback cb, String oldMobile, String newMobile) {
        String rowid = cb.getRowid();
        if(rowid.startsWith(oldMobile)) {
            String newRowid = rowid.replace(oldMobile, newMobile);
            cb.setRowid(newRowid);
            // save updated cashback object
            if(mBackendOps.saveCashback(cb) == null) {
                return mBackendOps.mLastOpStatus;
            }
        } else {
            mLogger.error("Cashback with non-matching mobile num: "+rowid+","+oldMobile);
            return BackendResponseCodes.BE_ERROR_GENERAL;
        }
        return null;
    }

    private String updateCashbackForQrCode(Cashback cb, String oldQrCode, String newQrCode) {
        String rowid_qr = cb.getRowid_card();
        if(rowid_qr.startsWith(oldQrCode)) {
            String newRowid = rowid_qr.replace(oldQrCode, newQrCode);
            cb.setRowid_card(newRowid);
            // save updated cashback object
            if(mBackendOps.saveCashback(cb) == null) {
                return mBackendOps.mLastOpStatus;
            }
        } else {
            mLogger.error("Cashback with non-matching qr code: "+cb.getRowid_card()+","+oldQrCode);
            return BackendResponseCodes.BE_ERROR_GENERAL;
        }
        return null;
    }

    private String resetCustomerPin(String mobileNum) {
        String errorCode = null;

        // fetch user with the given id with related customer object
        BackendlessUser user = mBackendOps.fetchUser(mobileNum, DbConstants.USER_TYPE_CUSTOMER);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }
        Customers customer = (Customers) user.getProperty("customer");
        // check if customer is enabled
        errorCode = CommonUtils.checkCustomerStatus(customer);
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
            return BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED;
            // dont care about return code - if failed, user can always reset pin again
        }

        return null;
    }

    private String buildPwdResetSMS(String userId, String pin) {
        return String.format(SmsConstants.SMS_PIN, CommonUtils.getHalfVisibleId(userId),pin);
    }

    private String buildNewCardSMS(String userId, String card_num) {
        return String.format(SmsConstants.SMS_CUSTOMER_NEW_CARD, card_num, CommonUtils.getHalfVisibleId(userId));
    }

    private String buildMobileChangeSMS(String userId, String mobile_num) {
        return String.format(SmsConstants.SMS_MOBILE_CHANGE, CommonUtils.getHalfVisibleId(userId), CommonUtils.getHalfVisibleId(mobile_num));
    }


}