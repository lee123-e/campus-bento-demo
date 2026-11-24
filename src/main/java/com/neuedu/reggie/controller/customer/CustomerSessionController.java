package com.neuedu.reggie.controller.customer;

import com.neuedu.reggie.dto.customer.CustomerCodeRequest;
import com.neuedu.reggie.dto.customer.CustomerCodeResponse;
import com.neuedu.reggie.dto.customer.CustomerLoginRequest;
import com.neuedu.reggie.dto.customer.CustomerSessionResponse;
import com.neuedu.reggie.entity.Customer;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.security.CustomerPrincipal;
import com.neuedu.reggie.service.CustomerService;
import com.neuedu.reggie.service.CustomerVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/customer")
public class CustomerSessionController {

    private static final String CHALLENGE = CustomerSessionController.class.getName() + ".CHALLENGE";

    private final CustomerService customers;
    private final CustomerVerificationService verification;
    private final HttpSessionSecurityContextRepository contexts;
    private final HttpSessionCsrfTokenRepository csrfTokens;
    private final boolean localVerification;

    public CustomerSessionController(CustomerService customers, CustomerVerificationService verification,
                                     HttpSessionSecurityContextRepository contexts, HttpSessionCsrfTokenRepository csrfTokens,
                                     @Value("${reggie.customer.local-verification}") boolean localVerification) {
        this.customers = customers;
        this.verification = verification;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
        this.localVerification = localVerification;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/auth/code")
    public CustomerCodeResponse issueCode(@Valid @RequestBody CustomerCodeRequest input, HttpSession session) {
        if (!localVerification) {
            throw new BusinessException(BusinessException.Reason.SERVICE_UNAVAILABLE, "验证码服务暂未配置。");
        }
        synchronized (WebUtils.getSessionMutex(session)) {
            var previous = (CustomerVerificationService.Challenge) session.getAttribute(CHALLENGE);
            var challenge = verification.issue(previous, input.phone());
            session.setAttribute(CHALLENGE, challenge);
            return new CustomerCodeResponse(challenge.code(), verification.resendDelaySeconds());
        }
    }

    @PostMapping("/auth/login")
    public CustomerSessionResponse login(@Valid @RequestBody CustomerLoginRequest input,
                                         HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        synchronized (WebUtils.getSessionMutex(session)) {
            var challenge = (CustomerVerificationService.Challenge) session.getAttribute(CHALLENGE);
            verification.verify(challenge, input.phone(), input.code());
            session.removeAttribute(CHALLENGE);
            Customer customer = customers.findOrRegister(input.phone());

            request.changeSessionId();
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    new CustomerPrincipal(customer.id(), customer.phone()), null,
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            csrfTokens.saveToken(null, request, response);
            return CustomerSessionResponse.from(customer);
        }
    }

    @GetMapping("/session")
    public CustomerSessionResponse session(@AuthenticationPrincipal CustomerPrincipal customer) {
        return CustomerSessionResponse.from(customers.getProfile(customer.id()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (WebUtils.getSessionMutex(session)) {
                session.removeAttribute(CHALLENGE);
                contexts.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
                csrfTokens.saveToken(null, request, response);
            }
        }
        SecurityContextHolder.clearContext();
    }
}
