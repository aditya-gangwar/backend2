package com.mytest.models;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

import java.io.Serializable;
import java.util.Date;

public class Transaction implements Serializable
{
  private String ownerId;
  private Integer cl_credit;
  private String cb_percent;
  private Integer cb_credit;
  private Integer cb_debit;
  private Date created;
  private Integer cl_debit;
  private Integer cb_billed;
  private String objectId;
  private Integer total_billed;
  private Date updated;
  private Cashback cashback;
  private String trans_id;
  private String customer_id;
  private String cust_private_id;

  public Date getCreate_time() {
    return create_time;
  }

  public void setCreate_time(Date create_time) {
    this.create_time = create_time;
  }

  private Date create_time;

  public String getCust_private_id() {
    return cust_private_id;
  }

  public void setCust_private_id(String cust_private_id) {
    this.cust_private_id = cust_private_id;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public String getCustomer_id() {
    return customer_id;
  }

  public void setCustomer_id(String customer_id) {
    this.customer_id = customer_id;
  }

  public String getTrans_id()
  {
    return trans_id;
  }

  public void setTrans_id( String trans_id )
  {
    this.trans_id = trans_id;
  }

  public Integer getCl_credit()
  {
    return cl_credit;
  }

  public void setCl_credit( Integer cl_credit )
  {
    this.cl_credit = cl_credit;
  }

  public String getCb_percent()
  {
    return cb_percent;
  }

  public void setCb_percent( String cb_percent )
  {
    this.cb_percent = cb_percent;
  }

  public Integer getCb_credit()
  {
    return cb_credit;
  }

  public void setCb_credit( Integer cb_credit )
  {
    this.cb_credit = cb_credit;
  }

  public Integer getCb_debit()
  {
    return cb_debit;
  }

  public void setCb_debit( Integer cb_debit )
  {
    this.cb_debit = cb_debit;
  }

  public Date getCreated()
  {
    return created;
  }

  public Integer getCl_debit()
  {
    return cl_debit;
  }

  public void setCl_debit( Integer cl_debit )
  {
    this.cl_debit = cl_debit;
  }

  public Integer getCb_billed()
  {
    return cb_billed;
  }

  public void setCb_billed( Integer cb_billed )
  {
    this.cb_billed = cb_billed;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public Integer getTotal_billed()
  {
    return total_billed;
  }

  public void setTotal_billed( Integer total_billed )
  {
    this.total_billed = total_billed;
  }

  public Date getUpdated()
  {
    return updated;
  }

  public Cashback getCashback()
  {
    return cashback;
  }

  public void setCashback( Cashback cashback )
  {
    this.cashback = cashback;
  }


  public Transaction save()
  {
    return Backendless.Data.of( Transaction.class ).save( this );
  }

  public Future<Transaction> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Transaction> future = new Future<Transaction>();
      Backendless.Data.of( Transaction.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<Transaction> callback )
  {
    Backendless.Data.of( Transaction.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( Transaction.class ).remove( this );
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
      Backendless.Data.of( Transaction.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( Transaction.class ).remove( this, callback );
  }

  public static Transaction findById( String id )
  {
    return Backendless.Data.of( Transaction.class ).findById( id );
  }

  public static Future<Transaction> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Transaction> future = new Future<Transaction>();
      Backendless.Data.of( Transaction.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<Transaction> callback )
  {
    Backendless.Data.of( Transaction.class ).findById( id, callback );
  }

  public static Transaction findFirst()
  {
    return Backendless.Data.of( Transaction.class ).findFirst();
  }

  public static Future<Transaction> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Transaction> future = new Future<Transaction>();
      Backendless.Data.of( Transaction.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<Transaction> callback )
  {
    Backendless.Data.of( Transaction.class ).findFirst( callback );
  }

  public static Transaction findLast()
  {
    return Backendless.Data.of( Transaction.class ).findLast();
  }

  public static Future<Transaction> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<Transaction> future = new Future<Transaction>();
      Backendless.Data.of( Transaction.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<Transaction> callback )
  {
    Backendless.Data.of( Transaction.class ).findLast( callback );
  }

  public static BackendlessCollection<Transaction> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( Transaction.class ).find( query );
  }

  public static Future<BackendlessCollection<Transaction>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<Transaction>> future = new Future<BackendlessCollection<Transaction>>();
      Backendless.Data.of( Transaction.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<Transaction>> callback )
  {
    Backendless.Data.of( Transaction.class ).find( query, callback );
  }
}