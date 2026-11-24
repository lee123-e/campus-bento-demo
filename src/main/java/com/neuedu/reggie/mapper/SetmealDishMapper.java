package com.neuedu.reggie.mapper;

import com.neuedu.reggie.dto.menu.MealComponentRow;
import com.neuedu.reggie.dto.setmeal.SetmealDishResponse;
import com.neuedu.reggie.entity.SetmealDish;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SetmealDishMapper {

    @Insert("""
            <script>
            INSERT INTO dbo.setmeal_dish (setmeal_id, dish_id, quantity, position)
            VALUES
            <foreach collection="items" item="item" separator=",">
                (#{setmealId}, #{item.dishId}, #{item.quantity}, #{item.position})
            </foreach>
            </script>
            """)
    void insertAll(@Param("setmealId") long setmealId, @Param("items") List<SetmealDish> items);

    @Delete("DELETE FROM dbo.setmeal_dish WHERE setmeal_id = #{setmealId}")
    void deleteBySetmeal(long setmealId);

    @Select("""
            SELECT d.id AS dishId, d.name, d.price, d.on_sale AS onSale, sd.quantity
            FROM dbo.setmeal_dish sd
            JOIN dbo.dish d ON d.id = sd.dish_id
            WHERE sd.setmeal_id = #{setmealId}
            ORDER BY sd.position, sd.dish_id
            """)
    List<SetmealDishResponse> findDetails(long setmealId);

    @Select("""
            <script>
            SELECT DISTINCT dish_id FROM dbo.setmeal_dish WHERE setmeal_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Long> findDishIds(@Param("ids") List<Long> setmealIds);

    @Select("""
            <script>
            SELECT TOP (1) s.name
            FROM dbo.setmeal_dish sd
            JOIN dbo.setmeal s WITH (READCOMMITTEDLOCK) ON s.id = sd.setmeal_id
            WHERE s.on_sale = 1 AND sd.dish_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY s.id
            </script>
            """)
    Optional<String> findActiveSetmealName(@Param("ids") List<Long> dishIds);

    @Select("""
            <script>
            SELECT sd.setmeal_id AS setmealId, d.name, sd.quantity
            FROM dbo.setmeal_dish sd
            JOIN dbo.dish d ON d.id = sd.dish_id
            WHERE sd.setmeal_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY sd.setmeal_id, sd.position, sd.dish_id
            </script>
            """)
    List<MealComponentRow> findMenuContents(@Param("ids") List<Long> ids);

}
