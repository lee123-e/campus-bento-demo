package com.neuedu.reggie.controller.admin;

import com.neuedu.reggie.dto.menu.MenuDeleteRequest;
import com.neuedu.reggie.dto.dish.DishDetailResponse;
import com.neuedu.reggie.dto.dish.DishPageResponse;
import com.neuedu.reggie.dto.dish.DishRowResponse;
import com.neuedu.reggie.dto.menu.MenuSaleStatusRequest;
import com.neuedu.reggie.dto.dish.DishWriteRequest;
import com.neuedu.reggie.entity.MenuImage;
import com.neuedu.reggie.service.DishImageService;
import com.neuedu.reggie.service.DishService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/dishes")
public class DishController {

    private final DishService dishes;
    private final DishImageService images;

    public DishController(DishService dishes, DishImageService images) {
        this.dishes = dishes;
        this.images = images;
    }

    @GetMapping
    public DishPageResponse list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 60) String name,
            @RequestParam(required = false) @Min(1) Long categoryId) {
        return dishes.findPage(page, size, name, categoryId);
    }

    @GetMapping("/{id}")
    public DishDetailResponse detail(@PathVariable @Min(1) long id) {
        return dishes.findDetail(id);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable @Min(1) long id) {
        MenuImage image = images.find(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .cacheControl(CacheControl.noStore())
                .body(image.contents());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DishRowResponse create(@Valid @RequestPart("dish") DishWriteRequest form,
                                  @RequestPart("image") MultipartFile image, Principal employee) throws IOException {
        return dishes.create(form, image.getBytes(), employee.getName());
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable @Min(1) long id, @Valid @RequestPart("dish") DishWriteRequest form,
                       @RequestPart(value = "image", required = false) MultipartFile image,
                       Principal employee) throws IOException {
        dishes.update(id, form, image == null ? null : image.getBytes(), employee.getName());
    }

    @PutMapping("/sale-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setSaleStatus(@Valid @RequestBody MenuSaleStatusRequest form, Principal employee) {
        dishes.setSaleStatus(form.ids(), form.onSale(), employee.getName());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Valid @RequestBody MenuDeleteRequest form, Principal employee) {
        dishes.delete(form.ids(), employee.getName());
    }
}
