package com.yuvaintern.blog.comment;

import com.yuvaintern.blog.post.Post;
import com.yuvaintern.blog.user.BlogUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private BlogUser author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Comment() {
    }

    public Comment(Post post, BlogUser author, String content, OffsetDateTime now) {
        this.post = post;
        this.author = author;
        this.content = content;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String content, OffsetDateTime now) {
        this.content = content;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public BlogUser getAuthor() { return author; }
    public String getContent() { return content; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
