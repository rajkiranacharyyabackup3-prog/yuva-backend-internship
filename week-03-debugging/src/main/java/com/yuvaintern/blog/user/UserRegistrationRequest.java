package com.yuvaintern.blog.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Use letters, numbers, and underscores only.")
        String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 10, max = 72,
                message = "Password must contain between 10 and 72 characters.")
        String password
) {
}
