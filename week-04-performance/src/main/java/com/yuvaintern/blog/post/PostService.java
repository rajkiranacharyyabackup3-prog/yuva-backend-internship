package com.yuvaintern.blog.post;

import com.yuvaintern.blog.common.ResourceNotFoundException;
import com.yuvaintern.blog.common.BadRequestException;
import com.yuvaintern.blog.user.BlogUser;
import com.yuvaintern.blog.user.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class PostService {
    private final PostRepository posts;
    private final UserService users;

    public PostService(PostRepository posts, UserService users) {
        this.posts = posts;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<PostResponse> list() {
        return posts.findAllByOrderByCreatedAtDesc().stream().map(PostResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PostPageResponse listPage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("page must be 0 or greater and size must be between 1 and 100.");
        }
        Slice<Post> result = posts.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return new PostPageResponse(result.getContent().stream().map(PostResponse::from).toList(),
                page, size, result.hasNext());
    }

    @Transactional(readOnly = true)
    public PostResponse get(long id) {
        return PostResponse.from(requirePost(id));
    }

    @Transactional
    public PostResponse create(PostRequest request, String username) {
        BlogUser author = users.requireByUsername(username);
        return PostResponse.from(posts.save(new Post(clean(request.title()), clean(request.content()),
                author, OffsetDateTime.now(ZoneOffset.UTC))));
    }

    @Transactional
    public PostResponse update(long id, PostRequest request, String username) {
        Post post = requireOwnedPost(id, username);
        post.update(clean(request.title()), clean(request.content()), OffsetDateTime.now(ZoneOffset.UTC));
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(long id, String username) {
        posts.delete(requireOwnedPost(id, username));
    }

    private Post requirePost(long id) {
        return posts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post was not found."));
    }

    private Post requireOwnedPost(long id, String username) {
        Post post = requirePost(id);
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("Only the post author may change this post.");
        }
        return post;
    }

    private String clean(String value) { return value.trim(); }
}
