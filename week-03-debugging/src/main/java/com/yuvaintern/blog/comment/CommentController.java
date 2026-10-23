package com.yuvaintern.blog.comment;

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
@RequestMapping("/api/v1")
public class CommentController {
    private final CommentService comments;

    public CommentController(CommentService comments) { this.comments = comments; }

    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponse> listForPost(@PathVariable long postId) { return comments.listForPost(postId); }

    @GetMapping("/comments/{id}")
    public CommentResponse get(@PathVariable long id) { return comments.get(id); }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable long postId, @Valid @RequestBody CommentRequest request,
                                  Authentication authentication) {
        return comments.create(postId, request, authentication.getName());
    }

    @PutMapping("/comments/{id}")
    public CommentResponse update(@PathVariable long id, @Valid @RequestBody CommentRequest request,
                                  Authentication authentication) {
        return comments.update(id, request, authentication.getName());
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication authentication) { comments.delete(id, authentication.getName()); }
}
