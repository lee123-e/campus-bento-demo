package com.neuedu.reggie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName(value = "address_book", schema = "dbo")
public class CustomerAddress {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String contactName;
    private String phone;
    private String deliveryAddress;
    private String label;
    @TableField("is_default")
    private Boolean defaultAddress;

    public CustomerAddress() {
    }

    public CustomerAddress(long customerId, String contactName, String phone, String deliveryAddress,
                           String label, boolean defaultAddress) {
        this.customerId = customerId;
        this.contactName = contactName;
        this.phone = phone;
        this.deliveryAddress = deliveryAddress;
        this.label = label;
        this.defaultAddress = defaultAddress;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getContactName() { return contactName; }
    public String getPhone() { return phone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getLabel() { return label; }
    public Boolean getDefaultAddress() { return defaultAddress; }
}
