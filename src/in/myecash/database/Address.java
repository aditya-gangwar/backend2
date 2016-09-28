package in.myecash.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;

public class Address
{
  private String line_1;
  private java.util.Date updated;
  private Double longitude;
  private java.util.Date created;
  private String pincode;
  private String objectId;
  private String line_2;
  private Double latitude;
  private String ownerId;
  private String city;
  private String state;

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getLine_1()
  {
    return line_1;
  }

  public void setLine_1( String line_1 )
  {
    this.line_1 = line_1;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public Double getLongitude()
  {
    return longitude;
  }

  public void setLongitude( Double longitude )
  {
    this.longitude = longitude;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getPincode()
  {
    return pincode;
  }

  public void setPincode( String pincode )
  {
    this.pincode = pincode;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public String getLine_2()
  {
    return line_2;
  }

  public void setLine_2( String line_2 )
  {
    this.line_2 = line_2;
  }

  public Double getLatitude()
  {
    return latitude;
  }

  public void setLatitude( Double latitude )
  {
    this.latitude = latitude;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public String getCity()
  {
    return city;
  }

  public void setCity( String city )
  {
    this.city = city;
  }

                                                    
  public Address save()
  {
    return Backendless.Data.of( Address.class ).save( this );
  }

  public Future<Address> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Address> future = new Future<Address>();
      Backendless.Data.of( Address.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<Address> callback )
  {
    Backendless.Data.of( Address.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( Address.class ).remove( this );
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
      Backendless.Data.of( Address.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( Address.class ).remove( this, callback );
  }

  public static Address findById( String id )
  {
    return Backendless.Data.of( Address.class ).findById( id );
  }

  public static Future<Address> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Address> future = new Future<Address>();
      Backendless.Data.of( Address.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<Address> callback )
  {
    Backendless.Data.of( Address.class ).findById( id, callback );
  }

  public static Address findFirst()
  {
    return Backendless.Data.of( Address.class ).findFirst();
  }

  public static Future<Address> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Address> future = new Future<Address>();
      Backendless.Data.of( Address.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<Address> callback )
  {
    Backendless.Data.of( Address.class ).findFirst( callback );
  }

  public static Address findLast()
  {
    return Backendless.Data.of( Address.class ).findLast();
  }

  public static Future<Address> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Address> future = new Future<Address>();
      Backendless.Data.of( Address.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<Address> callback )
  {
    Backendless.Data.of( Address.class ).findLast( callback );
  }

  public static BackendlessCollection<Address> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( Address.class ).find( query );
  }

  public static Future<BackendlessCollection<Address>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<Address>> future = new Future<BackendlessCollection<Address>>();
      Backendless.Data.of( Address.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<Address>> callback )
  {
    Backendless.Data.of( Address.class ).find( query, callback );
  }
}