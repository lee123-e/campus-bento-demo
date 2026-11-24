package com.neuedu.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.reggie.entity.CustomerAddress;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerAddressMapper extends BaseMapper<CustomerAddress> {

    @Select("SELECT TOP (1) id FROM dbo.address_book WHERE customer_id = #{customerId} ORDER BY id")
    Optional<Long> findFirstId(long customerId);
}
