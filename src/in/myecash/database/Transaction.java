package in.myecash.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;

public class Transaction
{
  private Integer cb_credit;
  private Integer cl_credit;
  private java.util.Date create_time;
  private Integer cb_billed;
  private Integer cl_debit;
  private String cust_private_id;
  private Boolean archived;
  private String ownerId;
  private String customer_id;
  private String objectId;
  private java.util.Date updated;
  private String cpin;
  private String cb_percent;
  private String trans_id;
  private String merchant_id;
  private java.util.Date created;
  private Integer total_billed;
  private Integer cb_debit;
  private Cashback cashback;
  private String merchant_name;
  private String usedCardId;
  private String imgFileName;

  public String getImgFileName() {
    return imgFileName;
  }

  public void setImgFileName(String imgFileName) {
    this.imgFileName = imgFileName;
  }

  public String getUsedCardId() {
    return usedCardId;
  }

  public void setUsedCardId(String usedCardId) {
    this.usedCardId = usedCardId;
  }

  public String getMerchant_name() {
    return merchant_name;
  }

  public void setMerchant_name(String merchant_name) {
    this.merchant_name = merchant_name;
  }

  public Integer getCb_credit()
  {
    return cb_credit;
  }

  public void setCb_credit( Integer cb_credit )
  {
    this.cb_credit = cb_credit;
  }

  public Integer getCl_credit()
  {
    return cl_credit;
  }

  public void setCl_credit( Integer cl_credit )
  {
    this.cl_credit = cl_credit;
  }

  public java.util.Date getCreate_time()
  {
    return create_time;
  }

  public void setCreate_time( java.util.Date create_time )
  {
    this.create_time = create_time;
  }

  public Integer getCb_billed()
  {
    return cb_billed;
  }

  public void setCb_billed( Integer cb_billed )
  {
    this.cb_billed = cb_billed;
  }

  public Integer getCl_debit()
  {
    return cl_debit;
  }

  public void setCl_debit( Integer cl_debit )
  {
    this.cl_debit = cl_debit;
  }

  public String getCust_private_id()
  {
    return cust_private_id;
  }

  public void setCust_private_id( String cust_private_id )
  {
    this.cust_private_id = cust_private_id;
  }

  public Boolean getArchived()
  {
    return archived;
  }

  public void setArchived( Boolean archived )
  {
    this.archived = archived;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public String getCustomer_id()
  {
    return customer_id;
  }

  public void setCustomer_id( String customer_id )
  {
    this.customer_id = customer_id;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getCpin()
  {
    return cpin;
  }

  public void setCpin( String cpin )
  {
    this.cpin = cpin;
  }

  public String getCb_percent()
  {
    return cb_percent;
  }

  public void setCb_percent( String cb_percent )
  {
    this.cb_percent = cb_percent;
  }

  public String getTrans_id()
  {
    return trans_id;
  }

  public void setTrans_id( String trans_id )
  {
    this.trans_id = trans_id;
  }

  public String getMerchant_id()
  {
    return merchant_id;
  }

  public void setMerchant_id( String merchant_id )
  {
    this.merchant_id = merchant_id;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public Integer getTotal_billed()
  {
    return total_billed;
  }

  public void setTotal_billed( Integer total_billed )
  {
    this.total_billed = total_billed;
  }

  public Integer getCb_debit()
  {
    return cb_debit;
  }

  public void setCb_debit( Integer cb_debit )
  {
    this.cb_debit = cb_debit;
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