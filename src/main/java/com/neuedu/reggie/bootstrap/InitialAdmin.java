package com.neuedu.reggie.bootstrap;

import com.neuedu.reggie.mapper.EmployeeMapper;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialAdmin implements ApplicationRunner {

    private final EmployeeMapper employees;
    private final PasswordEncoder passwords;
    private final String initialPassword;

    public InitialAdmin(EmployeeMapper employees, PasswordEncoder passwords,
                        @Value("${reggie.admin.password:}") String initialPassword) {
        this.employees = employees;
        this.passwords = passwords;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (initialPassword.isBlank() || employees.findByUsername("admin").isPresent()) {
            return;
        }
        if (initialPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("REGGIE_ADMIN_PASSWORD must fit within bcrypt's 72-byte limit");
        }
        employees.insertIfAbsent("admin", "店铺管理员", passwords.encode(initialPassword));
    }
}
