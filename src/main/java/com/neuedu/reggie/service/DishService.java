package com.neuedu.reggie.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.reggie.dto.dish.DishDetailResponse;
import com.neuedu.reggie.dto.dish.DishFlavorGroup;
import com.neuedu.reggie.dto.dish.DishPageResponse;
import com.neuedu.reggie.dto.dish.DishRowResponse;
import com.neuedu.reggie.dto.dish.DishWriteRequest;
import com.neuedu.reggie.entity.Dish;
import com.neuedu.reggie.entity.DishFlavor;
import com.neuedu.reggie.entity.MenuImage;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.DishFlavorMapper;
import com.neuedu.reggie.mapper.DishMapper;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishService {

    private final DishMapper dishes;
    private final DishFlavorMapper flavors;
    private final DishImageService images;
    private final MenuImageValidator imageValidator;
    private final MenuAvailabilityService availability;
    private final CategoryService categories;
    private final EmployeeService employees;

    public DishService(DishMapper dishes, DishFlavorMapper flavors, DishImageService images,
                       CategoryService categories, EmployeeService employees, MenuImageValidator imageValidator, MenuAvailabilityService availability) {
        this.dishes = dishes;
        this.flavors = flavors;
        this.images = images;
        this.imageValidator = imageValidator;
        this.availability = availability;
        this.categories = categories;
        this.employees = employees;
    }

    public DishPageResponse findPage(int page, int size, String name, Long categoryId) {
        String query = name == null ? "" : name.strip();
        Page<Dish> result = dishes.selectPage(new Page<>(page, size), Wrappers.<Dish>lambdaQuery()
                .like(!query.isEmpty(), Dish::getName, query)
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .orderByDesc(Dish::getUpdatedAt, Dish::getId));
        Map<Long, String> names = categories.findNames(result.getRecords().stream().map(Dish::getCategoryId).distinct().toList());
        return new DishPageResponse(result.getTotal(), result.getRecords().stream()
                .map(dish -> DishRowResponse.from(dish, names.get(dish.getCategoryId())))
                .toList());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DishDetailResponse findDetail(long id) {
        Dish dish = dishes.selectById(id);
        if (dish == null) {
            throw dishNotFound();
        }
        String categoryName = categories.findNames(List.of(dish.getCategoryId())).get(dish.getCategoryId());
        List<DishFlavorGroup> groups = DishFlavorGroup.fromRows(flavors.findByDish(id));
        return new DishDetailResponse(DishRowResponse.from(dish, categoryName), dish.getDescription(), groups);
    }

    @Transactional
    public DishRowResponse create(DishWriteRequest request, byte[] imageBytes, String username) {
        long employeeId = employees.requireActive(username).id();
        String categoryName = categories.requireDishCategory(request.categoryId());
        List<DishFlavor> choices = normalizeFlavors(request.flavors());
        MenuImage image = imageValidator.validate(imageBytes);
        Dish dish = new Dish(request.categoryId(), request.name().strip(), request.price(),
                description(request), employeeId, now());
        try {
            dishes.insert(dish);
        } catch (DataIntegrityViolationException exception) {
            throw dishWriteFailure(exception);
        }
        insertFlavors(dish.getId(), choices);
        images.save(dish.getId(), image);
        return DishRowResponse.from(dish, categoryName);
    }

    @Transactional
    public void update(long id, DishWriteRequest request, byte[] imageBytes, String username) {
        long employeeId = employees.requireActive(username).id();
        categories.requireDishCategory(request.categoryId());
        List<DishFlavor> choices = normalizeFlavors(request.flavors());
        MenuImage replacement = imageBytes == null ? null : imageValidator.validate(imageBytes);
        try {
            int updated = dishes.update(null, Wrappers.<Dish>lambdaUpdate()
                    .eq(Dish::getId, id)
                    .set(Dish::getCategoryId, request.categoryId())
                    .set(Dish::getName, request.name().strip())
                    .set(Dish::getPrice, request.price())
                    .set(Dish::getDescription, description(request))
                    .set(Dish::getUpdatedAt, now())
                    .set(Dish::getUpdatedBy, employeeId));
            if (updated == 0) {
                throw dishNotFound();
            }
        } catch (DataIntegrityViolationException exception) {
            throw dishWriteFailure(exception);
        }
        flavors.deleteByDish(id);
        insertFlavors(id, choices);
        if (replacement != null) {
            images.save(id, replacement);
        }
    }

    @Transactional
    public void setSaleStatus(List<Long> requestedIds, boolean onSale, String username) {
        long employeeId = employees.requireActive(username).id();
        List<Long> ids = requestedIds.stream().distinct().toList();
        if (!onSale) {
            availability.requireCanStopDishes(ids);
        }
        int updated = dishes.update(null, Wrappers.<Dish>lambdaUpdate()
                .in(Dish::getId, ids)
                .set(Dish::getOnSale, onSale)
                .set(Dish::getUpdatedAt, now())
                .set(Dish::getUpdatedBy, employeeId));
        if (updated != ids.size()) {
            throw dishNotFound();
        }
    }

    @Transactional
    public void delete(List<Long> requestedIds, String username) {
        employees.requireActive(username);
        List<Long> ids = requestedIds.stream().distinct().toList();
        try {
            int deleted = dishes.delete(Wrappers.<Dish>lambdaQuery()
                    .in(Dish::getId, ids)
                    .eq(Dish::getOnSale, false));
            if (deleted != ids.size()) {
                throw new BusinessException(BusinessException.Reason.RULE_CONFLICT,
                        "只能删除仍存在且已停售的菜品。请刷新列表并确认所选状态。");
            }
        } catch (DataIntegrityViolationException exception) {
            if (isReferenceConflict(exception)) {
                throw new BusinessException(BusinessException.Reason.RULE_CONFLICT,
                        "该菜品仍被套餐或其他记录引用，无法删除。", exception);
            }
            throw exception;
        }
    }

    private void insertFlavors(long dishId, List<DishFlavor> choices) {
        if (!choices.isEmpty()) {
            flavors.insertAll(dishId, choices);
        }
    }

    private static List<DishFlavor> normalizeFlavors(List<DishFlavorGroup> groups) {
        if (groups == null) {
            return List.of();
        }
        List<DishFlavor> result = new ArrayList<>();
        Set<String> groupNames = new HashSet<>();
        for (int groupPosition = 0; groupPosition < groups.size(); groupPosition++) {
            DishFlavorGroup group = groups.get(groupPosition);
            String groupName = group.name().strip();
            if (!groupNames.add(groupName.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(BusinessException.Reason.INVALID_INPUT, "同一菜品的口味分组不能重名。");
            }
            Set<String> values = new HashSet<>();
            for (int optionPosition = 0; optionPosition < group.options().size(); optionPosition++) {
                String option = group.options().get(optionPosition).strip();
                if (!values.add(option.toLowerCase(Locale.ROOT))) {
                    throw new BusinessException(BusinessException.Reason.INVALID_INPUT, "同一口味分组里的选项不能重名。");
                }
                result.add(new DishFlavor(groupPosition, optionPosition, groupName, option));
            }
        }
        return result;
    }

    private static String description(DishWriteRequest request) {
        return request.description() == null ? "" : request.description().strip();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static BusinessException dishNotFound() {
        return new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "有菜品已不存在，请刷新列表。");
    }

    private static RuntimeException dishWriteFailure(DataIntegrityViolationException exception) {
        if (exception instanceof DuplicateKeyException) {
            return new BusinessException(BusinessException.Reason.RULE_CONFLICT, "已经有同名菜品。", exception);
        }
        if (isReferenceConflict(exception)) {
            return new BusinessException(BusinessException.Reason.RULE_CONFLICT,
                    "所选菜品分类已变动，请刷新后重试。", exception);
        }
        return exception;
    }

    private static boolean isReferenceConflict(DataIntegrityViolationException exception) {
        return exception.getMostSpecificCause() instanceof SQLException sql && sql.getErrorCode() == 547;
    }
}
