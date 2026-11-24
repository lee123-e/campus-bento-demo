package com.neuedu.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.reggie.entity.Dish;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    @Select("""
            <script>
            SELECT id, name, on_sale
            FROM dbo.dish WITH (UPDLOCK, HOLDLOCK)
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY id
            </script>
            """)
    List<Dish> lockForMenuUpdate(@Param("ids") List<Long> ids);
}
