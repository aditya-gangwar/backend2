package com.mytest.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

public class Cashback
{
  private Integer cb_debit;
  private String ownerId;
  private String objectId;
  private Integer cl_credit;
  private java.util.Date updated;
  private Integer cb_credit;
  private Integer cl_debit;
  private java.util.Date created;
  //private String rowid_card;
  private Integer total_billed;
  private String rowid;
  private String cust_private_id;
  private String customer_details;
  private String merchant_id;
  private Integer cb_billed;

  public Integer getCb_billed() {
    return cb_billed;
  }

  public void setCb_billed(Integer cb_billed) {
    this.cb_billed = cb_billed;
  }

  public String getMerchant_id() {
    return merchant_id;
  }

  public void setMerchant_id(String merchant_id) {
    this.merchant_id = merchant_id;
  }

  public String getCustomer_details() {
    return customer_details;
  }

  public void setCustomer_details(String customer_details) {
    this.customer_details = customer_details;
  }

  public String getCust_private_id() {
    return cust_private_id;
  }

  public void setCust_private_id(String cust_private_id) {
    this.cust_private_id = cust_private_id;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public Integer getCb_debit()
  {
    return cb_debit;
  }

  public void setCb_debit( Integer cb_debit )
  {
    this.cb_debit = cb_debit;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public Integer getCl_credit()
  {
    return cl_credit;
  }

  public void setCl_credit( Integer cl_credit )
  {
    this.cl_credit = cl_credit;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public Integer getCb_credit()
  {
    return cb_credit;
  }

  public void setCb_credit( Integer cb_credit )
  {
    this.cb_credit = cb_credit;
  }

  public Integer getCl_debit()
  {
    return cl_debit;
  }

  public void setCl_debit( Integer cl_debit )
  {
    this.cl_debit = cl_debit;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  /*
  public String getRowid_card()
  {
    return rowid_card;
  }

  public void setRowid_card( String rowid_card )
  {
    this.rowid_card = rowid_card;
  }
  */

  public Integer getTotal_billed()
  {
    return total_billed;
  }

  public void setTotal_billed( Integer total_billed )
  {
    this.total_billed = total_billed;
  }

  public String getRowid()
  {
    return rowid;
  }

  public void setRowid( String rowid )
  {
    this.rowid = rowid;
  }

  public Cashback save()
  {
    return Backendless.Data.of( Cashback.class ).save( this );
  }

  public Future<Cashback> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Cashback> future = new Future<Cashback>();
      Backendless.Data.of( Cashback.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<Cashback> callback )
  {
    Backendless.Data.of( Cashback.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( Cashback.class ).remove( this );
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
      Backendless.Data.of( Cashback.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( Cashback.class ).remove( this, callback );
  }

  public static Cashback findById( String id )
  {
    return Backendless.Data.of( Cashback.class ).findById( id );
  }

  public static Future<Cashback> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Cashback> future = new Future<Cashback>();
      Backendless.Data.of( Cashback.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<Cashback> callback )
  {
    Backendless.Data.of( Cashback.class ).findById( id, callback );
  }

  public static Cashback findFirst()
  {
    return Backendless.Data.of( Cashback.class ).findFirst();
  }

  public static Future<Cashback> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Cashback> future = new Future<Cashback>();
      Backendless.Data.of( Cashback.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<Cashback> callback )
  {
    Backendless.Data.of( Cashback.class ).findFirst( callback );
  }

  public static Cashback findLast()
  {
    return Backendless.Data.of( Cashback.class ).findLast();
  }

  public static Future<Cashback> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Cashback> future = new Future<Cashback>();
      Backendless.Data.of( Cashback.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<Cashback> callback )
  {
    Backendless.Data.of( Cashback.class ).findLast( callback );
  }

  public static BackendlessCollection<Cashback> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( Cashback.class ).find( query );
  }

  public static Future<BackendlessCollection<Cashback>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<Cashback>> future = new Future<BackendlessCollection<Cashback>>();
      Backendless.Data.of( Cashback.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<Cashback>> callback )
  {
    Backendless.Data.of( Cashback.class ).find( query, callback );
  }
}