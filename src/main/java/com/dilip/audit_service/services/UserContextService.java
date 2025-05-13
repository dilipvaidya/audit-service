package com.dilip.audit_service.services;

import com.dilip.audit_service.config.CustomUserDetails;
import com.dilip.audit_service.data.entity.UserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserContextService {

    public Optional<UserContext> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String loggedInUserId = userDetails.getUserId();

        String username = authentication.getName(); // from basic auth or JWT "sub"
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Optional.of(new UserContext(loggedInUserId, username, roles));
    }
}
