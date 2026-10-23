package com.yuvaintern.blog;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Week3RegressionTests {
    private static final String PASSWORD = "DebugPass123!";

    @Autowired MockMvc mvc;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void emailUniquenessIsCaseInsensitive() throws Exception {
        Account first = register("Email", null);
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(uniqueUsername("EmailAgain"), first.email().toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void missingPostDoesNotReturnAnEmptyCommentList() throws Exception {
        mvc.perform(get("/api/v1/posts/{id}/comments", 99999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post was not found."));
    }

    @Test
    void postOwnerCanUpdateTheirPost() throws Exception {
        Account owner = register("PostOwner", null);
        long postId = createPost(owner);
        mvc.perform(put("/api/v1/posts/{id}", postId).with(httpBasic(owner.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated by owner\",\"content\":\"Expected to work.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated by owner"));
    }

    @Test
    void anotherUserCannotUpdateThePost() throws Exception {
        Account owner = register("PostOwnerOther", null);
        Account other = register("PostVisitor", null);
        long postId = createPost(owner);
        mvc.perform(put("/api/v1/posts/{id}", postId).with(httpBasic(other.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Unauthorized\",\"content\":\"Must stay unchanged.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void commentOwnerCanUpdateTheirComment() throws Exception {
        Account owner = register("CommentOwner", null);
        long postId = createPost(owner);
        long commentId = createComment(owner, postId);
        mvc.perform(put("/api/v1/comments/{id}", commentId).with(httpBasic(owner.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Updated by author.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated by author."));
    }

    @Test
    void anotherUserCannotUpdateTheComment() throws Exception {
        Account owner = register("CommentOwnerOther", null);
        Account other = register("CommentVisitor", null);
        long postId = createPost(owner);
        long commentId = createComment(owner, postId);
        mvc.perform(put("/api/v1/comments/{id}", commentId).with(httpBasic(other.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Unauthorized.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listingPostsUsesOneQueryForAuthorsInsteadOfOneQueryPerAuthor() throws Exception {
        for (int i = 0; i < 5; i++) {
            Account author = register("QueryAuthor" + i, null);
            createPost(author);
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorUsername").exists());

        long selects = statistics.getPrepareStatementCount();
        org.junit.jupiter.api.Assertions.assertEquals(1L, selects,
                "The list query should load each post and its author together.");
    }

    private Account register(String prefix, String selectedEmail) throws Exception {
        String username = uniqueUsername(prefix);
        String email = selectedEmail == null ? username + "@example.com" : selectedEmail;
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(username, email, PASSWORD)))
                .andExpect(status().isCreated());
        return new Account(username, email);
    }

    private long createPost(Account owner) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/posts").with(httpBasic(owner.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Debug post\",\"content\":\"Baseline test fixture.\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private long createComment(Account owner, long postId) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/posts/{id}/comments", postId)
                        .with(httpBasic(owner.username(), PASSWORD)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Baseline comment.\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private long idFrom(MvcResult result) throws Exception {
        return new tools.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String registrationJson(String username, String email, String password) {
        return "{\"username\":\"" + username + "\",\"email\":\"" + email
                + "\",\"password\":\"" + password + "\"}";
    }

    private String uniqueUsername(String prefix) {
        return (prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)).toLowerCase();
    }

    private record Account(String username, String email) { }
}
