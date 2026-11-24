package com.neuedu.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.reggie.entity.CustomerOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerOrderMapper extends BaseMapper<CustomerOrder> {
}
