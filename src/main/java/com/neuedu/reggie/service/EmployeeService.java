package com.neuedu.reggie.service;

import com.neuedu.reggie.entity.EmployeeAccount;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.EmployeeMapper;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private final EmployeeMapper employees;

    public EmployeeService(EmployeeMapper employees) {
        this.employees = employees;
    }

    public Optional<EmployeeAccount> findByUsername(String username) {
        return employees.findByUsername(username);
    }

    public EmployeeAccount requireActive(String username) {
        return findByUsername(username)
                .filter(EmployeeAccount::enabled)
                .orElseThrow(() -> new BusinessException(BusinessException.Reason.SESSION_EXPIRED, "登录已失效，请重新登录。"));
    }
}
