package com.neuedu.reggie.dto.customer;

import com.neuedu.reggie.entity.CustomerAddress;

public record AddressResponse(long id, String contactName, String phone, String deliveryAddress,
                              String label, boolean defaultAddress) {

    public static AddressResponse from(CustomerAddress address) {
        return new AddressResponse(address.getId(), address.getContactName(), address.getPhone(),
                address.getDeliveryAddress(), address.getLabel(), address.getDefaultAddress());
    }
}
