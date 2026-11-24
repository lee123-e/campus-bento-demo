package com.neuedu.reggie.mapper;

import com.neuedu.reggie.dto.customer.CatalogRow;
import com.neuedu.reggie.dto.customer.OrderableProduct;
import com.neuedu.reggie.entity.MenuImage;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerCatalogMapper {

    String VISIBLE_ITEMS = """
            SELECT category_id, id, 1 AS item_type, name, description, price
            FROM dbo.dish WHERE on_sale = 1
            UNION ALL
            SELECT category_id, id, 2 AS item_type, name, description, price
            FROM dbo.setmeal WHERE on_sale = 1
            """;
    String CATALOG = """
            SELECT c.id AS categoryId, c.name AS categoryName, m.item_type AS type,
                   m.id, m.name, m.description, m.price
            FROM (
            """ + VISIBLE_ITEMS + """
            ) m JOIN dbo.category c ON c.id = m.category_id
            """;

    @Select(CATALOG + " ORDER BY c.sort_order, c.updated_at DESC, c.id, m.id DESC")
    List<CatalogRow> findAvailable();

    @Select(CATALOG + " WHERE m.item_type = #{type} AND m.id = #{id}")
    Optional<CatalogRow> findAvailableItem(@Param("type") int type, @Param("id") long id);

    @Select("""
            SELECT i.media_type AS mediaType, i.contents
            FROM dbo.dish_image i JOIN dbo.dish d ON d.id = i.dish_id
            WHERE d.id = #{id} AND d.on_sale = 1
            """)
    Optional<MenuImage> findDishImage(long id);

    @Select("""
            SELECT image_media_type AS mediaType, image_contents AS contents
            FROM dbo.setmeal WHERE id = #{id} AND on_sale = 1
            """)
    Optional<MenuImage> findSetmealImage(long id);

    @Select("SELECT id, name, price FROM dbo.dish WITH (UPDLOCK, HOLDLOCK) WHERE id = #{id} AND on_sale = 1")
    Optional<OrderableProduct> lockDish(long id);

    @Select("SELECT id, name, price FROM dbo.setmeal WITH (UPDLOCK, HOLDLOCK) WHERE id = #{id} AND on_sale = 1")
    Optional<OrderableProduct> lockSetmeal(long id);
}
