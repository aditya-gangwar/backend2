
package com.myecash.services;

import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import com.myecash.constants.*;
import com.myecash.database.*;
import com.myecash.messaging.SmsHelper;
import com.myecash.utilities.BackendOps;
import com.myecash.utilities.CommonUtils;
import com.myecash.utilities.MyLogger;
import com.myecash.utilities.MyMerchant;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 25-09-2016.
 */
public class CustomerServices implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.CustomerServices");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     * Merchant operations
     */
    public Customers changeMobile(String verifyParam, String newMobile, String otp) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "changeMobile";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = verifyParam+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                newMobile+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                otp;

        boolean positiveException = false;

        try {
            mLogger.debug("In changeMobile: " + verifyParam + "," + newMobile);

            // Fetch merchant with all child - as the same instance is to be returned too
            Customers customer = (Customers) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_CUSTOMER, mEdr, mLogger, true);
            String oldMobile = customer.getMobile_num();

            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // Validate based on given current number
                if (!customer.getTxn_pin().equals(verifyParam)) {
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
                }

                // Generate OTP to verify new mobile number
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(customer.getPrivate_id());
                newOtp.setMobile_num(newMobile);
                newOtp.setOpcode(DbConstants.CUSTOMER_OP_CHANGE_MOBILE);
                BackendOps.generateOtp(newOtp,mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(customer.getPrivate_id(), otp);
                mLogger.debug("OTP matched for given customer operation: " + customer.getPrivate_id());

                // first add record in merchant ops table
                CustomerOps customerOp = new CustomerOps();
                customerOp.setPrivateId(customer.getPrivate_id());
                customerOp.setOp_code(DbConstants.CUSTOMER_OP_CHANGE_MOBILE);
                customerOp.setMobile_num(oldMobile);
                customerOp.setInitiatedBy(DbConstantsBackend.USER_OP_INITBY_CUSTOMER);
                customerOp.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_APP);
                customerOp.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
                // set extra params in presentable format
                String extraParams = "Old Mobile: "+oldMobile+", New Mobile: "+newMobile;
                customerOp.setExtra_op_params(extraParams);
                BackendOps.saveCustomerOp(customerOp);

                // Update with new mobile number
                try {
                    customer.setMobile_num(newMobile);
                    customer = BackendOps.updateCustomer(customer);
                } catch(Exception e) {
                    mLogger.error("changeMobile: Exception while updating customer mobile: "+customer.getPrivate_id());
                    // Rollback - delete customer op added
                    try {
                        BackendOps.deleteCustomerOp(customerOp);
                    } catch(Exception ex) {
                        mLogger.fatal("changeMobile: Failed to rollback: customer op deletion failed: "+customer.getPrivate_id());
                        // Rollback also failed
                        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                        throw ex;
                    }
                    throw e;
                }

                mLogger.debug("Processed mobile change for: " + customer.getPrivate_id());

                // Send SMS on old and new mobile - ignore sent status
                String smsText = SmsHelper.buildMobileChangeSMS(oldMobile, newMobile);
                if(SmsHelper.sendSMS(smsText, oldMobile + "," + newMobile, mLogger)) {
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
                } else {
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return customer;

        } catch(Exception e) {
            CommonUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public List<Cashback> getCashbacks(String custPrivateId, long updatedSince) {

        CommonUtils.initTableToClassMappings();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getCashbacks";

        boolean positiveException = false;
        try {
            mLogger.debug("In getCashbacks");

            // Fetch merchant - send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediatly after
            Object userObj = CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            boolean callByCC = false;
            Customers customer = null;
            if(userType==DbConstants.USER_TYPE_CUSTOMER) {
                customer = (Customers) userObj;
                custPrivateId = customer.getPrivate_id();
                mEdr[BackendConstants.EDR_CUST_ID_IDX] = custPrivateId;

            } else if(userType==DbConstants.USER_TYPE_CC) {
                // fetch merchant
                customer = BackendOps.getCustomer(custPrivateId, BackendConstants.CUSTOMER_ID_PRIVATE_ID, false);
                callByCC = true;
            } else {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "Operation not allowed to this user");
            }

            // not checking for customer account status

            List<Cashback> cbs= null;
            String[] csvFields = customer.getCashback_table().split(CommonConstants.CSV_DELIMETER);

            // fetch cashback records from each table
            for(int i=0; i<csvFields.length; i++) {

                // fetch all CB records for this customer in this table
                String whereClause = "cust_private_id = '" + custPrivateId + "' AND updated > "+updatedSince;
                mLogger.debug("whereClause: "+whereClause);

                ArrayList<Cashback> data = BackendOps.fetchCashback(whereClause,csvFields[i], false, true);
                if (data != null) {
                    if(cbs==null) {
                        cbs= new ArrayList<>();
                    }
                    // dont want to send complete merchant objects
                    // convert the required info into a CSV string
                    // and send as other_details column of the cashback object
                    for (Cashback cb :
                            data) {
                        cb.setOther_details(MyMerchant.toCsvString(cb.getMerchant()));
                    }

                    // add all fetched records from this table to final set
                    cbs.addAll(data);
                }
            }

            if(cbs==null || cbs.size()==0) {
                positiveException = true;
                throw new BackendlessException(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND, "");
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return cbs;

        } catch(Exception e) {
            CommonUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }


}
