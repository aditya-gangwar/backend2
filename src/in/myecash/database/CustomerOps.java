package in.myecash.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;
import in.myecash.common.database.Future;

public class CustomerOps
{
  private String extra_op_params;
  private java.util.Date created;
  private String mobile_num;
  private java.util.Date updated;
  private String qr_card;
  private String ownerId;
  private String requestor_id;
  private String op_code;
  private String objectId;
  private String initiatedBy;
  private String initiatedVia;
  private String ticketNum;
  private String privateId;
  private String op_status;
  private String reason;
  private String remarks;

  public String getInitiatedBy() {
    return initiatedBy;
  }

  public void setInitiatedBy(String initiatedBy) {
    this.initiatedBy = initiatedBy;
  }

  public String getInitiatedVia() {
    return initiatedVia;
  }

  public void setInitiatedVia(String initiatedVia) {
    this.initiatedVia = initiatedVia;
  }

  public String getTicketNum() {
    return ticketNum;
  }

  public void setTicketNum(String ticketNum) {
    this.ticketNum = ticketNum;
  }

  public String getPrivateId() {
    return privateId;
  }

  public void setPrivateId(String privateId) {
    this.privateId = privateId;
  }

  public String getOp_status() {
    return op_status;
  }

  public void setOp_status(String op_status) {
    this.op_status = op_status;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public String getExtra_op_params()
  {
    return extra_op_params;
  }

  public void setExtra_op_params( String extra_op_params )
  {
    this.extra_op_params = extra_op_params;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getMobile_num()
  {
    return mobile_num;
  }

  public void setMobile_num( String mobile_num )
  {
    this.mobile_num = mobile_num;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getQr_card()
  {
    return qr_card;
  }

  public void setQr_card( String qr_card )
  {
    this.qr_card = qr_card;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

  public String getRequestor_id()
  {
    return requestor_id;
  }

  public void setRequestor_id( String requestor_id )
  {
    this.requestor_id = requestor_id;
  }

  public String getOp_code()
  {
    return op_code;
  }

  public void setOp_code( String op_code )
  {
    this.op_code = op_code;
  }

  public String getObjectId()
  {
    return objectId;
  }

                                                    
  public CustomerOps save()
  {
    return Backendless.Data.of( CustomerOps.class ).save( this );
  }

  public Future<CustomerOps> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerOps> future = new Future<CustomerOps>();
      Backendless.Data.of( CustomerOps.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<CustomerOps> callback )
  {
    Backendless.Data.of( CustomerOps.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( CustomerOps.class ).remove( this );
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
      Backendless.Data.of( CustomerOps.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( CustomerOps.class ).remove( this, callback );
  }

  public static CustomerOps findById( String id )
  {
    return Backendless.Data.of( CustomerOps.class ).findById( id );
  }

  public static Future<CustomerOps> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerOps> future = new Future<CustomerOps>();
      Backendless.Data.of( CustomerOps.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<CustomerOps> callback )
  {
    Backendless.Data.of( CustomerOps.class ).findById( id, callback );
  }

  public static CustomerOps findFirst()
  {
    return Backendless.Data.of( CustomerOps.class ).findFirst();
  }

  public static Future<CustomerOps> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerOps> future = new Future<CustomerOps>();
      Backendless.Data.of( CustomerOps.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<CustomerOps> callback )
  {
    Backendless.Data.of( CustomerOps.class ).findFirst( callback );
  }

  public static CustomerOps findLast()
  {
    return Backendless.Data.of( CustomerOps.class ).findLast();
  }

  public static Future<CustomerOps> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerOps> future = new Future<CustomerOps>();
      Backendless.Data.of( CustomerOps.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<CustomerOps> callback )
  {
    Backendless.Data.of( CustomerOps.class ).findLast( callback );
  }

  public static BackendlessCollection<CustomerOps> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( CustomerOps.class ).find( query );
  }

  public static Future<BackendlessCollection<CustomerOps>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<CustomerOps>> future = new Future<BackendlessCollection<CustomerOps>>();
      Backendless.Data.of( CustomerOps.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<CustomerOps>> callback )
  {
    Backendless.Data.of( CustomerOps.class ).find( query, callback );
  }
}