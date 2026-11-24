package com.neuedu.reggie.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.util.HttpSessionMutexListener;

@Configuration(proxyBeanMethods = false)
public class CustomerSecurity {

    @Bean
    HttpSessionSecurityContextRepository customerContexts() {
        var repository = new HttpSessionSecurityContextRepository();
        repository.setSpringSecurityContextKey("REGGIE_CUSTOMER_SECURITY_CONTEXT");
        return repository;
    }

    @Bean
    HttpSessionCsrfTokenRepository customerCsrfTokens() {
        var repository = new HttpSessionCsrfTokenRepository();
        repository.setSessionAttributeName("REGGIE_CUSTOMER_CSRF_TOKEN");
        return repository;
    }

    @Bean
    ServletListenerRegistrationBean<HttpSessionMutexListener> customerSessionMutex() {
        return new ServletListenerRegistrationBean<>(new HttpSessionMutexListener());
    }

    @Bean
    @Order(1)
    SecurityFilterChain customerSecurityFilterChain(HttpSecurity http, HttpSessionSecurityContextRepository customerContexts,
                                                    HttpSessionCsrfTokenRepository customerCsrfTokens) {
        return http.securityMatcher("/api/customer/**")
                .securityContext(context -> context.securityContextRepository(customerContexts))
                .csrf(csrf -> csrf.csrfTokenRepository(customerCsrfTokens))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customer/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customer/auth/code", "/api/customer/auth/login").permitAll()
                        .anyRequest().hasRole("CUSTOMER"))
                .requestCache(cache -> cache.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}
