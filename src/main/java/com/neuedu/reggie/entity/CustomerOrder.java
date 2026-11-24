package com.neuedu.reggie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName(value = "orders", schema = "dbo")
public class CustomerOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String orderNumber;
    private String requestId;
    private Integer status;
    private BigDecimal total;
    private String contactName;
    private String phone;
    private String deliveryAddress;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime dispatchedAt;
    private Long dispatchedBy;
    private LocalDateTime completedAt;
    private Long completedBy;

    public CustomerOrder() {
    }

    public CustomerOrder(long customerId, String orderNumber, String requestId, BigDecimal total,
                         String contactName, String phone, String deliveryAddress, String note, LocalDateTime createdAt) {
        this.customerId = customerId;
        this.orderNumber = orderNumber;
        this.requestId = requestId;
        this.status = OrderStatus.PENDING_PAYMENT.code();
        this.total = total;
        this.contactName = contactName;
        this.phone = phone;
        this.deliveryAddress = deliveryAddress;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getOrderNumber() { return orderNumber; }
    public String getRequestId() { return requestId; }
    public Integer getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public String getContactName() { return contactName; }
    public String getPhone() { return phone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public Long getDispatchedBy() { return dispatchedBy; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Long getCompletedBy() { return completedBy; }
}
