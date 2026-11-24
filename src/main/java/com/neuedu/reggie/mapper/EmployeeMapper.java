package com.neuedu.reggie.mapper;

import com.neuedu.reggie.entity.EmployeeAccount;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    @Select("""
            SELECT id, username, display_name AS displayName, password_hash AS passwordHash, enabled
            FROM dbo.employee
            WHERE username = #{username}
            """)
    Optional<EmployeeAccount> findByUsername(@Param("username") String username);

    @Insert("""
            INSERT INTO dbo.employee (username, display_name, password_hash)
            SELECT #{username}, #{displayName}, #{passwordHash}
            WHERE NOT EXISTS (
                SELECT 1 FROM dbo.employee WITH (UPDLOCK, HOLDLOCK) WHERE username = #{username}
            )
            """)
    int insertIfAbsent(@Param("username") String username, @Param("displayName") String displayName,
                       @Param("passwordHash") String passwordHash);
}
