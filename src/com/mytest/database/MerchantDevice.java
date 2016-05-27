package com.mytest.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

import java.util.Date;

public class MerchantDevice
{
  private String device_id;
  private String manufacturer;
  private String os_version;
  private String model;
  private java.util.Date created;
  private String ownerId;
  private String objectId;
  private java.util.Date updated;
  private String os_type;
  private String merchant_id;
  private java.util.Date last_login;

  public Date getLast_login() {
    return last_login;
  }

  public void setLast_login(Date last_login) {
    this.last_login = last_login;
  }


  public String getDevice_id()
  {
    return device_id;
  }

  public void setDevice_id( String device_id )
  {
    this.device_id = device_id;
  }

  public String getManufacturer()
  {
    return manufacturer;
  }

  public void setManufacturer( String manufacturer )
  {
    this.manufacturer = manufacturer;
  }

  public String getOs_version()
  {
    return os_version;
  }

  public void setOs_version( String os_version )
  {
    this.os_version = os_version;
  }

  public String getModel()
  {
    return model;
  }

  public void setModel( String model )
  {
    this.model = model;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getOs_type()
  {
    return os_type;
  }

  public void setOs_type( String os_type )
  {
    this.os_type = os_type;
  }

  public String getMerchant_id()
  {
    return merchant_id;
  }

  public void setMerchant_id( String merchant_id )
  {
    this.merchant_id = merchant_id;
  }

                                                    
  public MerchantDevice save()
  {
    return Backendless.Data.of( MerchantDevice.class ).save( this );
  }

  public Future<MerchantDevice> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantDevice> future = new Future<MerchantDevice>();
      Backendless.Data.of( MerchantDevice.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<MerchantDevice> callback )
  {
    Backendless.Data.of( MerchantDevice.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( MerchantDevice.class ).remove( this );
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
      Backendless.Data.of( MerchantDevice.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( MerchantDevice.class ).remove( this, callback );
  }

  public static MerchantDevice findById( String id )
  {
    return Backendless.Data.of( MerchantDevice.class ).findById( id );
  }

  public static Future<MerchantDevice> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantDevice> future = new Future<MerchantDevice>();
      Backendless.Data.of( MerchantDevice.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<MerchantDevice> callback )
  {
    Backendless.Data.of( MerchantDevice.class ).findById( id, callback );
  }

  public static MerchantDevice findFirst()
  {
    return Backendless.Data.of( MerchantDevice.class ).findFirst();
  }

  public static Future<MerchantDevice> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantDevice> future = new Future<MerchantDevice>();
      Backendless.Data.of( MerchantDevice.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<MerchantDevice> callback )
  {
    Backendless.Data.of( MerchantDevice.class ).findFirst( callback );
  }

  public static MerchantDevice findLast()
  {
    return Backendless.Data.of( MerchantDevice.class ).findLast();
  }

  public static Future<MerchantDevice> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantDevice> future = new Future<MerchantDevice>();
      Backendless.Data.of( MerchantDevice.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<MerchantDevice> callback )
  {
    Backendless.Data.of( MerchantDevice.class ).findLast( callback );
  }

  public static BackendlessCollection<MerchantDevice> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( MerchantDevice.class ).find( query );
  }

  public static Future<BackendlessCollection<MerchantDevice>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<MerchantDevice>> future = new Future<BackendlessCollection<MerchantDevice>>();
      Backendless.Data.of( MerchantDevice.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<MerchantDevice>> callback )
  {
    Backendless.Data.of( MerchantDevice.class ).find( query, callback );
  }
}