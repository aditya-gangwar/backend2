package com.mytest.models;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

public class BusinessCategories
{
  private String description;
  private String category_name;
  private String objectId;
  private java.util.Date updated;
  private String ownerId;
  private java.util.Date created;
  public String getDescription()
  {
    return description;
  }

  public void setDescription( String description )
  {
    this.description = description;
  }

  public String getCategory_name()
  {
    return category_name;
  }

  public void setCategory_name( String category_name )
  {
    this.category_name = category_name;
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

  public java.util.Date getCreated()
  {
    return created;
  }

                                                    
  public BusinessCategories save()
  {
    return Backendless.Data.of( BusinessCategories.class ).save( this );
  }

  public Future<BusinessCategories> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BusinessCategories> future = new Future<BusinessCategories>();
      Backendless.Data.of( BusinessCategories.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<BusinessCategories> callback )
  {
    Backendless.Data.of( BusinessCategories.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( BusinessCategories.class ).remove( this );
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
      Backendless.Data.of( BusinessCategories.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( BusinessCategories.class ).remove( this, callback );
  }

  public static BusinessCategories findById( String id )
  {
    return Backendless.Data.of( BusinessCategories.class ).findById( id );
  }

  public static Future<BusinessCategories> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BusinessCategories> future = new Future<BusinessCategories>();
      Backendless.Data.of( BusinessCategories.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<BusinessCategories> callback )
  {
    Backendless.Data.of( BusinessCategories.class ).findById( id, callback );
  }

  public static BusinessCategories findFirst()
  {
    return Backendless.Data.of( BusinessCategories.class ).findFirst();
  }

  public static Future<BusinessCategories> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BusinessCategories> future = new Future<BusinessCategories>();
      Backendless.Data.of( BusinessCategories.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<BusinessCategories> callback )
  {
    Backendless.Data.of( BusinessCategories.class ).findFirst( callback );
  }

  public static BusinessCategories findLast()
  {
    return Backendless.Data.of( BusinessCategories.class ).findLast();
  }

  public static Future<BusinessCategories> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BusinessCategories> future = new Future<BusinessCategories>();
      Backendless.Data.of( BusinessCategories.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<BusinessCategories> callback )
  {
    Backendless.Data.of( BusinessCategories.class ).findLast( callback );
  }

  public static BackendlessCollection<BusinessCategories> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( BusinessCategories.class ).find( query );
  }

  public static Future<BackendlessCollection<BusinessCategories>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<BusinessCategories>> future = new Future<BackendlessCollection<BusinessCategories>>();
      Backendless.Data.of( BusinessCategories.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<BusinessCategories>> callback )
  {
    Backendless.Data.of( BusinessCategories.class ).find( query, callback );
  }
}