package com.neuedu.reggie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName(value = "category", schema = "dbo")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer categoryType;
    private String name;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    public Category() {
    }

    public Category(int categoryType, String name, int sortOrder, long employeeId, LocalDateTime now) {
        this.categoryType = categoryType;
        this.name = name;
        this.sortOrder = sortOrder;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = employeeId;
        this.updatedBy = employeeId;
    }

    public Long getId() {
        return id;
    }

    public Integer getCategoryType() {
        return categoryType;
    }

    public String getName() {
        return name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }
}
