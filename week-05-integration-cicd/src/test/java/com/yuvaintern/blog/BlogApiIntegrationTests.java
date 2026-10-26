package com.yuvaintern.blog;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlogApiIntegrationTests {
    private static final String PASSWORD = "StrongPass123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void registrationValidatesInputAndDoesNotReturnPassword() throws Exception {
        Account account = register("Register");
        mvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(account.username(), account.email(), PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void postAndCommentCrudWorksWithPublicReadsAndAuthenticatedWrites() throws Exception {
        Account author = register("Crud");

        mvc.perform(post("/api/v1/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First post\",\"content\":\"A useful article.\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        MvcResult createdPost = mvc.perform(post("/api/v1/posts").with(httpBasic(author.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First post\",\"content\":\"A useful article.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorUsername").value(author.username()))
                .andReturn();
        long postId = idFrom(createdPost);

        mvc.perform(get("/api/v1/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("First post"));
        mvc.perform(put("/api/v1/posts/{id}", postId).with(httpBasic(author.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated post\",\"content\":\"Updated article.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated post"));

        MvcResult createdComment = mvc.perform(post("/api/v1/posts/{id}/comments", postId)
                        .with(httpBasic(author.username(), PASSWORD)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Helpful comment.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(postId))
                .andReturn();
        long commentId = idFrom(createdComment);

        mvc.perform(get("/api/v1/posts/{id}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Helpful comment."));
        mvc.perform(get("/api/v1/comments/{id}", commentId))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/comments/{id}", commentId).with(httpBasic(author.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Edited comment.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Edited comment."));
        mvc.perform(delete("/api/v1/comments/{id}", commentId).with(httpBasic(author.username(), PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/posts/{id}", postId).with(httpBasic(author.username(), PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/posts/{id}", postId))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyTheOwnerCanUpdateOrDeleteTheirResources() throws Exception {
        Account owner = register("Owner");
        Account other = register("Other");
        long postId = idFrom(mvc.perform(post("/api/v1/posts").with(httpBasic(owner.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Owned\",\"content\":\"Only the owner can edit.\"}"))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(put("/api/v1/posts/{id}", postId).with(httpBasic(other.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Stolen\",\"content\":\"Should be forbidden.\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("owner")));

        long commentId = idFrom(mvc.perform(post("/api/v1/posts/{id}/comments", postId)
                        .with(httpBasic(owner.username(), PASSWORD)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Owner comment.\"}"))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(delete("/api/v1/comments/{id}", commentId).with(httpBasic(other.username(), PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/posts/{id}", postId).with(httpBasic(owner.username(), PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/comments/{id}", commentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestValidationAndUnknownResourcesHaveStructuredErrors() throws Exception {
        Account author = register("Validation");
        mvc.perform(post("/api/v1/posts").with(httpBasic(author.username(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"content\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.content").exists());
        mvc.perform(get("/api/v1/posts/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/v1/posts/99999999"));
        mvc.perform(get("/api/v1/not-an-endpoint"))
                .andExpect(status().isNotFound());
    }

    private Account register(String prefix) throws Exception {
        String username = (prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .toLowerCase();
        String email = username + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(username, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        if (response.has("passwordHash")) throw new AssertionError("Password hash must never be returned.");
        return new Account(username, email);
    }

    private String registrationJson(String username, String email, String password) {
        return "{\"username\":\"" + username + "\",\"email\":\"" + email
                + "\",\"password\":\"" + password + "\"}";
    }

    private long idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private record Account(String username, String email) { }
}
