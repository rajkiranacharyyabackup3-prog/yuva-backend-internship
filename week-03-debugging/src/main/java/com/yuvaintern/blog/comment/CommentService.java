package com.yuvaintern.blog.comment;

import com.yuvaintern.blog.common.ResourceNotFoundException;
import com.yuvaintern.blog.post.Post;
import com.yuvaintern.blog.post.PostRepository;
import com.yuvaintern.blog.user.BlogUser;
import com.yuvaintern.blog.user.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class CommentService {
    private final CommentRepository comments;
    private final PostRepository posts;
    private final UserService users;

    public CommentService(CommentRepository comments, PostRepository posts, UserService users) {
        this.comments = comments;
        this.posts = posts;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listForPost(long postId) {
        if (!posts.existsById(postId)) {
            throw new ResourceNotFoundException("Post was not found.");
        }
        return comments.findAllByPostIdOrderByCreatedAtAsc(postId).stream().map(CommentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CommentResponse get(long id) { return CommentResponse.from(requireComment(id)); }

    @Transactional
    public CommentResponse create(long postId, CommentRequest request, String username) {
        Post post = posts.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post was not found."));
        BlogUser author = users.requireByUsername(username);
        return CommentResponse.from(comments.save(new Comment(post, author, request.content().trim(),
                OffsetDateTime.now(ZoneOffset.UTC))));
    }

    @Transactional
    public CommentResponse update(long id, CommentRequest request, String username) {
        Comment comment = requireOwnedComment(id, username);
        comment.update(request.content().trim(), OffsetDateTime.now(ZoneOffset.UTC));
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(long id, String username) { comments.delete(requireOwnedComment(id, username)); }

    private Comment requireComment(long id) {
        return comments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment was not found."));
    }

    private Comment requireOwnedComment(long id, String username) {
        Comment comment = requireComment(id);
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("Only the comment author may change this comment.");
        }
        return comment;
    }
}
