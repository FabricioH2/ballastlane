package com.ballastlane.blaexcercise.web.controller;

import com.ballastlane.blaexcercise.application.port.in.SyncPokemonService;
import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.exception.PokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.infra.security.SecurityConfig;
import com.ballastlane.blaexcercise.web.LocalPokemonController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocalPokemonController.class)
@Import(SecurityConfig.class)
@WithMockUser
class LocalPokemonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SyncPokemonService syncPokemonService;

    @Test
    void syncReturns201WithLocationAndBody() throws Exception {
        Instant now = Instant.parse("2026-10-10T12:00:00Z");
        var pikachu = new LocalPokemon(1L, 25, "pikachu", "https://img/25.png", "Electric mouse",
                null, null, Set.of(), now, now);
        when(syncPokemonService.sync("pikachu")).thenReturn(pikachu);

        mockMvc.perform(post("/api/local-pokemon/sync/{idOrName}", "pikachu"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/local-pokemon/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.pokeapiId").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.createdAt").value("2026-10-10T12:00:00Z"));
    }

    @Test
    void syncReturns409WhenPokemonIsAlreadySynced() throws Exception {
        when(syncPokemonService.sync("pikachu")).thenThrow(new PokemonAlreadySyncedException(25));

        mockMvc.perform(post("/api/local-pokemon/sync/{idOrName}", "pikachu"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Pokemon already synced"));
    }

    @Test
    void syncReturns404WhenPokemonDoesNotExist() throws Exception {
        when(syncPokemonService.sync("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

        mockMvc.perform(post("/api/local-pokemon/sync/{idOrName}", "missingno"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser // overrides the class-level @WithMockUser: nobody is logged in
    void syncReturns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/local-pokemon/sync/{idOrName}", "pikachu"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(syncPokemonService);
    }
}
