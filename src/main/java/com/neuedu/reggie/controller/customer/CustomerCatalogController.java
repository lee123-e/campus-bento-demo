package com.neuedu.reggie.controller.customer;

import com.neuedu.reggie.dto.customer.CatalogCategoryResponse;
import com.neuedu.reggie.dto.customer.CatalogDetailResponse;
import com.neuedu.reggie.entity.MenuImage;
import com.neuedu.reggie.service.CustomerCatalogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/catalog")
public class CustomerCatalogController {

    private final CustomerCatalogService catalog;

    public CustomerCatalogController(CustomerCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<CatalogCategoryResponse> list() {
        return catalog.list();
    }

    @GetMapping("/{type}/{id}")
    public CatalogDetailResponse detail(@PathVariable @Min(1) @Max(2) int type, @PathVariable @Min(1) long id) {
        return catalog.detail(type, id);
    }

    @GetMapping("/{type}/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable @Min(1) @Max(2) int type, @PathVariable @Min(1) long id) {
        MenuImage image = catalog.image(type, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mediaType()))
                .cacheControl(CacheControl.noStore()).body(image.contents());
    }
}
