package com.myecash.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;

import java.util.Date;

public class MerchantStats
{
  public Integer cust_cnt_no_balance;
  public Integer bill_amt_no_cb;
  public Integer cb_credit;
  private java.util.Date created;
  public Integer cust_cnt_cash;
  private java.util.Date updated;
  private String merchant_id;
  public Integer cash_debit;
  public Integer cb_debit;
  public Integer bill_amt_total;
  public Integer cash_credit;
  private String objectId;
  public Integer cust_cnt_cb_and_cash;
  private String ownerId;
  public Integer cust_cnt_cb;
  private java.util.Date update_time;

  public Date getUpdate_time() {
    return update_time;
  }

  public void setUpdate_time(Date update_time) {
    this.update_time = update_time;
  }

  public Integer getCust_cnt_no_balance()
  {
    return cust_cnt_no_balance;
  }

  public void setCust_cnt_no_balance( Integer cust_cnt_no_balance )
  {
    this.cust_cnt_no_balance = cust_cnt_no_balance;
  }

  public Integer getBill_amt_no_cb()
  {
    return bill_amt_no_cb;
  }

  public void setBill_amt_no_cb( Integer bill_amt_no_cb )
  {
    this.bill_amt_no_cb = bill_amt_no_cb;
  }

  public Integer getCb_credit()
  {
    return cb_credit;
  }

  public void setCb_credit( Integer cb_credit )
  {
    this.cb_credit = cb_credit;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public Integer getCust_cnt_cash()
  {
    return cust_cnt_cash;
  }

  public void setCust_cnt_cash( Integer cust_cnt_cash )
  {
    this.cust_cnt_cash = cust_cnt_cash;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getMerchant_id()
  {
    return merchant_id;
  }

  public void setMerchant_id( String merchant_id )
  {
    this.merchant_id = merchant_id;
  }

  public Integer getCash_debit()
  {
    return cash_debit;
  }

  public void setCash_debit( Integer cash_debit )
  {
    this.cash_debit = cash_debit;
  }

  public Integer getCb_debit()
  {
    return cb_debit;
  }

  public void setCb_debit( Integer cb_debit )
  {
    this.cb_debit = cb_debit;
  }

  public Integer getBill_amt_total()
  {
    return bill_amt_total;
  }

  public void setBill_amt_total( Integer bill_amt_total )
  {
    this.bill_amt_total = bill_amt_total;
  }

  public Integer getCash_credit()
  {
    return cash_credit;
  }

  public void setCash_credit( Integer cash_credit )
  {
    this.cash_credit = cash_credit;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public Integer getCust_cnt_cb_and_cash()
  {
    return cust_cnt_cb_and_cash;
  }

  public void setCust_cnt_cb_and_cash( Integer cust_cnt_cb_and_cash )
  {
    this.cust_cnt_cb_and_cash = cust_cnt_cb_and_cash;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public Integer getCust_cnt_cb()
  {
    return cust_cnt_cb;
  }

  public void setCust_cnt_cb( Integer cust_cnt_cb )
  {
    this.cust_cnt_cb = cust_cnt_cb;
  }

                                                    
  public MerchantStats save()
  {
    return Backendless.Data.of( MerchantStats.class ).save( this );
  }

  public Future<MerchantStats> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantStats> future = new Future<MerchantStats>();
      Backendless.Data.of( MerchantStats.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<MerchantStats> callback )
  {
    Backendless.Data.of( MerchantStats.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( MerchantStats.class ).remove( this );
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
      Backendless.Data.of( MerchantStats.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( MerchantStats.class ).remove( this, callback );
  }

  public static MerchantStats findById( String id )
  {
    return Backendless.Data.of( MerchantStats.class ).findById( id );
  }

  public static Future<MerchantStats> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantStats> future = new Future<MerchantStats>();
      Backendless.Data.of( MerchantStats.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<MerchantStats> callback )
  {
    Backendless.Data.of( MerchantStats.class ).findById( id, callback );
  }

  public static MerchantStats findFirst()
  {
    return Backendless.Data.of( MerchantStats.class ).findFirst();
  }

  public static Future<MerchantStats> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantStats> future = new Future<MerchantStats>();
      Backendless.Data.of( MerchantStats.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<MerchantStats> callback )
  {
    Backendless.Data.of( MerchantStats.class ).findFirst( callback );
  }

  public static MerchantStats findLast()
  {
    return Backendless.Data.of( MerchantStats.class ).findLast();
  }

  public static Future<MerchantStats> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<MerchantStats> future = new Future<MerchantStats>();
      Backendless.Data.of( MerchantStats.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<MerchantStats> callback )
  {
    Backendless.Data.of( MerchantStats.class ).findLast( callback );
  }

  public static BackendlessCollection<MerchantStats> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( MerchantStats.class ).find( query );
  }

  public static Future<BackendlessCollection<MerchantStats>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<MerchantStats>> future = new Future<BackendlessCollection<MerchantStats>>();
      Backendless.Data.of( MerchantStats.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<MerchantStats>> callback )
  {
    Backendless.Data.of( MerchantStats.class ).find( query, callback );
  }
}