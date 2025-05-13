package com.dilip.audit_service.data.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserContext {
    private final String userId;
    private final String userName;
    private final List<String> roles;

    public UserContext(String userId, String userName, List<String> roles) {
        this.userId = userId;
        this.userName = userName;
        this.roles = roles;
    }

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN"); // or whatever role your system uses
    }
}
