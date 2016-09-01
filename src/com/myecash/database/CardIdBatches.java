package com.myecash.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;

public class CardIdBatches
{
  private java.util.Date statusTime;
  private java.util.Date created;
  private int batchId;
  private String ownerId;
  private String objectId;
  private String rangeId;
  private String rangeBatchId;
  private java.util.Date updated;
  private String status;
  public java.util.Date getStatusTime()
  {
    return statusTime;
  }

  public void setStatusTime( java.util.Date statusTime )
  {
    this.statusTime = statusTime;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public int getBatchId()
  {
    return batchId;
  }

  public void setBatchId(int batchId )
  {
    this.batchId = batchId;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public String getRangeId()
  {
    return rangeId;
  }

  public void setRangeId( String rangeId )
  {
    this.rangeId = rangeId;
  }

  public String getRangeBatchId()
  {
    return rangeBatchId;
  }

  public void setRangeBatchId(String rangeBatchId)
  {
    this.rangeBatchId = rangeBatchId;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getStatus()
  {
    return status;
  }

  public void setStatus( String status )
  {
    this.status = status;
  }

                                                    
  public CardIdBatches save()
  {
    return Backendless.Data.of( CardIdBatches.class ).save( this );
  }

  public Future<CardIdBatches> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CardIdBatches> future = new Future<CardIdBatches>();
      Backendless.Data.of( CardIdBatches.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<CardIdBatches> callback )
  {
    Backendless.Data.of( CardIdBatches.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( CardIdBatches.class ).remove( this );
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
      Backendless.Data.of( CardIdBatches.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( CardIdBatches.class ).remove( this, callback );
  }

  public static CardIdBatches findById(String id )
  {
    return Backendless.Data.of( CardIdBatches.class ).findById( id );
  }

  public static Future<CardIdBatches> findByIdAsync(String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CardIdBatches> future = new Future<CardIdBatches>();
      Backendless.Data.of( CardIdBatches.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<CardIdBatches> callback )
  {
    Backendless.Data.of( CardIdBatches.class ).findById( id, callback );
  }

  public static CardIdBatches findFirst()
  {
    return Backendless.Data.of( CardIdBatches.class ).findFirst();
  }

  public static Future<CardIdBatches> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CardIdBatches> future = new Future<CardIdBatches>();
      Backendless.Data.of( CardIdBatches.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<CardIdBatches> callback )
  {
    Backendless.Data.of( CardIdBatches.class ).findFirst( callback );
  }

  public static CardIdBatches findLast()
  {
    return Backendless.Data.of( CardIdBatches.class ).findLast();
  }

  public static Future<CardIdBatches> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CardIdBatches> future = new Future<CardIdBatches>();
      Backendless.Data.of( CardIdBatches.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<CardIdBatches> callback )
  {
    Backendless.Data.of( CardIdBatches.class ).findLast( callback );
  }

  public static BackendlessCollection<CardIdBatches> find(BackendlessDataQuery query )
  {
    return Backendless.Data.of( CardIdBatches.class ).find( query );
  }

  public static Future<BackendlessCollection<CardIdBatches>> findAsync(BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<CardIdBatches>> future = new Future<BackendlessCollection<CardIdBatches>>();
      Backendless.Data.of( CardIdBatches.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<CardIdBatches>> callback )
  {
    Backendless.Data.of( CardIdBatches.class ).find( query, callback );
  }
}