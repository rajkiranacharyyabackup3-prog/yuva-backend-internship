package com.yuvaintern.blog.user;

import com.yuvaintern.blog.common.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class UserService {
    private final BlogUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(BlogUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) {
            throw new ConflictException("That username is already registered.");
        }
        if (users.existsByEmail(email)) {
            throw new ConflictException("That email address is already registered.");
        }
        BlogUser user = users.save(new BlogUser(username, email,
                passwordEncoder.encode(request.password()), OffsetDateTime.now(ZoneOffset.UTC)));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public BlogUser requireByUsername(String username) {
        return users.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new com.yuvaintern.blog.common.ResourceNotFoundException("User was not found."));
    }
}
