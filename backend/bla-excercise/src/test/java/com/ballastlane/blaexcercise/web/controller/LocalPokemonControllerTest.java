package com.ballastlane.blaexcercise.web.controller;

import com.ballastlane.blaexcercise.application.port.in.LocalPokemonService;
import com.ballastlane.blaexcercise.application.port.in.SyncPokemonService;
import com.ballastlane.blaexcercise.domain.exception.LocalPokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.exception.PokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.infra.security.SecurityConfig;
import com.ballastlane.blaexcercise.web.LocalPokemonController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @MockitoBean
    private LocalPokemonService localPokemonService;

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

    // ---------- list / get ----------

    @Test
    void listReturnsAPageOfLocalPokemon() throws Exception {
        when(localPokemonService.list(0, 20)).thenReturn(new PageResult<>(List.of(pikachu()), 0, 20, 1));

        mockMvc.perform(get("/api/local-pokemon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("pikachu"))
                .andExpect(jsonPath("$.content[0].tags[0]").value("electric"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getReturnsThePokemon() throws Exception {
        when(localPokemonService.get(1L)).thenReturn(pikachu());

        mockMvc.perform(get("/api/local-pokemon/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localizedName").value("Pikachu"))
                .andExpect(jsonPath("$.region").value("Kanto"));
    }

    @Test
    void getReturns404WhenItDoesNotExist() throws Exception {
        when(localPokemonService.get(99L)).thenThrow(new LocalPokemonNotFoundException(99L));

        mockMvc.perform(get("/api/local-pokemon/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Local pokemon not found"));
    }

    // ---------- US04: update custom fields ----------

    @Test
    void updateReturns200WithTheUpdatedPokemon() throws Exception {
        when(localPokemonService.updateCustomFields(1L, "Pikachu", "Kanto", Set.of("electric")))
                .thenReturn(pikachu());
        String body = """
                {"localizedName": "Pikachu", "region": "Kanto", "tags": ["electric"]}
                """;

        mockMvc.perform(put("/api/local-pokemon/{id}", 1).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localizedName").value("Pikachu"))
                .andExpect(jsonPath("$.tags[0]").value("electric"));
    }

    @Test
    void updateReturns400WhenFieldsAreInvalid() throws Exception {
        String body = """
                {"localizedName": "%s", "region": "Kanto", "tags": [""]}
                """.formatted("x".repeat(101));

        mockMvc.perform(put("/api/local-pokemon/{id}", 1).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.localizedName").exists());
        verifyNoInteractions(localPokemonService);
    }

    @Test
    void updateReturns404WhenItDoesNotExist() throws Exception {
        when(localPokemonService.updateCustomFields(99L, "Pikachu", null, Set.of()))
                .thenThrow(new LocalPokemonNotFoundException(99L));
        String body = """
                {"localizedName": "Pikachu", "tags": []}
                """;

        mockMvc.perform(put("/api/local-pokemon/{id}", 99).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    // ---------- delete ----------

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/local-pokemon/{id}", 1))
                .andExpect(status().isNoContent());
        verify(localPokemonService).delete(1L);
    }

    @Test
    void deleteReturns404WhenItDoesNotExist() throws Exception {
        doThrow(new LocalPokemonNotFoundException(99L)).when(localPokemonService).delete(99L);

        mockMvc.perform(delete("/api/local-pokemon/{id}", 99))
                .andExpect(status().isNotFound());
    }

    private static LocalPokemon pikachu() {
        Instant now = Instant.parse("2026-10-10T12:00:00Z");
        return new LocalPokemon(1L, 25, "pikachu", "https://img/25.png", "Electric mouse",
                "Pikachu", "Kanto", Set.of("electric"), now, now);
    }
}
