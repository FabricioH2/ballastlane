package com.ballastlane.blaexcercise.web.controller;

import com.ballastlane.blaexcercise.application.port.in.ListPokemonService;
import com.ballastlane.blaexcercise.application.port.in.PokemonDetailService;
import com.ballastlane.blaexcercise.domain.exception.InvalidPaginationException;
import com.ballastlane.blaexcercise.domain.exception.InvalidPokemonIdentifierException;
import com.ballastlane.blaexcercise.domain.exception.PokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemoDetail;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;
import com.ballastlane.blaexcercise.domain.model.Stat;
import com.ballastlane.blaexcercise.infra.security.SecurityConfig;
import com.ballastlane.blaexcercise.web.PokemonController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PokemonController.class)
@Import(SecurityConfig.class)
class PokemonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonService listPokemonService;

    @MockitoBean
    private PokemonDetailService pokemonDetailService;

    @Test
    void returnsFirstPageByDefault() throws Exception {
        var bulbasaur = new PokemonSummary(1, "bulbasaur", "https://img/1.png",
                "Seed Pokémon", 6.9, List.of("overgrow", "chlorophyll"));
        when(listPokemonService.list(0, 20))
                .thenReturn(new PageResult<>(List.of(bulbasaur), 0, 20, 1302));

        mockMvc.perform(get("/api/pokemon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$.content[0].weightKg").value(6.9))
                .andExpect(jsonPath("$.content[0].category").value("Seed Pokémon"))
                .andExpect(jsonPath("$.totalElements").value(1302))
                .andExpect(jsonPath("$.totalPages").value(66));
    }

    @Test
    void returns400WhenPaginationIsInvalid() throws Exception {
        when(listPokemonService.list(-1, 20))
                .thenThrow(new InvalidPaginationException("Page must be >= 0"));

        mockMvc.perform(get("/api/pokemon").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Page must be >= 0"));
    }

    @Test
    void returnsPokemonDetail() throws Exception {
        var bulbasaur = new PokemoDetail(
                1,
                "bulbasaur",
                "https://img/official-artwork/1.png",
                List.of(new Stat("hp", 45), new Stat("attack", 49)),
                "A strange seed was planted on its back at birth.",
                List.of("bulbasaur", "ivysaur", "venusaur"));
        when(pokemonDetailService.getDetail("bulbasaur")).thenReturn(bulbasaur);

        mockMvc.perform(get("/api/pokemon/{idOrName}", "bulbasaur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("bulbasaur"))
                .andExpect(jsonPath("$.imageUrl").value("https://img/official-artwork/1.png"))
                .andExpect(jsonPath("$.stats[0].name").value("hp"))
                .andExpect(jsonPath("$.stats[0].baseValue").value(45))
                .andExpect(jsonPath("$.description").value("A strange seed was planted on its back at birth."))
                .andExpect(jsonPath("$.evolutionChain[2]").value("venusaur"));
    }

    @Test
    void returns404WhenPokemonDoesNotExist() throws Exception {
        when(pokemonDetailService.getDetail("missingno"))
                .thenThrow(new PokemonNotFoundException("missingno"));

        mockMvc.perform(get("/api/pokemon/{idOrName}", "missingno"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pokemon not found"))
                .andExpect(jsonPath("$.detail").value("Pokemon not found: missingno"));
    }

    @Test
    void returns400WhenIdentifierIsInvalid() throws Exception {
        when(pokemonDetailService.getDetail("invalid"))
                .thenThrow(new InvalidPokemonIdentifierException("Pokemon id or name must not be blank"));

        mockMvc.perform(get("/api/pokemon/{idOrName}", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid pokemon identifier"));
    }
}
