package com.neuedu.reggie.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.reggie.dto.category.CategoryOptionResponse;
import com.neuedu.reggie.dto.category.CategoryPageResponse;
import com.neuedu.reggie.dto.category.CategoryResponse;
import com.neuedu.reggie.entity.Category;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.CategoryMapper;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private static final int DISH_CATEGORY = 1;
    private static final int SETMEAL_CATEGORY = 2;

    private final CategoryMapper categories;
    private final EmployeeService employees;

    public CategoryService(CategoryMapper categories, EmployeeService employees) {
        this.categories = categories;
        this.employees = employees;
    }

    public CategoryPageResponse findPage(int page, int size, Integer type) {
        Page<Category> result = categories.selectPage(new Page<>(page, size), Wrappers.<Category>lambdaQuery()
                .eq(type != null, Category::getCategoryType, type)
                .orderByAsc(Category::getSortOrder, Category::getId));
        return new CategoryPageResponse(result.getTotal(), result.getRecords().stream().map(CategoryResponse::from).toList());
    }

    public List<CategoryOptionResponse> findOptions(int type) {
        return categories.selectList(Wrappers.<Category>lambdaQuery()
                        .select(Category::getId, Category::getName)
                        .eq(Category::getCategoryType, type)
                        .orderByAsc(Category::getSortOrder)
                        .orderByDesc(Category::getUpdatedAt)
                        .orderByAsc(Category::getId))
                .stream()
                .map(category -> new CategoryOptionResponse(category.getId(), category.getName()))
                .toList();
    }

    public Map<Long, String> findNames(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categories.selectList(Wrappers.<Category>lambdaQuery()
                        .select(Category::getId, Category::getName)
                        .in(Category::getId, ids))
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    public String requireDishCategory(long id) {
        return requireCategoryType(id, DISH_CATEGORY, "请选择有效的菜品分类。");
    }

    public String requireSetmealCategory(long id) {
        return requireCategoryType(id, SETMEAL_CATEGORY, "请选择有效的套餐分类。");
    }

    private String requireCategoryType(long id, int type, String message) {
        Category category = categories.selectById(id);
        if (category == null || category.getCategoryType() != type) {
            throw new BusinessException(BusinessException.Reason.INVALID_INPUT, message);
        }
        return category.getName();
    }

    public CategoryResponse create(int type, String name, int sort, String username) {
        Category category = new Category(type, name.strip(), sort, employees.requireActive(username).id(), currentTime());
        try {
            categories.insert(category);
            return CategoryResponse.from(category);
        } catch (DuplicateKeyException exception) {
            throw duplicateName(exception);
        }
    }

    public void update(long id, String name, int sort, String username) {
        long employeeId = employees.requireActive(username).id();
        try {
            int updated = categories.update(null, Wrappers.<Category>lambdaUpdate()
                    .eq(Category::getId, id)
                    .set(Category::getName, name.strip())
                    .set(Category::getSortOrder, sort)
                    .set(Category::getUpdatedAt, currentTime())
                    .set(Category::getUpdatedBy, employeeId));
            requireCategory(updated);
        } catch (DuplicateKeyException exception) {
            throw duplicateName(exception);
        }
    }

    public void delete(long id, String username) {
        employees.requireActive(username);
        try {
            requireCategory(categories.deleteById(id));
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause() instanceof SQLException sql && sql.getErrorCode() == 547) {
                throw new BusinessException(BusinessException.Reason.RULE_CONFLICT, "该分类下仍有菜品或套餐，无法删除。", exception);
            }
            throw exception;
        }
    }

    private static LocalDateTime currentTime() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static void requireCategory(int affectedRows) {
        if (affectedRows == 0) {
            throw new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "该分类已不存在。");
        }
    }

    private static BusinessException duplicateName(DuplicateKeyException exception) {
        return new BusinessException(BusinessException.Reason.RULE_CONFLICT, "同类型下已存在这个分类名称。", exception);
    }
}
