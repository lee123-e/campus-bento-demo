package com.neuedu.reggie.mapper;

import com.neuedu.reggie.entity.OrderLine;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderLineMapper {

    @Insert("""
            <script>
            INSERT INTO dbo.order_detail (order_id, product_type, product_id, product_name, unit_price, quantity, flavors_json, meal_contents_json)
            VALUES
            <foreach collection="items" item="item" separator=",">
                (#{orderId}, #{item.type}, #{item.itemId}, #{item.name}, #{item.unitPrice}, #{item.quantity}, #{item.flavorsJson}, #{item.mealContentsJson})
            </foreach>
            </script>
            """)
    void insertForOrder(@Param("orderId") long orderId, @Param("items") List<OrderLine> items);

    @Select("""
            SELECT product_type AS type, product_id AS itemId, product_name AS name,
                   unit_price AS unitPrice, quantity, flavors_json AS flavorsJson,
                   meal_contents_json AS mealContentsJson
            FROM dbo.order_detail WHERE order_id = #{orderId}
            ORDER BY id
            """)
    List<OrderLine> findByOrder(long orderId);
}
