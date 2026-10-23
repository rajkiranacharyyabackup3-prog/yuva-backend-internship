package com.yuvaintern.blog.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class BlogUserDetailsService implements UserDetailsService {
    private final BlogUserRepository users;

    public BlogUserDetailsService(BlogUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        BlogUser account = users.findByUsername(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Account was not found."));
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles("USER")
                .build();
    }
}
