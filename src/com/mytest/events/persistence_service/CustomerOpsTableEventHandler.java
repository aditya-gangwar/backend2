package com.mytest.events.persistence_service;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.AbstractContext;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.mytest.constants.*;
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

    @Override
    public void beforeCreate(RunnerContext context, CustomerOps customerops) throws Exception
    {
        initCommon();
        boolean positiveException = false;

        try {
            mLogger.debug("In CustomerOpsTableEventHandler: beforeCreate: " + context.getUserRoles());
            //mLogger.debug(context.toString());
            HeadersManager.getInstance().addHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY, context.getUserToken());

            String otp = customerops.getOtp();
            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine
                String errorMsg = "";

                // Fetch customer
                String mobileNum = CommonUtils.addMobileCC(customerops.getMobile_num());
                Customers customer = BackendOps.getCustomer(mobileNum, BackendConstants.CUSTOMER_ID_MOBILE);
                // check if customer is enabled
                CommonUtils.checkCustomerStatus(customer);

                // Don't verify QR card# for 'new card' operation
                String custOp = customerops.getOp_code();
                if (!custOp.equals(DbConstants.CUSTOMER_OP_NEW_CARD) &&
                        !customer.getMembership_card().getCard_id().equals(customerops.getQr_card())) {

                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_CARD, "Wrong membership card");
                }

                // Don't verify PIN for 'reset PIN' operation
                String pin = customerops.getPin();
                if (!custOp.equals(DbConstants.CUSTOMER_OP_RESET_PIN) &&
                        !customer.getTxn_pin().equals(pin)) {

                    CommonUtils.handleWrongAttempt(customer, DbConstants.USER_TYPE_CUSTOMER, DbConstantsBackend.ATTEMPT_TYPE_USER_PIN);
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_PIN, "Wrong PIN attempt: " + customer.getMobile_num());
                }

                // Generate OTP and send SMS
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(mobileNum);
                if (custOp.equals(DbConstants.CUSTOMER_OP_CHANGE_MOBILE)) {
                    newOtp.setMobile_num(customerops.getExtra_op_params());
                } else {
                    newOtp.setMobile_num(mobileNum);
                }
                newOtp.setOpcode(custOp);
                BackendOps.generateOtp(newOtp);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(customerops.getMobile_num(), otp);
                // remove PIN and OTP from the object
                customerops.setPin(null);
                customerops.setOtp(null);
                customerops.setOp_status(DbConstants.CUSTOMER_OP_STATUS_OTP_MATCHED);
                mLogger.debug("OTP matched for given customer operation: " + customerops.getMobile_num() + ", " + customerops.getOp_code());
            }

        } catch (Exception e) {
            if(!positiveException) {
                mLogger.error("Exception in CustomerOpsTableEventHandler:beforeCreate: "+e.toString());
            }
            Backendless.Logging.flush();

            if(e instanceof BackendlessException) {
                throw CommonUtils.getNewException((BackendlessException) e);
            }
            throw e;
        }
    }

    @Override
    public void afterCreate( RunnerContext context, CustomerOps customerops, ExecutionResult<CustomerOps> result ) throws Exception
    {
        initCommon();
        try {
            mLogger.debug("In CustomerOpsTableEventHandler: afterCreate"+((AbstractContext)context).toString());
            //mLogger.debug(context.toString());
            HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, context.getUserToken() );

            String opcode = customerops.getOp_code();
            switch(opcode) {
                case DbConstants.CUSTOMER_OP_NEW_CARD:
                    changeCustomerCard(customerops);
                    break;
                case DbConstants.CUSTOMER_OP_CHANGE_MOBILE:
                    changeCustomerMobile(customerops);
                    break;
                case DbConstants.CUSTOMER_OP_RESET_PIN:
                    resetCustomerPin(customerops.getMobile_num());
                    break;
            }

        } catch (Exception e) {
            mLogger.error("Exception in CustomerOpsTableEventHandler:afterCreate: "+e.toString());
            Backendless.Logging.flush();
            if(e instanceof BackendlessException) {
                throw CommonUtils.getNewException((BackendlessException) e);
            }
            throw e;
        }
    }

    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.CustomerOpsTableEventHandler");
    }

    private void changeCustomerMobile(CustomerOps custOp) {
        String oldMobile = CommonUtils.addMobileCC(custOp.getMobile_num());
        String newMobile = CommonUtils.addMobileCC(custOp.getExtra_op_params());

        // fetch user with the given id with related customer object
        BackendlessUser user = BackendOps.fetchUser(oldMobile, DbConstants.USER_TYPE_CUSTOMER);
        Customers customer = (Customers) user.getProperty("customer");
        // check if customer is enabled
        CommonUtils.checkCustomerStatus(customer);

        // update mobile number
        user.setProperty("user_id", newMobile);
        customer.setMobile_num(newMobile);
        user.setProperty("customer", customer);
        BackendOps.updateUser(user);

        // Send message to customer informing the same - ignore sent status
        String smsText = buildMobileChangeSMS( CommonUtils.removeMobileCC(oldMobile), CommonUtils.removeMobileCC(customer.getMobile_num()) );
        SmsHelper.sendSMS(smsText, oldMobile+","+customer.getMobile_num());
    }

    private void changeCustomerCard(CustomerOps custOp) {
        // fetch new card record
        CustomerCards newCard = BackendOps.getCustomerCard(custOp.getQr_card());
        CommonUtils.checkCardForAllocation(newCard);
        //TODO: enable this in final testing
        //if(!newCard.getMerchantId().equals(merchantId)) {
        //  return ResponseCodes.RESPONSE_CODE_QR_WRONG_MERCHANT;
        //}

        // fetch user with the given id with related customer object
        String custMobile = custOp.getMobile_num();
        BackendlessUser user = BackendOps.fetchUser(custMobile, DbConstants.USER_TYPE_CUSTOMER);
        Customers customer = (Customers) user.getProperty("customer");
        CustomerCards oldCard = customer.getMembership_card();
        // check if customer is enabled
        CommonUtils.checkCustomerStatus(customer);

        // update 'customer' and 'CustomerCard' objects for new card
        newCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
        newCard.setStatus_update_time(new Date());
        customer.setMembership_card(newCard);
        customer.setCardId(newCard.getCard_id());

        user.setProperty("customer", customer);
        BackendOps.updateUser(user);

        // update old card status
        try {
            oldCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_REMOVED);
            oldCard.setStatus_reason(custOp.getExtra_op_params());
            oldCard.setStatus_update_time(new Date());
            BackendOps.saveCustomerCard(oldCard);
        } catch (BackendlessException e) {
            // ignore as not considered as failure for whole 'changeCustomerCardOrMobile' operation
            // but log as alarm for manual correction
            //TODO: raise alarm
            mLogger.error("Exception while updating old card status: "+e.toString());
        }

        // Send message to customer informing the same - ignore sent status
        String smsText = buildNewCardSMS(CommonUtils.removeMobileCC(customer.getMobile_num()), newCard.getCard_id());
        SmsHelper.sendSMS(smsText, customer.getMobile_num());
    }

    private void resetCustomerPin(String mobileNum) {
        // fetch user with the given id with related customer object
        BackendlessUser user = BackendOps.fetchUser(CommonUtils.addMobileCC(mobileNum), DbConstants.USER_TYPE_CUSTOMER);
        Customers customer = (Customers) user.getProperty("customer");
        CommonUtils.checkCustomerStatus(customer);

        // generate pin
        String newPin = CommonUtils.generateCustomerPIN();

        // update user account for the PIN
        user.setPassword(newPin);
        //TODO: encode PIN
        customer.setTxn_pin(newPin);
        user.setProperty("customer",customer);

        BackendOps.updateUser(user);

        // Send SMS through HTTP
        String smsText = buildPwdResetSMS(CommonUtils.removeMobileCC(customer.getMobile_num()), newPin);
        if( !SmsHelper.sendSMS(smsText, customer.getMobile_num()) )
        {
            //TODO: raise alarm
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, "");
        }
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

    /*
    private void changeCustomerCardOrMobile(CustomerOps custOp, boolean isNewCardCase) {
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
            newCard = BackendOps.getCustomerCard(newCardId);
            CommonUtils.checkCardForAllocation(newCard);
            //TODO: enable this in final testing
            //if(!newCard.getMerchantId().equals(merchantId)) {
            //  return ResponseCodes.RESPONSE_CODE_QR_WRONG_MERCHANT;
            //}
        }

        // fetch user with the given id with related customer object
        String custMobile = custOp.getMobile_num();
        BackendlessUser user = BackendOps.fetchUser(custMobile, DbConstants.USER_TYPE_CUSTOMER);
        Customers customer = (Customers) user.getProperty("customer");
        // check if customer is enabled
        CommonUtils.checkCustomerStatus(customer);

        CustomerCards oldCard = null;
        if(isNewCardCase) {
            // current QR card object
            oldCard = customer.getMembership_card();
        }

        // get cashback tables for this customer
        String cbTablesRow = customer.getCashback_table();
        String[] cbTables = cbTablesRow.split(",");

        // loop on all CB tables for this customer
        try {
            String priv_id = customer.getPrivate_id();
            for (int i = 0; i < cbTables.length; i++) {
                // fetch all CB records for this customer from this CB table
                //Backendless.Data.mapTableToClass(cbTables[i], Cashback.class);
                ArrayList<Cashback> data = BackendOps.fetchCashback("cust_private_id = '" + priv_id + "'", cbTables[i]);
                if (data != null) {
                    // disable account temporarily till we complete the update
                    // do it only once for first cashback table (i==0)
                    if (i == 0) {
                        customer.setAdmin_status(DbConstants.USER_STATUS_DISABLED);
                        customer.setStatus_reason(DbConstants.DISABLED_AUTO_BY_SYSTEM);
                        customer.setAdmin_remarks("For changing mobile number or membership card");
                        user = BackendOps.updateUser(user);
                    }

                    // loop on all cashback objects and update rowid_qr
                    mLogger.debug("Fetched cashback records: " + custMobile + ", " + priv_id + ", " + cbTables[i] + ", " + data.size());
                    for (int k = 0; k < data.size(); k++) {
                        if (isNewCardCase) {
                            updateCashbackForCardId(data.get(k), oldCard.getCard_id(), newCardId);
                        } else {
                            updateCashbackForMobile(data.get(k), customer.getMobile_num(), newMobile);
                        }
                    }
                } else {
                    //TODO: raise alarm
                    mLogger.error("No cashback data found for " + custMobile + " in " + cbTables[i]);
                }
            }

            // all cashback objects updated successfully, in all CB tables
            // enable back the user, and set the new QR card / mobile num too
            // update status of new card too
            customer.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
            customer.setStatus_reason(DbConstants.ENABLED_ACTIVE);
            customer.setAdmin_remarks(null);

            if (isNewCardCase) {
                newCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
                newCard.setStatus_update_time(new Date());
                customer.setMembership_card(newCard);
                customer.setCardId(newCard.getCard_id());
            } else {
                user.setProperty("user_id", newMobile);
                customer.setMobile_num(newMobile);
            }

            user.setProperty("customer", customer);
            BackendOps.updateUser(user);

        } catch(Exception e) {
            // Any exception in part needs manual correction - raise alarm accordingly
            // TODO: raise alarm for manual correction
            mLogger.error("Exception while updating customer for mobile/card: "+e.toString());
            throw e;
        }

        // update old qr card status
        try {
            if(isNewCardCase) {
                oldCard.setStatus(DbConstants.CUSTOMER_CARD_STATUS_REMOVED);
                oldCard.setStatus_reason(custOp.getExtra_op_params());
                oldCard.setStatus_update_time(new Date());
                    BackendOps.saveCustomerCard(oldCard);
            }
        } catch (BackendlessException e) {
            // ignore as not considered as failure for whole 'changeCustomerCardOrMobile' operation
            // but log as alarm for manual correction
            //TODO: raise alarm
            mLogger.error("Exception while updating old card status: "+e.toString());
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
    }

    private void updateCashbackForMobile(Cashback cb, String oldMobile, String newMobile) {
        String rowid = cb.getRowid();
        if(rowid.startsWith(oldMobile)) {
            String newRowid = rowid.replace(oldMobile, newMobile);
            cb.setRowid(newRowid);
            // save updated cashback object
            BackendOps.saveCashback(cb);
        } else {
            //TODO: raise alarm
            mLogger.error("Cashback with non-matching mobile num: "+rowid+","+oldMobile);
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, "");
        }
    }

    private void updateCashbackForCardId(Cashback cb, String oldQrCode, String newQrCode) {
        String rowid_qr = cb.getRowid_card();
        if(rowid_qr.startsWith(oldQrCode)) {
            String newRowid = rowid_qr.replace(oldQrCode, newQrCode);
            cb.setRowid_card(newRowid);
            // save updated cashback object
            BackendOps.saveCashback(cb);
        } else {
            //TODO: raise alarm
            mLogger.error("Cashback with non-matching qr code: "+cb.getRowid_card()+","+oldQrCode);
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, "");
        }
    }
    */

