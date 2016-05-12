package com.mytest.models;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

public class CustomerCards
{
  private Integer status;
  private java.util.Date created;
  private java.util.Date updated;
  private String ownerId;
  private java.util.Date allocationTime;
  private java.util.Date removedTime;
  private String removedReason;
  private String objectId;
  private String qrcode;
  private String merchantId;
  public Integer getStatus()
  {
    return status;
  }

  public void setStatus( Integer status )
  {
    this.status = status;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public java.util.Date getAllocationTime()
  {
    return allocationTime;
  }

  public void setAllocationTime( java.util.Date allocationTime )
  {
    this.allocationTime = allocationTime;
  }

  public java.util.Date getRemovedTime()
  {
    return removedTime;
  }

  public void setRemovedTime( java.util.Date removedTime )
  {
    this.removedTime = removedTime;
  }

  public String getRemovedReason()
  {
    return removedReason;
  }

  public void setRemovedReason( String removedReason )
  {
    this.removedReason = removedReason;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public String getQrcode()
  {
    return qrcode;
  }

  public void setQrcode( String qrcode )
  {
    this.qrcode = qrcode;
  }

  public String getMerchantId()
  {
    return merchantId;
  }

  public void setMerchantId( String merchantId )
  {
    this.merchantId = merchantId;
  }

                                                    
  public CustomerCards save()
  {
    return Backendless.Data.of( CustomerCards.class ).save( this );
  }

  public Future<CustomerCards> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerCards> future = new Future<CustomerCards>();
      Backendless.Data.of( CustomerCards.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<CustomerCards> callback )
  {
    Backendless.Data.of( CustomerCards.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( CustomerCards.class ).remove( this );
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
      Backendless.Data.of( CustomerCards.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( CustomerCards.class ).remove( this, callback );
  }

  public static CustomerCards findById( String id )
  {
    return Backendless.Data.of( CustomerCards.class ).findById( id );
  }

  public static Future<CustomerCards> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerCards> future = new Future<CustomerCards>();
      Backendless.Data.of( CustomerCards.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<CustomerCards> callback )
  {
    Backendless.Data.of( CustomerCards.class ).findById( id, callback );
  }

  public static CustomerCards findFirst()
  {
    return Backendless.Data.of( CustomerCards.class ).findFirst();
  }

  public static Future<CustomerCards> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerCards> future = new Future<CustomerCards>();
      Backendless.Data.of( CustomerCards.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<CustomerCards> callback )
  {
    Backendless.Data.of( CustomerCards.class ).findFirst( callback );
  }

  public static CustomerCards findLast()
  {
    return Backendless.Data.of( CustomerCards.class ).findLast();
  }

  public static Future<CustomerCards> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerCards> future = new Future<CustomerCards>();
      Backendless.Data.of( CustomerCards.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<CustomerCards> callback )
  {
    Backendless.Data.of( CustomerCards.class ).findLast( callback );
  }

  public static BackendlessCollection<CustomerCards> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( CustomerCards.class ).find( query );
  }

  public static Future<BackendlessCollection<CustomerCards>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<CustomerCards>> future = new Future<BackendlessCollection<CustomerCards>>();
      Backendless.Data.of( CustomerCards.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<CustomerCards>> callback )
  {
    Backendless.Data.of( CustomerCards.class ).find( query, callback );
  }
}