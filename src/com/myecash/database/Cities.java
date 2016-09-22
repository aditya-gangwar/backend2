package com.myecash.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;

public class Cities
{
  private java.util.Date updated;
  private String objectId;
  private String city;
  private String ownerId;
  private java.util.Date created;
  private String state;
  private String countryCode;
  private String cbTableCode;

  public String getCbTableCode() {
    return cbTableCode;
  }

  public void setCbTableCode(String cbTableCode) {
    this.cbTableCode = cbTableCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public String getCity()
  {
    return city;
  }

  public void setCity( String city )
  {
    this.city = city;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getState()
  {
    return state;
  }

  public void setState( String state )
  {
    this.state = state;
  }

                                                    
  public Cities save()
  {
    return Backendless.Data.of( Cities.class ).save( this );
  }

  public Future<Cities> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Cities> future = new Future<Cities>();
      Backendless.Data.of( Cities.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<Cities> callback )
  {
    Backendless.Data.of( Cities.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( Cities.class ).remove( this );
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
      Backendless.Data.of( Cities.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( Cities.class ).remove( this, callback );
  }

  public static Cities findById( String id )
  {
    return Backendless.Data.of( Cities.class ).findById( id );
  }

  public static Future<Cities> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Cities> future = new Future<Cities>();
      Backendless.Data.of( Cities.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<Cities> callback )
  {
    Backendless.Data.of( Cities.class ).findById( id, callback );
  }

  public static Cities findFirst()
  {
    return Backendless.Data.of( Cities.class ).findFirst();
  }

  public static Future<Cities> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Cities> future = new Future<Cities>();
      Backendless.Data.of( Cities.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<Cities> callback )
  {
    Backendless.Data.of( Cities.class ).findFirst( callback );
  }

  public static Cities findLast()
  {
    return Backendless.Data.of( Cities.class ).findLast();
  }

  public static Future<Cities> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Cities> future = new Future<Cities>();
      Backendless.Data.of( Cities.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<Cities> callback )
  {
    Backendless.Data.of( Cities.class ).findLast( callback );
  }

  public static BackendlessCollection<Cities> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( Cities.class ).find( query );
  }

  public static Future<BackendlessCollection<Cities>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<Cities>> future = new Future<BackendlessCollection<Cities>>();
      Backendless.Data.of( Cities.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<Cities>> callback )
  {
    Backendless.Data.of( Cities.class ).find( query, callback );
  }
}