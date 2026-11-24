package com.neuedu.reggie.mapper;

import com.neuedu.reggie.entity.DishFlavor;
import com.neuedu.reggie.entity.DishFlavorRow;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishFlavorMapper {

    @Select("""
            SELECT group_position, option_position, group_name, option_value
            FROM dbo.dish_flavor
            WHERE dish_id = #{dishId}
            ORDER BY group_position, option_position
            """)
    List<DishFlavor> findByDish(long dishId);

    @Insert("""
            <script>
            INSERT INTO dbo.dish_flavor (dish_id, group_position, option_position, group_name, option_value)
            VALUES
            <foreach collection="flavors" item="flavor" separator=",">
                (#{dishId}, #{flavor.groupPosition}, #{flavor.optionPosition}, #{flavor.name}, #{flavor.option})
            </foreach>
            </script>
            """)
    void insertAll(@Param("dishId") long dishId, @Param("flavors") List<DishFlavor> flavors);

    @Delete("DELETE FROM dbo.dish_flavor WHERE dish_id = #{dishId}")
    void deleteByDish(long dishId);

    @Select("""
            <script>
            SELECT dish_id AS dishId, group_position AS groupPosition, option_position AS optionPosition,
                   group_name AS name, option_value AS [option]
            FROM dbo.dish_flavor
            WHERE dish_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY dish_id, group_position, option_position
            </script>
            """)
    List<DishFlavorRow> findByDishes(@Param("ids") List<Long> ids);

}
