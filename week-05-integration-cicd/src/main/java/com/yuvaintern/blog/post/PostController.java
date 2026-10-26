package com.yuvaintern.blog.post;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {
    private final PostService posts;

    public PostController(PostService posts) { this.posts = posts; }

    @GetMapping
    public List<PostResponse> list() { return posts.list(); }

    @GetMapping("/{id}")
    public PostResponse get(@PathVariable long id) { return posts.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest request, Authentication authentication) {
        return posts.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public PostResponse update(@PathVariable long id, @Valid @RequestBody PostRequest request,
                               Authentication authentication) {
        return posts.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication authentication) {
        posts.delete(id, authentication.getName());
    }
}
