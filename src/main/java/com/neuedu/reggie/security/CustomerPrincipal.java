package com.neuedu.reggie.security;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

public record CustomerPrincipal(long id, String phone) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return phone;
    }
}
