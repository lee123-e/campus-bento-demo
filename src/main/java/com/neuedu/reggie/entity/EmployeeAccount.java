package com.neuedu.reggie.entity;

public record EmployeeAccount(long id, String username, String displayName, String passwordHash, boolean enabled) {
}
