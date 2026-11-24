package com.neuedu.reggie.controller.admin;

import com.neuedu.reggie.dto.category.CategoryCreateRequest;
import com.neuedu.reggie.dto.category.CategoryOptionResponse;
import com.neuedu.reggie.dto.category.CategoryPageResponse;
import com.neuedu.reggie.dto.category.CategoryResponse;
import com.neuedu.reggie.dto.category.CategoryUpdateRequest;
import com.neuedu.reggie.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryController {

    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    public CategoryPageResponse list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Min(1) @Max(2) Integer type) {
        return categories.findPage(page, size, type);
    }

    @GetMapping("/options")
    public List<CategoryOptionResponse> options(@RequestParam @Min(1) @Max(2) int type) {
        return categories.findOptions(type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryCreateRequest form, Principal employee) {
        return categories.create(form.type(), form.name(), form.sort(), employee.getName());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable @Min(1) long id, @Valid @RequestBody CategoryUpdateRequest form,
                       Principal employee) {
        categories.update(id, form.name(), form.sort(), employee.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Min(1) long id, Principal employee) {
        categories.delete(id, employee.getName());
    }
}
