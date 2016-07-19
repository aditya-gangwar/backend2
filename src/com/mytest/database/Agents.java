package com.mytest.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

public class Agents
{
  private java.util.Date updated;
  private String id;
  private String mobile_num;
  private Integer status_reason;
  private String objectId;
  private java.util.Date created;
  private String name;
  private Integer admin_status;
  private String dob;
  private String ownerId;
  private String admin_remarks;
  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMobile_num()
  {
    return mobile_num;
  }

  public void setMobile_num( String mobile_num )
  {
    this.mobile_num = mobile_num;
  }

  public Integer getStatus_reason()
  {
    return status_reason;
  }

  public void setStatus_reason( Integer status_reason )
  {
    this.status_reason = status_reason;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public Integer getAdmin_status()
  {
    return admin_status;
  }

  public void setAdmin_status( Integer admin_status )
  {
    this.admin_status = admin_status;
  }

  public String getDob()
  {
    return dob;
  }

  public void setDob( String dob )
  {
    this.dob = dob;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public String getAdmin_remarks()
  {
    return admin_remarks;
  }

  public void setAdmin_remarks( String admin_remarks )
  {
    this.admin_remarks = admin_remarks;
  }

                                                    
  public Agents save()
  {
    return Backendless.Data.of( Agents.class ).save( this );
  }

  public Future<Agents> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Agents> future = new Future<Agents>();
      Backendless.Data.of( Agents.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<Agents> callback )
  {
    Backendless.Data.of( Agents.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( Agents.class ).remove( this );
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
      Backendless.Data.of( Agents.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( Agents.class ).remove( this, callback );
  }

  public static Agents findById( String id )
  {
    return Backendless.Data.of( Agents.class ).findById( id );
  }

  public static Future<Agents> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Agents> future = new Future<Agents>();
      Backendless.Data.of( Agents.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<Agents> callback )
  {
    Backendless.Data.of( Agents.class ).findById( id, callback );
  }

  public static Agents findFirst()
  {
    return Backendless.Data.of( Agents.class ).findFirst();
  }

  public static Future<Agents> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Agents> future = new Future<Agents>();
      Backendless.Data.of( Agents.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<Agents> callback )
  {
    Backendless.Data.of( Agents.class ).findFirst( callback );
  }

  public static Agents findLast()
  {
    return Backendless.Data.of( Agents.class ).findLast();
  }

  public static Future<Agents> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Agents> future = new Future<Agents>();
      Backendless.Data.of( Agents.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<Agents> callback )
  {
    Backendless.Data.of( Agents.class ).findLast( callback );
  }

  public static BackendlessCollection<Agents> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( Agents.class ).find( query );
  }

  public static Future<BackendlessCollection<Agents>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<Agents>> future = new Future<BackendlessCollection<Agents>>();
      Backendless.Data.of( Agents.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<Agents>> callback )
  {
    Backendless.Data.of( Agents.class ).find( query, callback );
  }
}