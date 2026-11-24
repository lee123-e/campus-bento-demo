package com.neuedu.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.reggie.entity.MenuImage;
import com.neuedu.reggie.entity.Setmeal;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {

    @Select("SELECT image_media_type AS mediaType, image_contents AS contents FROM dbo.setmeal WHERE id = #{id}")
    Optional<MenuImage> findImage(long id);

    @Select("""
            <script>
            SELECT id, on_sale
            FROM dbo.setmeal WITH (UPDLOCK, HOLDLOCK)
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY id
            </script>
            """)
    List<Setmeal> lockRows(@Param("ids") List<Long> ids);
}
