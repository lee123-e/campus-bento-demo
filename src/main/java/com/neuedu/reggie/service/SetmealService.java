package com.neuedu.reggie.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.reggie.dto.setmeal.SetmealDetailResponse;
import com.neuedu.reggie.dto.setmeal.SetmealDishRequest;
import com.neuedu.reggie.dto.setmeal.SetmealPageResponse;
import com.neuedu.reggie.dto.setmeal.SetmealRowResponse;
import com.neuedu.reggie.dto.setmeal.SetmealWriteRequest;
import com.neuedu.reggie.entity.MenuImage;
import com.neuedu.reggie.entity.Setmeal;
import com.neuedu.reggie.entity.SetmealDish;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.SetmealDishMapper;
import com.neuedu.reggie.mapper.SetmealMapper;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetmealService {

    private final SetmealMapper setmeals;
    private final SetmealDishMapper items;
    private final CategoryService categories;
    private final EmployeeService employees;
    private final MenuAvailabilityService availability;
    private final MenuImageValidator images;

    public SetmealService(SetmealMapper setmeals, SetmealDishMapper items, CategoryService categories,
                          EmployeeService employees, MenuAvailabilityService availability, MenuImageValidator images) {
        this.setmeals = setmeals;
        this.items = items;
        this.categories = categories;
        this.employees = employees;
        this.availability = availability;
        this.images = images;
    }

    public SetmealPageResponse findPage(int page, int size, String name, Long categoryId) {
        String query = name == null ? "" : name.strip();
        Page<Setmeal> result = setmeals.selectPage(new Page<>(page, size), Wrappers.<Setmeal>lambdaQuery()
                .like(!query.isEmpty(), Setmeal::getName, query)
                .eq(categoryId != null, Setmeal::getCategoryId, categoryId)
                .orderByDesc(Setmeal::getUpdatedAt, Setmeal::getId));
        Map<Long, String> names = categories.findNames(result.getRecords().stream().map(Setmeal::getCategoryId).distinct().toList());
        return new SetmealPageResponse(result.getTotal(), result.getRecords().stream()
                .map(setmeal -> SetmealRowResponse.from(setmeal, names.get(setmeal.getCategoryId())))
                .toList());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SetmealDetailResponse findDetail(long id) {
        Setmeal setmeal = setmeals.selectById(id);
        if (setmeal == null) {
            throw notFound();
        }
        String name = categories.findNames(List.of(setmeal.getCategoryId())).get(setmeal.getCategoryId());
        return new SetmealDetailResponse(SetmealRowResponse.from(setmeal, name), setmeal.getDescription(), items.findDetails(id));
    }

    public MenuImage findImage(long id) {
        return setmeals.findImage(id).orElseThrow(SetmealService::notFound);
    }

    @Transactional
    public SetmealRowResponse create(SetmealWriteRequest form, byte[] imageBytes, String username) {
        long employeeId = employees.requireActive(username).id();
        MenuImage image = images.validate(imageBytes);
        String categoryName = categories.requireSetmealCategory(form.categoryId());
        List<SetmealDish> contents = normalizeItems(form.items());
        availability.requireDishes(dishIds(contents), false);
        Setmeal setmeal = new Setmeal(form.categoryId(), form.name().strip(), form.price(),
                description(form), image, employeeId, now());
        try {
            setmeals.insert(setmeal);
            items.insertAll(setmeal.getId(), contents);
        } catch (DataIntegrityViolationException exception) {
            throw writeFailure(exception);
        }
        return SetmealRowResponse.from(setmeal, categoryName);
    }

    @Transactional
    public void update(long id, SetmealWriteRequest form, byte[] imageBytes, String username) {
        long employeeId = employees.requireActive(username).id();
        MenuImage replacement = imageBytes == null ? null : images.validate(imageBytes);
        categories.requireSetmealCategory(form.categoryId());
        List<SetmealDish> contents = normalizeItems(form.items());
        List<Setmeal> locked = setmeals.lockRows(List.of(id));
        if (locked.isEmpty()) {
            throw notFound();
        }
        availability.requireDishes(dishIds(contents), locked.get(0).getOnSale());

        var change = Wrappers.<Setmeal>lambdaUpdate()
                .eq(Setmeal::getId, id)
                .set(Setmeal::getCategoryId, form.categoryId())
                .set(Setmeal::getName, form.name().strip())
                .set(Setmeal::getPrice, form.price())
                .set(Setmeal::getDescription, description(form))
                .set(Setmeal::getUpdatedAt, now())
                .set(Setmeal::getUpdatedBy, employeeId);
        if (replacement != null) {
            change.set(Setmeal::getImageMediaType, replacement.mediaType())
                    .set(Setmeal::getImageContents, replacement.contents());
        }

        try {
            setmeals.update(null, change);
            items.deleteBySetmeal(id);
            items.insertAll(id, contents);
        } catch (DataIntegrityViolationException exception) {
            throw writeFailure(exception);
        }
    }

    @Transactional
    public void setSaleStatus(List<Long> requestedIds, boolean onSale, String username) {
        long employeeId = employees.requireActive(username).id();
        List<Long> ids = requestedIds.stream().distinct().toList();
        if (onSale) {
            if (setmeals.lockRows(ids).size() != ids.size()) {
                throw notFound();
            }
            availability.requireDishes(items.findDishIds(ids), true);
        }
        int updated = setmeals.update(null, Wrappers.<Setmeal>lambdaUpdate()
                .in(Setmeal::getId, ids)
                .set(Setmeal::getOnSale, onSale)
                .set(Setmeal::getUpdatedAt, now())
                .set(Setmeal::getUpdatedBy, employeeId));
        if (updated != ids.size()) {
            throw notFound();
        }
    }

    @Transactional
    public void delete(List<Long> requestedIds, String username) {
        employees.requireActive(username);
        List<Long> ids = requestedIds.stream().distinct().toList();
        int removed = setmeals.delete(Wrappers.<Setmeal>lambdaQuery()
                .in(Setmeal::getId, ids)
                .eq(Setmeal::getOnSale, false));
        if (removed != ids.size()) {
            throw new BusinessException(BusinessException.Reason.RULE_CONFLICT,
                    "只能删除仍存在且已停售的套餐。请刷新列表并确认所选状态。");
        }
    }

    private static List<SetmealDish> normalizeItems(List<SetmealDishRequest> request) {
        List<SetmealDish> result = new ArrayList<>(request.size());
        Set<Long> used = new HashSet<>();
        for (int index = 0; index < request.size(); index++) {
            SetmealDishRequest item = request.get(index);
            if (!used.add(item.dishId())) {
                throw new BusinessException(BusinessException.Reason.INVALID_INPUT, "同一道菜只需选择一次，请直接调整份数。");
            }
            result.add(new SetmealDish(item.dishId(), item.quantity(), index));
        }
        return result;
    }

    private static List<Long> dishIds(List<SetmealDish> contents) {
        return contents.stream().map(SetmealDish::dishId).toList();
    }

    private static String description(SetmealWriteRequest request) {
        return request.description() == null ? "" : request.description().strip();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static BusinessException notFound() {
        return new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "套餐已不存在，请刷新列表。");
    }

    private static RuntimeException writeFailure(DataIntegrityViolationException exception) {
        if (exception instanceof DuplicateKeyException) {
            return new BusinessException(BusinessException.Reason.RULE_CONFLICT, "已经有同名套餐。", exception);
        }
        if (exception.getMostSpecificCause() instanceof SQLException sql && sql.getErrorCode() == 547) {
            return new BusinessException(BusinessException.Reason.RULE_CONFLICT, "所选分类或菜品已变动，请刷新后重试。", exception);
        }
        return exception;
    }
}
