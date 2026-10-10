package com.ballastlane.blaexcercise;

import com.ballastlane.blaexcercise.application.port.in.SyncPokemonService;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole application with a real PostgreSQL (Docker): register -> login -> call a protected endpoint.
 * Only the sync use case is mocked, so the test does not depend on the real PokeAPI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

    private static final String SYNC_PIKACHU = "/api/local-pokemon/sync/pikachu";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SyncPokemonService syncPokemonService;

    @Test
    void registeredUserCanLogInAndCallAProtectedEndpoint() throws Exception {
        String credentials = """
                {"username": "ash", "password": "pikachu123"}
                """;
        when(syncPokemonService.sync("pikachu")).thenReturn(new LocalPokemon(1L, 25, "pikachu", null, null,
                null, null, Set.of(), Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginBody, "$.accessToken");

        mockMvc.perform(post(SYNC_PIKACHU))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(SYNC_PIKACHU).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(SYNC_PIKACHU).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated());
    }
}
