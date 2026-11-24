package com.neuedu.reggie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName(value = "setmeal", schema = "dbo")
public class Setmeal {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String name;
    private BigDecimal price;
    private String description;
    private Boolean onSale;
    @TableField(select = false)
    private String imageMediaType;
    @TableField(select = false)
    private byte[] imageContents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    public Setmeal() {
    }

    public Setmeal(long categoryId, String name, BigDecimal price, String description,
                   MenuImage image, long employeeId, LocalDateTime now) {
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.onSale = false;
        this.imageMediaType = image.mediaType();
        this.imageContents = image.contents();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = employeeId;
        this.updatedBy = employeeId;
    }

    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getDescription() { return description; }
    public Boolean getOnSale() { return onSale; }
    public String getImageMediaType() { return imageMediaType; }
    public byte[] getImageContents() { return imageContents; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }
}
