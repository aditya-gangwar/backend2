package com.mytest.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

public class Merchants
{
  private String name;
  private Double longitude_reg;
  private String objectId;
  private java.util.Date temp_pswd_time;
  private Boolean cl_add_enable;
  private String ownerId;
  private java.util.Date last_archive;
  private String address_pincode;
  private String address_line;
  private java.util.Date created;
  private String image_url;
  private java.util.Date updated;
  private String cashback_table;
  private String mobile_num;
  private String email;
  private String admin_remarks;
  private String txn_table;
  private String auto_id;
  private Integer admin_status;
  private Double latitude_reg;
  private String cb_rate;
  private java.util.List<MerchantDevice> trusted_devices;
  private Cities city;
  private BusinessCategories buss_category;
  private Integer passwd_wrong_attempts;
  private String tempDevId;

  public String getTempDevId() {
    return tempDevId;
  }

  public void setTempDevId(String tempDevId) {
    this.tempDevId = tempDevId;
  }

  public Integer getPasswd_wrong_attempts() {
    return passwd_wrong_attempts;
  }

  public void setPasswd_wrong_attempts(Integer passwd_wrong_attempts) {
    this.passwd_wrong_attempts = passwd_wrong_attempts;
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public Double getLongitude_reg()
  {
    return longitude_reg;
  }

  public void setLongitude_reg( Double longitude_reg )
  {
    this.longitude_reg = longitude_reg;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public java.util.Date getTemp_pswd_time()
  {
    return temp_pswd_time;
  }

  public void setTemp_pswd_time( java.util.Date temp_pswd_time )
  {
    this.temp_pswd_time = temp_pswd_time;
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

  public java.util.Date getLast_archive()
  {
    return last_archive;
  }

  public void setLast_archive( java.util.Date last_archive )
  {
    this.last_archive = last_archive;
  }

  public String getAddress_pincode()
  {
    return address_pincode;
  }

  public void setAddress_pincode( String address_pincode )
  {
    this.address_pincode = address_pincode;
  }

  public String getAddress_line()
  {
    return address_line;
  }

  public void setAddress_line( String address_line )
  {
    this.address_line = address_line;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getImage_url()
  {
    return image_url;
  }

  public void setImage_url( String image_url )
  {
    this.image_url = image_url;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

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

  public String getAdmin_remarks()
  {
    return admin_remarks;
  }

  public void setAdmin_remarks( String admin_remarks )
  {
    this.admin_remarks = admin_remarks;
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

  public Integer getAdmin_status()
  {
    return admin_status;
  }

  public void setAdmin_status( Integer admin_status )
  {
    this.admin_status = admin_status;
  }

  public Double getLatitude_reg()
  {
    return latitude_reg;
  }

  public void setLatitude_reg( Double latitude_reg )
  {
    this.latitude_reg = latitude_reg;
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

  public Cities getCity()
  {
    return city;
  }

  public void setCity( Cities city )
  {
    this.city = city;
  }

  public BusinessCategories getBuss_category()
  {
    return buss_category;
  }

  public void setBuss_category( BusinessCategories buss_category )
  {
    this.buss_category = buss_category;
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