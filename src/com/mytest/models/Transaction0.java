package com.mytest.models;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.geo.GeoPoint;

public class Transaction0
{
  private java.util.Date create_time;
  private String ownerId;
  private String cust_private_id;
  private Integer cl_credit;
  private String cb_percent;
  private Integer cb_credit;
  private String customer_id;
  private Integer cb_debit;
  private java.util.Date created;
  private String trans_id;
  private Boolean archived;
  private Integer cl_debit;
  private Integer cb_billed;
  private String objectId;
  private Integer total_billed;
  private Cashback0 cashback;
  private java.util.Date updated;

  public java.util.Date getCreate_time()
  {
    return this.create_time;
  }

  public String getOwnerId()
  {
    return this.ownerId;
  }

  public String getCust_private_id()
  {
    return this.cust_private_id;
  }

  public Integer getCl_credit()
  {
    return this.cl_credit;
  }

  public String getCb_percent()
  {
    return this.cb_percent;
  }

  public Integer getCb_credit()
  {
    return this.cb_credit;
  }

  public String getCustomer_id()
  {
    return this.customer_id;
  }

  public Integer getCb_debit()
  {
    return this.cb_debit;
  }

  public java.util.Date getCreated()
  {
    return this.created;
  }

  public String getTrans_id()
  {
    return this.trans_id;
  }

  public Boolean getArchived()
  {
    return this.archived;
  }

  public Integer getCl_debit()
  {
    return this.cl_debit;
  }

  public Integer getCb_billed()
  {
    return this.cb_billed;
  }

  public String getObjectId()
  {
    return this.objectId;
  }

  public Integer getTotal_billed()
  {
    return this.total_billed;
  }

  public Cashback0 getCashback()
  {
    return this.cashback;
  }

  public java.util.Date getUpdated()
  {
    return this.updated;
  }


  public void setCreate_time( java.util.Date create_time )
  {
    this.create_time = create_time;
  }

  public void setOwnerId( String ownerId )
  {
    this.ownerId = ownerId;
  }

  public void setCust_private_id( String cust_private_id )
  {
    this.cust_private_id = cust_private_id;
  }

  public void setCl_credit( Integer cl_credit )
  {
    this.cl_credit = cl_credit;
  }

  public void setCb_percent( String cb_percent )
  {
    this.cb_percent = cb_percent;
  }

  public void setCb_credit( Integer cb_credit )
  {
    this.cb_credit = cb_credit;
  }

  public void setCustomer_id( String customer_id )
  {
    this.customer_id = customer_id;
  }

  public void setCb_debit( Integer cb_debit )
  {
    this.cb_debit = cb_debit;
  }

  public void setCreated( java.util.Date created )
  {
    this.created = created;
  }

  public void setTrans_id( String trans_id )
  {
    this.trans_id = trans_id;
  }

  public void setArchived( Boolean archived )
  {
    this.archived = archived;
  }

  public void setCl_debit( Integer cl_debit )
  {
    this.cl_debit = cl_debit;
  }

  public void setCb_billed( Integer cb_billed )
  {
    this.cb_billed = cb_billed;
  }

  public void setObjectId( String objectId )
  {
    this.objectId = objectId;
  }

  public void setTotal_billed( Integer total_billed )
  {
    this.total_billed = total_billed;
  }

  public void setCashback( Cashback0 cashback )
  {
    this.cashback = cashback;
  }

  public void setUpdated( java.util.Date updated )
  {
    this.updated = updated;
  }

  public Transaction0 save()
  {
    return Backendless.Data.of( Transaction0.class ).save( this );
  }

  public Long remove()
  {
    return Backendless.Data.of( Transaction0.class ).remove( this );
  }

  public static Transaction0 findById( String id )
  {
    return Backendless.Data.of( Transaction0.class ).findById( id );
  }

  public static Transaction0 findFirst()
  {
    return Backendless.Data.of( Transaction0.class ).findFirst();
  }

  public static Transaction0 findLast()
  {
    return Backendless.Data.of( Transaction0.class ).findLast();
  }
}