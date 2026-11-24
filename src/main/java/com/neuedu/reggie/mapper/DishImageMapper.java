package com.neuedu.reggie.mapper;

import com.neuedu.reggie.entity.MenuImage;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DishImageMapper {

    @Select("SELECT media_type, contents FROM dbo.dish_image WHERE dish_id = #{dishId}")
    Optional<MenuImage> findByDish(long dishId);

    @Insert("""
            INSERT INTO dbo.dish_image (dish_id, media_type, contents)
            VALUES (#{dishId}, #{image.mediaType}, #{image.contents})
            """)
    void insert(@Param("dishId") long dishId, @Param("image") MenuImage image);

    @Update("""
            UPDATE dbo.dish_image
            SET media_type = #{image.mediaType}, contents = #{image.contents}
            WHERE dish_id = #{dishId}
            """)
    int update(@Param("dishId") long dishId, @Param("image") MenuImage image);
}
