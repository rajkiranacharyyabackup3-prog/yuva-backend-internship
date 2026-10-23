package com.yuvaintern.blog.user;

import java.time.OffsetDateTime;

public record UserResponse(Long id, String username, String email, OffsetDateTime createdAt) {
    public static UserResponse from(BlogUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
}
