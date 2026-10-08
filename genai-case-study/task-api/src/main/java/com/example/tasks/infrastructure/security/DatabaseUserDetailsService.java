package com.example.tasks.infrastructure.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

// Signs in by username; the principal it returns is named by the user's id, which is what the API needs.
@Component
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var users = jdbcTemplate.query("select id, password_hash from users where username = ?",
            (row, rowNumber) -> User.withUsername(row.getString("id"))
                .password(row.getString("password_hash"))
                .authorities(List.of())
                .build(),
            username);
        if (users.isEmpty()) {
            throw new UsernameNotFoundException("Unknown user");
        }
        return users.getFirst();
    }
}
