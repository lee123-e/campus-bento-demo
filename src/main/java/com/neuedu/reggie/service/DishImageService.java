package com.neuedu.reggie.service;

import com.neuedu.reggie.entity.MenuImage;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.DishImageMapper;
import org.springframework.stereotype.Service;

@Service
public class DishImageService {

    private final DishImageMapper images;

    public DishImageService(DishImageMapper images) {
        this.images = images;
    }

    public MenuImage find(long dishId) {
        return images.findByDish(dishId)
                .orElseThrow(() -> new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "没有找到这张菜品图片。"));
    }

    public void save(long dishId, MenuImage image) {
        if (images.update(dishId, image) == 0) {
            images.insert(dishId, image);
        }
    }
}
