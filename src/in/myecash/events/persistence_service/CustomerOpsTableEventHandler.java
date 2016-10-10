package in.myecash.events.persistence_service;

/**
 * CustomerOpsTableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "CustomerOps" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */

/*
@Asset( "CustomerOps" )
public class CustomerOpsTableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<CustomerOps>
{
    private MyLogger mLogger;
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];

    @Override
    public void beforeCreate(RunnerContext context, CustomerOps customerops) throws Exception
    {
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "CustomerOps-beforeCreate";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = customerops.getOp_code()+BackendConstants.BACKEND_EDR_SUB_DELIMETER
                +customerops.getMobile_num()+BackendConstants.BACKEND_EDR_SUB_DELIMETER
                +customerops.getExtra_op_params()+BackendConstants.BACKEND_EDR_SUB_DELIMETER
                +customerops.getQr_card()+BackendConstants.BACKEND_EDR_SUB_DELIMETER
                +customerops.getRequestor_id();

        initCommon();
        boolean positiveException = false;

        try {
            mLogger.debug("In CustomerOpsTableEventHandler: beforeCreate: " + context.getUserRoles());
            HeadersManager.getInstance().addHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY, context.getUserToken());

            // Fetch merchant
            BackendlessUser user = BackendOps.fetchUserByObjectId(context.getUserId(), DbConstants.USER_TYPE_MERCHANT);
            mEdr[BackendConstants.EDR_USER_ID_IDX] = (String) user.getProperty("user_id");
            int userType = (Integer)user.getProperty("user_type");
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = String.valueOf(userType);
            if(userType!=DbConstants.USER_TYPE_MERCHANT) {
                throw new BackendlessException(BackendResponseCodes.OPERATION_NOT_ALLOWED, "Only merchant operation");
            }

            Merchants merchant = (Merchants) user.getProperty("merchant");
            mEdr[BackendConstants.EDR_MCHNT_ID_IDX] = merchant.getAuto_id();
            // check if merchant is enabled
            CommonUtils.checkMerchantStatus(merchant);
            customerops.setRequestor_id(merchant.getAuto_id());

            String otp = customerops.getOtp();
            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // Fetch customer
                String mobileNum = CommonUtils.addMobileCC(customerops.getMobile_num());
                Customers customer = BackendOps.getCustomer(mobileNum, BackendConstants.CUSTOMER_ID_MOBILE);
                mEdr[BackendConstants.EDR_CUST_ID_IDX] = customer.getMobile_num();
                // check if customer is enabled
                CommonUtils.checkCustomerStatus(customer);

                // Don't verify QR card# for 'new card' operation
                String cardId = customer.getMembership_card().getCard_id();
                mEdr[BackendConstants.EDR_CUST_CARD_ID_IDX] = cardId;
                String custOp = customerops.getOp_code();
                if (!custOp.equals(DbConstants.OP_NEW_CARD) &&
                        !cardId.equals(customerops.getQr_card())) {

                    throw new BackendlessException(BackendResponseCodes.WRONG_CARD, "Wrong membership card");
                }

                // Don't verify PIN for 'reset PIN' operation
                String pin = customerops.getPin();
                if (!custOp.equals(DbConstants.OP_RESET_PIN) &&
                        !customer.getTxn_pin().equals(pin)) {

                    CommonUtils.handleWrongAttempt(mobileNum, customer, DbConstants.USER_TYPE_CUSTOMER, DbConstantsBackend.WRONG_PARAM_TYPE_PIN);
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_PIN, "Wrong PIN attempt: " + customer.getMobile_num());
                }

                // Generate OTP and send SMS
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(mobileNum);
                if (custOp.equals(DbConstants.OP_CHANGE_MOBILE)) {
                    newOtp.setMobile_num(customerops.getExtra_op_params());
                } else {
                    newOtp.setMobile_num(mobileNum);
                }
                newOtp.setOpcode(custOp);
                BackendOps.generateOtp(newOtp);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.OTP_GENERATED, "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(customerops.getMobile_num(), otp);
                // remove PIN and OTP from the object
                customerops.setPin(null);
                customerops.setOtp(null);
                customerops.setOp_status(DbConstants.CUSTOMER_OP_STATUS_OTP_MATCHED);
                mLogger.debug("OTP matched for given customer operation: " + customerops.getMobile_num() + ", " + customerops.getOp_code());
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch (Exception e) {
            if(positiveException) {
                mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            } else {
                mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_NOK;
                mLogger.error("Exception in CustomerOpsTableEventHandler:beforeCreate: "+e.toString());
            }

            mEdr[BackendConstants.EDR_EXP_CODE_IDX] = e.getMessage();
            if(e instanceof BackendlessException) {
                mEdr[BackendConstants.EDR_EXP_CODE_IDX] = ((BackendlessException) e).getCode();
                throw CommonUtils.getNewException((BackendlessException) e);
            }
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            long execTime = endTime - startTime;
            mEdr[BackendConstants.EDR_END_TIME_IDX] = String.valueOf(endTime);
            mEdr[BackendConstants.EDR_EXEC_DURATION_IDX] = String.valueOf(execTime);
            mLogger.edr(mEdr);
            mLogger.flush();
        }
    }

    @Override
    public void afterCreate( RunnerContext context, CustomerOps customerops, ExecutionResult<CustomerOps> result ) throws Exception
    {
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "CustomerOps-afterCreate";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = customerops.getOp_code()+BackendConstants.BACKEND_EDR_SUB_DELIMETER
                +customerops.getMobile_num()+BackendConstants.BACKEND_EDR_SUB_DELIMETER
                +customerops.getExtra_op_params()+BackendConstants.BACKEND_EDR_SUB_DELIMETER
                +customerops.getQr_card()+BackendConstants.BACKEND_EDR_SUB_DELIMETER
                +customerops.getRequestor_id();

        initCommon();
        try {
            mLogger.debug("In CustomerOpsTableEventHandler: afterCreate"+context.toString());
            HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, context.getUserToken() );

            if(result.getException()==null) {
                String opcode = customerops.getOp_code();
                switch (opcode) {
                    case DbConstants.OP_NEW_CARD:
                        changeCustomerCard(customerops);
                        break;
                    case DbConstants.OP_CHANGE_MOBILE:
                        changeCustomerMobile(customerops);
                        break;
                    case DbConstants.OP_RESET_PIN:
                        resetCustomerPin(customerops.getMobile_num());
                        break;
                }
            } else {
                mLogger.error("Customer op create failed: "+result.getException().toString());
                mEdr[BackendConstants.EDR_EXP_CODE_IDX] = String.valueOf(result.getException().getCode());
                mEdr[BackendConstants.EDR_EXP_CODE_IDX] = result.getException().getExceptionMessage();
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch (Exception e) {
            mLogger.error("Exception in CustomerOpsTableEventHandler:afterCreate: "+e.toString());
            Backendless.Logging.flush();
            if(e instanceof BackendlessException) {
                throw CommonUtils.getNewException((BackendlessException) e);
            }
            throw e;
        }
    }

    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.myecash.services.CustomerOpsTableEventHandler");
        mLogger = new MyLogger(logger);
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
            throw new BackendlessException(BackendResponseCodes.GENERAL_ERROR, "");
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

}*/

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
            throw new BackendlessException(BackendResponseCodes.GENERAL_ERROR, "");
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
            throw new BackendlessException(BackendResponseCodes.GENERAL_ERROR, "");
        }
    }
    */

