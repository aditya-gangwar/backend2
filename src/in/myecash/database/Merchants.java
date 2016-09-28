package in.myecash.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;

import java.util.Date;

public class Merchants
{
  private String name;
  private String objectId;
  private Boolean cl_add_enable;
  private String ownerId;
  private java.util.Date last_txn_archive;
  private Integer admin_status;
  private java.util.Date created;
  private String displayImage;
  private java.util.Date updated;
  private java.util.Date status_update_time;
  private String status_reason;
  //private String admin_remarks;
  private String cashback_table;
  private String mobile_num;
  private String email;
  private String txn_table;
  private String auto_id;
  private String tempDevId;
  private String cb_rate;
  private java.util.List<MerchantDevice> trusted_devices;
  private BusinessCategories buss_category;
  private Address address;
  private Integer cl_debit_limit_for_pin;
  private Integer cb_debit_limit_for_pin;
  private Integer cl_credit_limit_for_pin;
  private Boolean first_login_ok;
  private Boolean debugLogs;
  private String dob;
  private String agentId;
  private Date lastRenewDate;

  public Date getLastRenewDate() {
    return lastRenewDate;
  }

  public void setLastRenewDate(Date lastRenewDate) {
    this.lastRenewDate = lastRenewDate;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getDob() {
    return dob;
  }

  public void setDob(String dob) {
    this.dob = dob;
  }

  /*
  private java.util.Date lastLogin;

  public java.util.Date getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(java.util.Date lastLogin) {
    this.lastLogin = lastLogin;
  }
  */

  public Boolean getDebugLogs() {
    return debugLogs;
  }

  public void setDebugLogs(Boolean debugLogs) {
    this.debugLogs = debugLogs;
  }

  public Boolean getFirst_login_ok() {
    return first_login_ok;
  }

  public void setFirst_login_ok(Boolean first_login_ok) {
    this.first_login_ok = first_login_ok;
  }

  public Integer getCl_debit_limit_for_pin() {
    return cl_debit_limit_for_pin;
  }

  public Integer getCb_debit_limit_for_pin() {
    return cb_debit_limit_for_pin;
  }

  public Integer getCl_credit_limit_for_pin() {
    return cl_credit_limit_for_pin;
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public Boolean getCl_add_enable()
  {
    return cl_add_enable;
  }

  public void setCl_add_enable( Boolean cl_add_enable )
  {
    this.cl_add_enable = cl_add_enable;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public java.util.Date getLast_txn_archive()
  {
    return last_txn_archive;
  }

  public void setLast_txn_archive( java.util.Date last_txn_archive )
  {
    this.last_txn_archive = last_txn_archive;
  }

  public Integer getAdmin_status()
  {
    return admin_status;
  }

  public void setAdmin_status( Integer admin_status )
  {
    this.admin_status = admin_status;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getDisplayImage()
  {
    return displayImage;
  }

  public void setDisplayImage(String displayImage)
  {
    this.displayImage = displayImage;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public java.util.Date getStatus_update_time()
  {
    return status_update_time;
  }

  public void setStatus_update_time( java.util.Date status_update_time )
  {
    this.status_update_time = status_update_time;
  }

  public String getStatus_reason()
  {
    return status_reason;
  }

  public void setStatus_reason( String status_reason )
  {
    this.status_reason = status_reason;
  }

  /*public String getAdmin_remarks()
  {
    return admin_remarks;
  }

  public void setAdmin_remarks( String admin_remarks )
  {
    this.admin_remarks = admin_remarks;
  }
  */

  public String getCashback_table()
  {
    return cashback_table;
  }

  public void setCashback_table( String cashback_table )
  {
    this.cashback_table = cashback_table;
  }

  public String getMobile_num()
  {
    return mobile_num;
  }

  public void setMobile_num( String mobile_num )
  {
    this.mobile_num = mobile_num;
  }

  public String getEmail()
  {
    return email;
  }

  public void setEmail( String email )
  {
    this.email = email;
  }

  public String getTxn_table()
  {
    return txn_table;
  }

  public void setTxn_table( String txn_table )
  {
    this.txn_table = txn_table;
  }

  public String getAuto_id()
  {
    return auto_id;
  }

  public void setAuto_id( String auto_id )
  {
    this.auto_id = auto_id;
  }

  public String getTempDevId()
  {
    return tempDevId;
  }

  public void setTempDevId( String tempDevId )
  {
    this.tempDevId = tempDevId;
  }

  public String getCb_rate()
  {
    return cb_rate;
  }

  public void setCb_rate( String cb_rate )
  {
    this.cb_rate = cb_rate;
  }

  public java.util.List<MerchantDevice> getTrusted_devices()
  {
    return trusted_devices;
  }

  public void setTrusted_devices( java.util.List<MerchantDevice> trusted_devices )
  {
    this.trusted_devices = trusted_devices;
  }

  public BusinessCategories getBuss_category()
  {
    return buss_category;
  }

  public void setBuss_category( BusinessCategories buss_category )
  {
    this.buss_category = buss_category;
  }

  public Address getAddress()
  {
    return address;
  }

  public void setAddress( Address address )
  {
    this.address = address;
  }

                                                    
  public Merchants save()
  {
    return Backendless.Data.of( Merchants.class ).save( this );
  }

  public Future<Merchants> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Merchants> future = new Future<Merchants>();
      Backendless.Data.of( Merchants.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<Merchants> callback )
  {
    Backendless.Data.of( Merchants.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( Merchants.class ).remove( this );
  }

  public Future<Long> removeAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Long> future = new Future<Long>();
      Backendless.Data.of( Merchants.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( Merchants.class ).remove( this, callback );
  }

  public static Merchants findById( String id )
  {
    return Backendless.Data.of( Merchants.class ).findById( id );
  }

  public static Future<Merchants> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Merchants> future = new Future<Merchants>();
      Backendless.Data.of( Merchants.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<Merchants> callback )
  {
    Backendless.Data.of( Merchants.class ).findById( id, callback );
  }

  public static Merchants findFirst()
  {
    return Backendless.Data.of( Merchants.class ).findFirst();
  }

  public static Future<Merchants> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Merchants> future = new Future<Merchants>();
      Backendless.Data.of( Merchants.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<Merchants> callback )
  {
    Backendless.Data.of( Merchants.class ).findFirst( callback );
  }

  public static Merchants findLast()
  {
    return Backendless.Data.of( Merchants.class ).findLast();
  }

  public static Future<Merchants> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Merchants> future = new Future<Merchants>();
      Backendless.Data.of( Merchants.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<Merchants> callback )
  {
    Backendless.Data.of( Merchants.class ).findLast( callback );
  }

  public static BackendlessCollection<Merchants> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( Merchants.class ).find( query );
  }

  public static Future<BackendlessCollection<Merchants>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<Merchants>> future = new Future<BackendlessCollection<Merchants>>();
      Backendless.Data.of( Merchants.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<Merchants>> callback )
  {
    Backendless.Data.of( Merchants.class ).find( query, callback );
  }
}