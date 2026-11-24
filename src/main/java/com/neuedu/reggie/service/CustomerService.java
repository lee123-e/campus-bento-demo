package com.neuedu.reggie.service;

import com.neuedu.reggie.entity.Customer;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.CustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerMapper customers;

    public CustomerService(CustomerMapper customers) {
        this.customers = customers;
    }

    public Customer findOrRegister(String phone) {
        return customers.findByPhone(phone).orElseGet(() -> {
            customers.insertIfAbsent(phone);
            return customers.findByPhone(phone).orElseThrow();
        });
    }

    public Customer getProfile(long id) {
        return customers.findById(id).orElseThrow(() -> new BusinessException(
                BusinessException.Reason.SESSION_EXPIRED, "登录已失效，请重新登录。"));
    }

    public Customer updateNickname(long id, String nickname) {
        customers.updateNickname(id, nickname.strip());
        return getProfile(id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockForWrite(long id) {
        customers.lockById(id).orElseThrow(() -> new BusinessException(
                BusinessException.Reason.SESSION_EXPIRED, "登录已失效，请重新登录。"));
    }
}
