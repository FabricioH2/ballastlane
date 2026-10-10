package com.ballastlane.blaexcercise.web.controller;

import com.ballastlane.blaexcercise.application.port.in.ListPokemonService;
import com.ballastlane.blaexcercise.domain.exception.InvalidPaginationException;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;
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
}
