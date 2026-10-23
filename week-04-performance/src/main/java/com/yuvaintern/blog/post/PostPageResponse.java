package com.yuvaintern.blog.post;

import java.util.List;

public record PostPageResponse(List<PostResponse> content, int page, int size, boolean hasNext) { }
