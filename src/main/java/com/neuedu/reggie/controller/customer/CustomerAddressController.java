package com.neuedu.reggie.controller.customer;

import com.neuedu.reggie.dto.customer.AddressResponse;
import com.neuedu.reggie.dto.customer.AddressWriteRequest;
import com.neuedu.reggie.security.CustomerPrincipal;
import com.neuedu.reggie.service.CustomerAddressService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/addresses")
public class CustomerAddressController {

    private final CustomerAddressService addresses;

    public CustomerAddressController(CustomerAddressService addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal CustomerPrincipal customer) {
        return addresses.list(customer.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse add(@Valid @RequestBody AddressWriteRequest request,
                               @AuthenticationPrincipal CustomerPrincipal customer) {
        return addresses.create(customer.id(), request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void edit(@PathVariable @Min(1) long id, @Valid @RequestBody AddressWriteRequest request,
                     @AuthenticationPrincipal CustomerPrincipal customer) {
        addresses.update(customer.id(), id, request);
    }

    @PutMapping("/{id}/default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setDefault(@PathVariable @Min(1) long id, @AuthenticationPrincipal CustomerPrincipal customer) {
        addresses.setDefault(customer.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Min(1) long id, @AuthenticationPrincipal CustomerPrincipal customer) {
        addresses.delete(customer.id(), id);
    }
}
