package com.mytest.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

import java.util.Date;

public class Customers
{
  private String admin_remarks;
  private String txn_pin;
  private String mobile_num;
  private String ownerId;
  private Date updated;
  private Date created;
  private String objectId;
  private Integer admin_status;
  private String name;
  private String private_id;
  private CustomerCards qr_card;
  private String cashback_table;
  private Date temp_blocked_time;

  public Date getTemp_blocked_time() {
    return temp_blocked_time;
  }

  public void setTemp_blocked_time(Date temp_blocked_time) {
    this.temp_blocked_time = temp_blocked_time;
  }

  public String getCashback_table() {
    return cashback_table;
  }

  public void setCashback_table(String cashback_table) {
    this.cashback_table = cashback_table;
  }

  public String getAdmin_remarks()
  {
    return admin_remarks;
  }

  public void setAdmin_remarks( String admin_remarks )
  {
    this.admin_remarks = admin_remarks;
  }

  public String getTxn_pin()
  {
    return txn_pin;
  }

  public void setTxn_pin( String txn_pin )
  {
    this.txn_pin = txn_pin;
  }

  public String getMobile_num()
  {
    return mobile_num;
  }

  public void setMobile_num( String mobile_num )
  {
    this.mobile_num = mobile_num;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public Date getUpdated()
  {
    return updated;
  }

  public Date getCreated()
  {
    return created;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public Integer getAdmin_status()
  {
    return admin_status;
  }

  public void setAdmin_status( Integer admin_status )
  {
    this.admin_status = admin_status;
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public String getPrivate_id()
  {
    return private_id;
  }

  public void setPrivate_id( String private_id )
  {
    this.private_id = private_id;
  }

  public CustomerCards getQr_card()
  {
    return qr_card;
  }

  public void setQr_card( CustomerCards qr_card )
  {
    this.qr_card = qr_card;
  }

                                                    
  public Customers save()
  {
    return Backendless.Data.of( Customers.class ).save( this );
  }

  public Future<Customers> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Customers> future = new Future<Customers>();
      Backendless.Data.of( Customers.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<Customers> callback )
  {
    Backendless.Data.of( Customers.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( Customers.class ).remove( this );
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
      Backendless.Data.of( Customers.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( Customers.class ).remove( this, callback );
  }

  public static Customers findById( String id )
  {
    return Backendless.Data.of( Customers.class ).findById( id );
  }

  public static Future<Customers> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Customers> future = new Future<Customers>();
      Backendless.Data.of( Customers.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<Customers> callback )
  {
    Backendless.Data.of( Customers.class ).findById( id, callback );
  }

  public static Customers findFirst()
  {
    return Backendless.Data.of( Customers.class ).findFirst();
  }

  public static Future<Customers> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Customers> future = new Future<Customers>();
      Backendless.Data.of( Customers.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<Customers> callback )
  {
    Backendless.Data.of( Customers.class ).findFirst( callback );
  }

  public static Customers findLast()
  {
    return Backendless.Data.of( Customers.class ).findLast();
  }

  public static Future<Customers> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Customers> future = new Future<Customers>();
      Backendless.Data.of( Customers.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<Customers> callback )
  {
    Backendless.Data.of( Customers.class ).findLast( callback );
  }

  public static BackendlessCollection<Customers> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( Customers.class ).find( query );
  }

  public static Future<BackendlessCollection<Customers>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<Customers>> future = new Future<BackendlessCollection<Customers>>();
      Backendless.Data.of( Customers.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<Customers>> callback )
  {
    Backendless.Data.of( Customers.class ).find( query, callback );
  }
}