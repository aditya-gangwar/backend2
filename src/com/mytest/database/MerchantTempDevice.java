package com.mytest.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

public class MerchantTempDevice
{
  private String temp_device_id;
  private String merchant_id;
  private java.util.Date created;
  private String objectId;
  private java.util.Date updated;
  private String ownerId;
  public String getTemp_device_id()
  {
    return temp_device_id;
  }

  public void setTemp_device_id( String temp_device_id )
  {
    this.temp_device_id = temp_device_id;
  }

  public String getMerchant_id()
  {
    return merchant_id;
  }

  public void setMerchant_id( String merchant_id )
  {
    this.merchant_id = merchant_id;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

                                                    
  public MerchantTempDevice save()
  {
    return Backendless.Data.of( MerchantTempDevice.class ).save( this );
  }

  public Future<MerchantTempDevice> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantTempDevice> future = new Future<MerchantTempDevice>();
      Backendless.Data.of( MerchantTempDevice.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<MerchantTempDevice> callback )
  {
    Backendless.Data.of( MerchantTempDevice.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( MerchantTempDevice.class ).remove( this );
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
      Backendless.Data.of( MerchantTempDevice.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( MerchantTempDevice.class ).remove( this, callback );
  }

  public static MerchantTempDevice findById( String id )
  {
    return Backendless.Data.of( MerchantTempDevice.class ).findById( id );
  }

  public static Future<MerchantTempDevice> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantTempDevice> future = new Future<MerchantTempDevice>();
      Backendless.Data.of( MerchantTempDevice.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<MerchantTempDevice> callback )
  {
    Backendless.Data.of( MerchantTempDevice.class ).findById( id, callback );
  }

  public static MerchantTempDevice findFirst()
  {
    return Backendless.Data.of( MerchantTempDevice.class ).findFirst();
  }

  public static Future<MerchantTempDevice> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantTempDevice> future = new Future<MerchantTempDevice>();
      Backendless.Data.of( MerchantTempDevice.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<MerchantTempDevice> callback )
  {
    Backendless.Data.of( MerchantTempDevice.class ).findFirst( callback );
  }

  public static MerchantTempDevice findLast()
  {
    return Backendless.Data.of( MerchantTempDevice.class ).findLast();
  }

  public static Future<MerchantTempDevice> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantTempDevice> future = new Future<MerchantTempDevice>();
      Backendless.Data.of( MerchantTempDevice.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<MerchantTempDevice> callback )
  {
    Backendless.Data.of( MerchantTempDevice.class ).findLast( callback );
  }

  public static BackendlessCollection<MerchantTempDevice> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( MerchantTempDevice.class ).find( query );
  }

  public static Future<BackendlessCollection<MerchantTempDevice>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<MerchantTempDevice>> future = new Future<BackendlessCollection<MerchantTempDevice>>();
      Backendless.Data.of( MerchantTempDevice.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<MerchantTempDevice>> callback )
  {
    Backendless.Data.of( MerchantTempDevice.class ).find( query, callback );
  }
}