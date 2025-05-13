package com.dilip.audit_service.config;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Require authentication for specific GET endpoints
                        .requestMatchers(HttpMethod.GET, "/api/audit/logs").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/audit/logs/event/*").authenticated() // for /{eventId}
                        .requestMatchers(HttpMethod.GET, "/api/audit/logs/user/*").authenticated() // for /{eventId}
                        .requestMatchers(HttpMethod.GET, "/api/audit/query").authenticated()

                        // Allow POST to /api/audit/logs without authentication
                        .requestMatchers(HttpMethod.POST, "/api/audit/logs").permitAll()

                        // Allow all other requests
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .csrf().disable();

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                if (username.equals("admin")) {
                    return new CustomUserDetails(
                            "admin123",
                            "admin",
                            "{noop}adminpass",
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );
                } else if (username.equals("user-001")) {
                    return new CustomUserDetails(
                            "user-001",
                            "user-001",
                            "{noop}userpass",
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                } else {
                    throw new UsernameNotFoundException("User not found: " + username);
                }
            }
        };
    }


//    @Bean
//    public InMemoryUserDetailsManager userDetailsService() {
//        UserDetails admin = new CustomUserDetails(
//                "admin123",
//                "admin",
//                "{noop}adminpass",
//                List.of(new SimpleGrantedAuthority("ADMIN"))
//        );
//
//        UserDetails user = new CustomUserDetails(
//                "user-001",
//                "user-001",
//                "{noop}userpass",
//                List.of(new SimpleGrantedAuthority("USER"))
//        );
//
//        return new InMemoryUserDetailsManager(admin, user);
//    }
}
