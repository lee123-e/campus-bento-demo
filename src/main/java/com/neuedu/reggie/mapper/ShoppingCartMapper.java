package com.neuedu.reggie.mapper;

import com.neuedu.reggie.dto.cart.CartReadRow;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShoppingCartMapper {

    String QUOTE_ROWS = """
            SELECT c.id, c.product_type AS type, c.product_id AS itemId,
                   COALESCE(d.name, s.name, c.product_name) AS name,
                   c.flavors_json AS flavorsJson, c.quantity,
                   COALESCE(d.price, s.price) AS unitPrice
            FROM dbo.shopping_cart c
            LEFT JOIN dbo.dish d ON c.product_type = 1 AND c.product_id = d.id AND d.on_sale = 1
            LEFT JOIN dbo.setmeal s ON c.product_type = 2 AND c.product_id = s.id AND s.on_sale = 1
            WHERE c.customer_id = #{customerId}
            """;

    @Select(QUOTE_ROWS + " ORDER BY c.id")
    List<CartReadRow> findRows(long customerId);

    @Select(QUOTE_ROWS + " AND c.id = #{id}")
    Optional<CartReadRow> findRow(@Param("customerId") long customerId, @Param("id") long id);

    @Select(QUOTE_ROWS + " AND c.product_type = #{type} AND c.product_id = #{itemId} AND c.flavor_key = #{key}")
    Optional<CartReadRow> findVariant(@Param("customerId") long customerId, @Param("type") int type,
                                      @Param("itemId") long itemId, @Param("key") String key);

    @Insert("""
            INSERT INTO dbo.shopping_cart (customer_id, product_type, product_id, product_name, flavors_json, flavor_key, quantity)
            VALUES (#{customerId}, #{type}, #{itemId}, #{name}, #{flavorsJson}, #{key}, 1)
            """)
    void insert(@Param("customerId") long customerId, @Param("type") int type, @Param("itemId") long itemId,
                @Param("name") String name, @Param("flavorsJson") String flavorsJson, @Param("key") String key);

    @Update("UPDATE dbo.shopping_cart SET quantity = #{quantity} WHERE customer_id = #{customerId} AND id = #{id}")
    void updateQuantity(@Param("customerId") long customerId, @Param("id") long id, @Param("quantity") int quantity);

    @Delete("DELETE FROM dbo.shopping_cart WHERE customer_id = #{customerId} AND id = #{id}")
    int deleteItem(@Param("customerId") long customerId, @Param("id") long id);

    @Delete("DELETE FROM dbo.shopping_cart WHERE customer_id = #{customerId}")
    void clear(long customerId);

    @Select("SELECT COUNT(*) FROM dbo.shopping_cart WHERE customer_id = #{customerId}")
    int count(long customerId);

    @Update("""
            UPDATE dbo.shopping_cart
            SET flavors_json = #{flavorsJson}, flavor_key = #{key}, product_name = #{name}
            WHERE customer_id = #{customerId} AND id = #{id}
            """)
    void replaceVariant(@Param("customerId") long customerId, @Param("id") long id,
                        @Param("flavorsJson") String flavorsJson, @Param("key") String key, @Param("name") String name);

}
