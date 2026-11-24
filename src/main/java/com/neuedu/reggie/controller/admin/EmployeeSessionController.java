package com.neuedu.reggie.controller.admin;

import com.neuedu.reggie.dto.employee.EmployeeSessionResponse;
import com.neuedu.reggie.entity.EmployeeAccount;
import com.neuedu.reggie.service.EmployeeService;
import java.security.Principal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class EmployeeSessionController {

    private final EmployeeService employees;

    public EmployeeSessionController(EmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @GetMapping("/session")
    public EmployeeSessionResponse session(Principal principal) {
        EmployeeAccount employee = employees.requireActive(principal.getName());
        return new EmployeeSessionResponse(employee.id(), employee.username(), employee.displayName());
    }
}
