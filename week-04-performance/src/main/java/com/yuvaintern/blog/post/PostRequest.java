package com.yuvaintern.blog.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 10000) String content
) {
}
