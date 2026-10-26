package com.yuvaintern.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

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
class Week5IntegrationTests {
    private static final String PASSWORD = "PipelinePass123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void authenticationPersistenceValidationAndOwnershipWorkAcrossRequests() throws Exception {
        String owner = register("owner");
        String reader = register("reader");

        mvc.perform(post("/api/v1/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Unauthenticated\",\"content\":\"Should be rejected.\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        MvcResult created = mvc.perform(post("/api/v1/posts").with(httpBasic(owner, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Pipeline article\",\"content\":\"Saved through HTTP.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorUsername").value(owner))
                .andReturn();
        long postId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/v1/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pipeline article"));

        mvc.perform(put("/api/v1/posts/{id}", postId).with(httpBasic(reader, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Not allowed\",\"content\":\"The other user cannot edit.\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/posts").with(httpBasic(owner, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"content\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        mvc.perform(get("/api/v1/posts/{id}", 987654321))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String register(String prefix) throws Exception {
        String username = "w5_" + prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + username
                                + "@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        return username;
    }
}
