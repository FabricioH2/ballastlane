package com.genai.tasks;

import com.genai.tasks.testsupport.PostgresTestcontainer;
import com.genai.tasks.user.infrastructure.persistence.SpringDataUserRepository;
import com.genai.tasks.user.infrastructure.persistence.UserJpaEntity;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof, against a real PostgreSQL and real JWTs, that a user can never see or change
 * another user's task, and that the API answers 404 so task ids are not leaked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainer.class)
class TaskOwnershipIsolationIT {

    private static final String PASSWORD = "correct horse battery staple";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private SpringDataUserRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void createUsersAndLogIn() throws Exception {
        String suffix = UUID.randomUUID().toString();
        aliceToken = registerAndLogIn("alice-" + suffix);
        bobToken = registerAndLogIn("bob-" + suffix);
    }

    @Test
    void userBCannotReadUpdateOrDeleteUserAsTask() throws Exception {
        String taskId = createTask(aliceToken, "Alice's secret plan");

        // Read: 404, and nothing about the task leaks into the error body.
        mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(bobToken)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(containsString("secret plan"))));

        // Update: 404, same answer as for an id that does not exist at all.
        mvc.perform(put("/api/tasks/{id}", taskId).header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Hijacked", "description": "pwned", "status": "DONE"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Delete: 404.
        mvc.perform(delete("/api/tasks/{id}", taskId).header("Authorization", bearer(bobToken)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Alice's task still exists and is untouched.
        mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Alice's secret plan"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void foreignTaskIdLooksExactlyLikeANonexistentOne() throws Exception {
        String taskId = createTask(aliceToken, "Alice's task");

        String foreign = mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(bobToken)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(get("/api/tasks/{id}", UUID.randomUUID())
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // Only "instance" (the request path) may differ.
        Map<String, Object> foreignBody = JsonPath.read(foreign, "$");
        Map<String, Object> missingBody = JsonPath.read(missing, "$");
        foreignBody.remove("instance");
        missingBody.remove("instance");
        assertThat(foreignBody).isEqualTo(missingBody);
    }

    @Test
    void listingReturnsOnlyTheCallersTasks() throws Exception {
        String aliceTask = createTask(aliceToken, "Alice only");
        String bobTask = createTask(bobToken, "Bob only");

        mvc.perform(get("/api/tasks").header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bobTask))
                .andExpect(content().string(not(containsString(aliceTask))));
    }

    @Test
    void ownerCannotHandTheirTaskToAnotherUserThroughPut() throws Exception {
        String taskId = createTask(aliceToken, "Mine forever");
        String bobId = subjectOf(bobToken);

        mvc.perform(put("/api/tasks/{id}", taskId).header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Mine forever", "status": "TODO", "ownerId": "%s"}
                                """.formatted(bobId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("ownerId"));

        mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(bobToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCanUpdateAndDeleteTheirOwnTask() throws Exception {
        String taskId = createTask(aliceToken, "Draft");

        mvc.perform(put("/api/tasks/{id}", taskId).header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Final\", \"status\": \"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.status").value("DONE"));

        mvc.perform(delete("/api/tasks/{id}", taskId).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void wrongPasswordIsRejectedWith401() throws Exception {
        String username = "carol-" + UUID.randomUUID();
        users.save(new UserJpaEntity(UUID.randomUUID(), username, passwordEncoder.encode(PASSWORD)));

        mvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"password\": \"wrong\"}".formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private String registerAndLogIn(String username) throws Exception {
        users.save(new UserJpaEntity(UUID.randomUUID(), username, passwordEncoder.encode(PASSWORD)));
        String body = mvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String createTask(String token, String title) throws Exception {
        String body = mvc.perform(post("/api/tasks").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String subjectOf(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        return JsonPath.read(payload, "$.sub");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
