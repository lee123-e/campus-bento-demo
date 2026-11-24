package com.neuedu.reggie.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neuedu.reggie.dto.customer.AddressResponse;
import com.neuedu.reggie.dto.customer.AddressWriteRequest;
import com.neuedu.reggie.entity.CustomerAddress;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.CustomerAddressMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAddressService {

    private final CustomerAddressMapper addresses;
    private final CustomerService customers;

    public CustomerAddressService(CustomerAddressMapper addresses, CustomerService customers) {
        this.addresses = addresses;
        this.customers = customers;
    }

    public List<AddressResponse> list(long customerId) {
        return addresses.selectList(owned(customerId)
                        .orderByDesc(CustomerAddress::getDefaultAddress)
                        .orderByDesc(CustomerAddress::getId))
                .stream().map(AddressResponse::from).toList();
    }

    @Transactional
    public AddressResponse create(long customerId, AddressWriteRequest request) {
        customers.lockForWrite(customerId);
        boolean first = addresses.selectCount(owned(customerId)) == 0;
        var address = new CustomerAddress(customerId, request.contactName().strip(), request.phone(),
                request.deliveryAddress().strip(), label(request), first);
        addresses.insert(address);
        return AddressResponse.from(address);
    }

    @Transactional
    public void update(long customerId, long id, AddressWriteRequest request) {
        customers.lockForWrite(customerId);
        int updated = addresses.update(null, Wrappers.<CustomerAddress>lambdaUpdate()
                .eq(CustomerAddress::getId, id)
                .eq(CustomerAddress::getCustomerId, customerId)
                .set(CustomerAddress::getContactName, request.contactName().strip())
                .set(CustomerAddress::getPhone, request.phone())
                .set(CustomerAddress::getDeliveryAddress, request.deliveryAddress().strip())
                .set(CustomerAddress::getLabel, label(request)));
        if (updated == 0) {
            throw notFound();
        }
    }

    @Transactional
    public void setDefault(long customerId, long id) {
        customers.lockForWrite(customerId);
        CustomerAddress address = requireOwned(customerId, id);
        if (!address.getDefaultAddress()) {
            addresses.update(null, Wrappers.<CustomerAddress>lambdaUpdate()
                    .eq(CustomerAddress::getCustomerId, customerId)
                    .eq(CustomerAddress::getDefaultAddress, true)
                    .set(CustomerAddress::getDefaultAddress, false));
            markDefault(customerId, id);
        }
    }

    @Transactional
    public void delete(long customerId, long id) {
        customers.lockForWrite(customerId);
        CustomerAddress address = requireOwned(customerId, id);
        addresses.delete(owned(customerId).eq(CustomerAddress::getId, id));
        if (address.getDefaultAddress()) {
            addresses.findFirstId(customerId).ifPresent(next -> markDefault(customerId, next));
        }
    }

    public AddressResponse getOwned(long customerId, long id) {
        return AddressResponse.from(requireOwned(customerId, id));
    }

    private CustomerAddress requireOwned(long customerId, long id) {
        CustomerAddress address = addresses.selectOne(owned(customerId).eq(CustomerAddress::getId, id));
        if (address == null) {
            throw notFound();
        }
        return address;
    }

    private void markDefault(long customerId, long id) {
        addresses.update(null, Wrappers.<CustomerAddress>lambdaUpdate()
                .eq(CustomerAddress::getCustomerId, customerId)
                .eq(CustomerAddress::getId, id)
                .set(CustomerAddress::getDefaultAddress, true));
    }

    private static LambdaQueryWrapper<CustomerAddress> owned(long customerId) {
        return Wrappers.<CustomerAddress>lambdaQuery().eq(CustomerAddress::getCustomerId, customerId);
    }

    private static String label(AddressWriteRequest input) {
        return input.label() == null ? "" : input.label().strip();
    }

    private static BusinessException notFound() {
        return new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "这条地址已不存在。");
    }
}
