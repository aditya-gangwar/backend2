package in.myecash.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;
import in.myecash.common.database.Future;

public class WrongAttempts
{
  private Integer user_type;
  private String user_id;
  private Integer attempt_cnt;
  private String objectId;
  private String attempt_type;
  private String ownerId;
  private java.util.Date updated;
  private java.util.Date created;
  public Integer getUser_type()
  {
    return user_type;
  }

  public void setUser_type( Integer user_type )
  {
    this.user_type = user_type;
  }

  public String getUser_id()
  {
    return user_id;
  }

  public void setUser_id( String user_id )
  {
    this.user_id = user_id;
  }

  public Integer getAttempt_cnt()
  {
    return attempt_cnt;
  }

  public void setAttempt_cnt( Integer attempt_cnt )
  {
    this.attempt_cnt = attempt_cnt;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public String getAttempt_type()
  {
    return attempt_type;
  }

  public void setAttempt_type( String attempt_type )
  {
    this.attempt_type = attempt_type;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

                                                    
  public WrongAttempts save()
  {
    return Backendless.Data.of( WrongAttempts.class ).save( this );
  }

  public Future<WrongAttempts> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<WrongAttempts> future = new Future<WrongAttempts>();
      Backendless.Data.of( WrongAttempts.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<WrongAttempts> callback )
  {
    Backendless.Data.of( WrongAttempts.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( WrongAttempts.class ).remove( this );
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
      Backendless.Data.of( WrongAttempts.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( WrongAttempts.class ).remove( this, callback );
  }

  public static WrongAttempts findById( String id )
  {
    return Backendless.Data.of( WrongAttempts.class ).findById( id );
  }

  public static Future<WrongAttempts> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<WrongAttempts> future = new Future<WrongAttempts>();
      Backendless.Data.of( WrongAttempts.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<WrongAttempts> callback )
  {
    Backendless.Data.of( WrongAttempts.class ).findById( id, callback );
  }

  public static WrongAttempts findFirst()
  {
    return Backendless.Data.of( WrongAttempts.class ).findFirst();
  }

  public static Future<WrongAttempts> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<WrongAttempts> future = new Future<WrongAttempts>();
      Backendless.Data.of( WrongAttempts.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<WrongAttempts> callback )
  {
    Backendless.Data.of( WrongAttempts.class ).findFirst( callback );
  }

  public static WrongAttempts findLast()
  {
    return Backendless.Data.of( WrongAttempts.class ).findLast();
  }

  public static Future<WrongAttempts> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<WrongAttempts> future = new Future<WrongAttempts>();
      Backendless.Data.of( WrongAttempts.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<WrongAttempts> callback )
  {
    Backendless.Data.of( WrongAttempts.class ).findLast( callback );
  }

  public static BackendlessCollection<WrongAttempts> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( WrongAttempts.class ).find( query );
  }

  public static Future<BackendlessCollection<WrongAttempts>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<WrongAttempts>> future = new Future<BackendlessCollection<WrongAttempts>>();
      Backendless.Data.of( WrongAttempts.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<WrongAttempts>> callback )
  {
    Backendless.Data.of( WrongAttempts.class ).find( query, callback );
  }
}