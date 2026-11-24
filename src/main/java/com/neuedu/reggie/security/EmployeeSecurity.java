package com.neuedu.reggie.security;

import com.neuedu.reggie.service.EmployeeService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration(proxyBeanMethods = false)
public class EmployeeSecurity {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/csrf", "/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("EMPLOYEE")
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/api/admin/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                        .failureHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/admin/logout")
                        .invalidateHttpSession(false)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    @Bean
    UserDetailsService employeeDetails(EmployeeService employees) {
        return username -> employees.findByUsername(username)
                .map(employee -> User.withUsername(employee.username())
                        .password(employee.passwordHash())
                        .disabled(!employee.enabled())
                        .roles("EMPLOYEE")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Employee account not found"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
