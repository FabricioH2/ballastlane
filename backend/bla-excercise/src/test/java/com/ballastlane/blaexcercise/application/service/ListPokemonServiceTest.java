package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.domain.exception.InvalidPaginationException;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListPokemonServiceTest {

    private final PokemonCatalog catalog = mock(PokemonCatalog.class);
    private final ListPokemonService service = new ListPokemonService(catalog);

    @Test
    void returnsPageFromCatalog() {
        var bulbasaur = new PokemonSummary(1, "bulbasaur", "https://img/1.png",
                "Seed Pokémon", 6.9, List.of("overgrow", "chlorophyll"));
        var expected = new PageResult<>(List.of(bulbasaur), 0, 20, 1302);
        when(catalog.findAll(0, 20)).thenReturn(expected);

        var result = service.list(0, 20);

        assertThat(result).isEqualTo(expected);
        assertThat(result.totalPages()).isEqualTo(66);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> service.list(-1, 20))
                .isInstanceOf(InvalidPaginationException.class);
        verifyNoInteractions(catalog);
    }

    @Test
    void rejectsSizeOutOfRange() {
        assertThatThrownBy(() -> service.list(0, 0))
                .isInstanceOf(InvalidPaginationException.class);
        assertThatThrownBy(() -> service.list(0, 101))
                .isInstanceOf(InvalidPaginationException.class);
        verifyNoInteractions(catalog);
    }

    @Test
    void acceptsBoundarySizes() {
        var empty = new PageResult<PokemonSummary>(List.of(), 0, 1, 0);
        when(catalog.findAll(0, 1)).thenReturn(empty);
        when(catalog.findAll(0, 100)).thenReturn(empty);

        assertThat(service.list(0, 1)).isEqualTo(empty);
        assertThat(service.list(0, 100)).isEqualTo(empty);
    }
}
