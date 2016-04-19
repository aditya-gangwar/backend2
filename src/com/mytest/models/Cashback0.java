package com.mytest.models;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.geo.GeoPoint;

public class Cashback0
{
  private Integer total_billed;
  private String rowid_qr;
  private String cust_private_id;
  private java.util.Date created;
  private Integer cl_debit;
  private String customer_name;
  private String ownerId;
  private Integer cb_debit;
  private String rowid;
  private BackendlessUser merchant;
  private java.util.Date updated;
  private BackendlessUser customer;
  private Integer cl_credit;
  private String objectId;
  private Integer cb_credit;
  private String merchant_name;

  public String getMerchant_name() {
    return merchant_name;
  }

  public void setMerchant_name(String merchant_name) {
    this.merchant_name = merchant_name;
  }

  public Integer getTotal_billed()
  {
    return this.total_billed;
  }

  public String getRowid_qr()
  {
    return this.rowid_qr;
  }

  public String getCust_private_id()
  {
    return this.cust_private_id;
  }

  public java.util.Date getCreated()
  {
    return this.created;
  }

  public Integer getCl_debit()
  {
    return this.cl_debit;
  }

  public String getCustomer_name()
  {
    return this.customer_name;
  }

  public String getOwnerId()
  {
    return this.ownerId;
  }

  public Integer getCb_debit()
  {
    return this.cb_debit;
  }

  public String getRowid()
  {
    return this.rowid;
  }

  public BackendlessUser getMerchant()
  {
    return this.merchant;
  }

  public java.util.Date getUpdated()
  {
    return this.updated;
  }

  public BackendlessUser getCustomer()
  {
    return this.customer;
  }

  public Integer getCl_credit()
  {
    return this.cl_credit;
  }

  public String getObjectId()
  {
    return this.objectId;
  }

  public Integer getCb_credit()
  {
    return this.cb_credit;
  }


  public void setTotal_billed( Integer total_billed )
  {
    this.total_billed = total_billed;
  }

  public void setRowid_qr( String rowid_qr )
  {
    this.rowid_qr = rowid_qr;
  }

  public void setCust_private_id( String cust_private_id )
  {
    this.cust_private_id = cust_private_id;
  }

  public void setCreated( java.util.Date created )
  {
    this.created = created;
  }

  public void setCl_debit( Integer cl_debit )
  {
    this.cl_debit = cl_debit;
  }

  public void setCustomer_name( String customer_name )
  {
    this.customer_name = customer_name;
  }

  public void setOwnerId( String ownerId )
  {
    this.ownerId = ownerId;
  }

  public void setCb_debit( Integer cb_debit )
  {
    this.cb_debit = cb_debit;
  }

  public void setRowid( String rowid )
  {
    this.rowid = rowid;
  }

  public void setMerchant( BackendlessUser merchant )
  {
    this.merchant = merchant;
  }

  public void setUpdated( java.util.Date updated )
  {
    this.updated = updated;
  }

  public void setCustomer( BackendlessUser customer )
  {
    this.customer = customer;
  }

  public void setCl_credit( Integer cl_credit )
  {
    this.cl_credit = cl_credit;
  }

  public void setObjectId( String objectId )
  {
    this.objectId = objectId;
  }

  public void setCb_credit( Integer cb_credit )
  {
    this.cb_credit = cb_credit;
  }

  public Cashback0 save()
  {
    return Backendless.Data.of( Cashback0.class ).save( this );
  }

  public Long remove()
  {
    return Backendless.Data.of( Cashback0.class ).remove( this );
  }

  public static Cashback0 findById( String id )
  {
    return Backendless.Data.of( Cashback0.class ).findById( id );
  }

  public static Cashback0 findFirst()
  {
    return Backendless.Data.of( Cashback0.class ).findFirst();
  }

  public static Cashback0 findLast()
  {
    return Backendless.Data.of( Cashback0.class ).findLast();
  }
}