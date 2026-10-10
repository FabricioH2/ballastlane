package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.domain.exception.InvalidPokemonIdentifierException;
import com.ballastlane.blaexcercise.domain.exception.PokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.model.PokemonDetail;
import com.ballastlane.blaexcercise.domain.model.Stat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class GetPokemonDetailServiceTest {

    private static final PokemonDetail BULBASAUR = new PokemonDetail(
            1,
            "bulbasaur",
            "https://img/1.png",
            List.of(new Stat("hp", 45), new Stat("attack", 49)),
            "A strange seed was planted on its back at birth.",
            List.of("bulbasaur", "ivysaur", "venusaur"));

    private final PokemonCatalog catalog = mock(PokemonCatalog.class);
    private final GetPokemonDetailServiceImpl service = new GetPokemonDetailServiceImpl(catalog);

    @Test
    void returnsDetailFromCatalog() {
        when(catalog.findByIdOrName("bulbasaur")).thenReturn(Optional.of(BULBASAUR));

        assertThat(service.getDetail("bulbasaur")).isEqualTo(BULBASAUR);
    }

    @Test
    void normalizesIdentifierBeforeSearching() {
        when(catalog.findByIdOrName("bulbasaur")).thenReturn(Optional.of(BULBASAUR));

        assertThat(service.getDetail("  Bulbasaur ")).isEqualTo(BULBASAUR);
        verify(catalog).findByIdOrName("bulbasaur");
    }

    @Test
    void throwsNotFoundWhenCatalogHasNoPokemon() {
        when(catalog.findByIdOrName("missingno")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail("missingno"))
                .isInstanceOf(PokemonNotFoundException.class)
                .hasMessageContaining("missingno");
    }

    @Test
    void rejectsBlankIdentifier() {
        assertThatThrownBy(() -> service.getDetail("   "))
                .isInstanceOf(InvalidPokemonIdentifierException.class);
        verifyNoInteractions(catalog);
    }
}
