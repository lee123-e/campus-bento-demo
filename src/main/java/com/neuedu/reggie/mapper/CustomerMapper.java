package com.neuedu.reggie.mapper;

import com.neuedu.reggie.entity.Customer;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CustomerMapper {

    @Select("SELECT id, phone, nickname FROM dbo.customer WHERE phone = #{phone}")
    Optional<Customer> findByPhone(String phone);

    @Select("SELECT id, phone, nickname FROM dbo.customer WHERE id = #{id}")
    Optional<Customer> findById(long id);

    @Update("UPDATE dbo.customer SET nickname = #{nickname} WHERE id = #{id}")
    void updateNickname(@Param("id") long id, @Param("nickname") String nickname);

    @Insert("""
            INSERT INTO dbo.customer (phone)
            SELECT #{phone}
            WHERE NOT EXISTS (SELECT 1 FROM dbo.customer WITH (UPDLOCK, HOLDLOCK) WHERE phone = #{phone})
            """)
    void insertIfAbsent(String phone);

    @Select("SELECT id FROM dbo.customer WITH (UPDLOCK, HOLDLOCK) WHERE id = #{id}")
    Optional<Long> lockById(long id);
}
