package com.mytest.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

public class GlobalSettings
{
  private String objectId;
  private Integer cb_pin_threshold_nocard;
  private Integer cl_pin_threshold_card;
  private java.util.Date created;
  private String ownerId;
  private java.util.Date updated;
  private Integer cb_table_pool_start;
  private Integer cb_redeem_limit;
  private Integer reports_blackout_end_hour;
  private java.util.Date service_disabled_until;
  private Integer txn_table_pool_size;
  private Integer cl_pin_threshold_nocard;
  private Integer cb_table_pool_size;
  private Integer cb_pin_threshold_card;
  private Integer temp_passwd_expiry_mins;
  private Integer txn_table_pool_start;
  private Integer report_download_old_days;
  private Integer reports_blackout_start_hour;
  private Integer temp_card_block_duration_hrs;
  private Integer wrong_pin_attempts;

  public Integer getWrong_pin_attempts() {
    return wrong_pin_attempts;
  }

  public Integer getTemp_card_block_duration_hrs() {
    return temp_card_block_duration_hrs;
  }

  public Integer getCb_pin_threshold_nocard()
  {
    return cb_pin_threshold_nocard;
  }

  public void setCb_pin_threshold_nocard( Integer cb_pin_threshold_nocard )
  {
    this.cb_pin_threshold_nocard = cb_pin_threshold_nocard;
  }

  public Integer getCl_pin_threshold_card()
  {
    return cl_pin_threshold_card;
  }

  public void setCl_pin_threshold_card( Integer cl_pin_threshold_card )
  {
    this.cl_pin_threshold_card = cl_pin_threshold_card;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public Integer getCb_table_pool_start()
  {
    return cb_table_pool_start;
  }

  public void setCb_table_pool_start( Integer cb_table_pool_start )
  {
    this.cb_table_pool_start = cb_table_pool_start;
  }

  public Integer getCb_redeem_limit()
  {
    return cb_redeem_limit;
  }

  public void setCb_redeem_limit( Integer cb_redeem_limit )
  {
    this.cb_redeem_limit = cb_redeem_limit;
  }

  public Integer getReports_blackout_end_hour()
  {
    return reports_blackout_end_hour;
  }

  public void setReports_blackout_end_hour( Integer reports_blackout_end_hour )
  {
    this.reports_blackout_end_hour = reports_blackout_end_hour;
  }

  public java.util.Date getService_disabled_until()
  {
    return service_disabled_until;
  }

  public void setService_disabled_until( java.util.Date service_disabled_until )
  {
    this.service_disabled_until = service_disabled_until;
  }

  public Integer getTxn_table_pool_size()
  {
    return txn_table_pool_size;
  }

  public void setTxn_table_pool_size( Integer txn_table_pool_size )
  {
    this.txn_table_pool_size = txn_table_pool_size;
  }

  public Integer getCl_pin_threshold_nocard()
  {
    return cl_pin_threshold_nocard;
  }

  public void setCl_pin_threshold_nocard( Integer cl_pin_threshold_nocard )
  {
    this.cl_pin_threshold_nocard = cl_pin_threshold_nocard;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public Integer getCb_table_pool_size()
  {
    return cb_table_pool_size;
  }

  public void setCb_table_pool_size( Integer cb_table_pool_size )
  {
    this.cb_table_pool_size = cb_table_pool_size;
  }

  public Integer getCb_pin_threshold_card()
  {
    return cb_pin_threshold_card;
  }

  public void setCb_pin_threshold_card( Integer cb_pin_threshold_card )
  {
    this.cb_pin_threshold_card = cb_pin_threshold_card;
  }

  public Integer getTemp_passwd_expiry_mins()
  {
    return temp_passwd_expiry_mins;
  }

  public void setTemp_passwd_expiry_mins( Integer temp_passwd_expiry_mins )
  {
    this.temp_passwd_expiry_mins = temp_passwd_expiry_mins;
  }

  public Integer getTxn_table_pool_start()
  {
    return txn_table_pool_start;
  }

  public void setTxn_table_pool_start( Integer txn_table_pool_start )
  {
    this.txn_table_pool_start = txn_table_pool_start;
  }

  public Integer getReport_download_old_days()
  {
    return report_download_old_days;
  }

  public void setReport_download_old_days( Integer report_download_old_days )
  {
    this.report_download_old_days = report_download_old_days;
  }

  public Integer getReports_blackout_start_hour()
  {
    return reports_blackout_start_hour;
  }

  public void setReports_blackout_start_hour( Integer reports_blackout_start_hour )
  {
    this.reports_blackout_start_hour = reports_blackout_start_hour;
  }

                                                    
  public GlobalSettings save()
  {
    return Backendless.Data.of( GlobalSettings.class ).save( this );
  }

  public Future<GlobalSettings> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<GlobalSettings> future = new Future<GlobalSettings>();
      Backendless.Data.of( GlobalSettings.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<GlobalSettings> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( GlobalSettings.class ).remove( this );
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
      Backendless.Data.of( GlobalSettings.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).remove( this, callback );
  }

  public static GlobalSettings findById( String id )
  {
    return Backendless.Data.of( GlobalSettings.class ).findById( id );
  }

  public static Future<GlobalSettings> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<GlobalSettings> future = new Future<GlobalSettings>();
      Backendless.Data.of( GlobalSettings.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<GlobalSettings> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).findById( id, callback );
  }

  public static GlobalSettings findFirst()
  {
    return Backendless.Data.of( GlobalSettings.class ).findFirst();
  }

  public static Future<GlobalSettings> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<GlobalSettings> future = new Future<GlobalSettings>();
      Backendless.Data.of( GlobalSettings.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<GlobalSettings> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).findFirst( callback );
  }

  public static GlobalSettings findLast()
  {
    return Backendless.Data.of( GlobalSettings.class ).findLast();
  }

  public static Future<GlobalSettings> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<GlobalSettings> future = new Future<GlobalSettings>();
      Backendless.Data.of( GlobalSettings.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<GlobalSettings> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).findLast( callback );
  }

  public static BackendlessCollection<GlobalSettings> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( GlobalSettings.class ).find( query );
  }

  public static Future<BackendlessCollection<GlobalSettings>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<GlobalSettings>> future = new Future<BackendlessCollection<GlobalSettings>>();
      Backendless.Data.of( GlobalSettings.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<GlobalSettings>> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).find( query, callback );
  }
}