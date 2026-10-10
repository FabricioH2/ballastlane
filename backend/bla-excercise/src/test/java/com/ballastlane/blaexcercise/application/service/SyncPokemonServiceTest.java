package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.in.PokemonDetailService;
import com.ballastlane.blaexcercise.application.port.out.LocalPokemonRepository;
import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.exception.PokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.domain.model.PokemonDetail;
import com.ballastlane.blaexcercise.domain.model.Stat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SyncPokemonServiceTest {

    private static final PokemonDetail PIKACHU = new PokemonDetail(
            25,
            "pikachu",
            "https://img/official-artwork/25.png",
            List.of(new Stat("hp", 35)),
            "It keeps its tail raised to monitor its surroundings.",
            List.of("pichu", "pikachu", "raichu"));

    private final PokemonDetailService pokemonDetailService = mock(PokemonDetailService.class);
    private final LocalPokemonRepository repository = mock(LocalPokemonRepository.class);
    private final SyncPokemonServiceImpl service = new SyncPokemonServiceImpl(pokemonDetailService, repository);

    @Test
    void savesPokemonFromCatalogWithEmptyCustomFields() {
        when(pokemonDetailService.getDetail("pikachu")).thenReturn(PIKACHU);
        when(repository.existsByPokeapiId(25)).thenReturn(false);
        when(repository.save(any(LocalPokemon.class))).thenAnswer(invocation -> saved(invocation.getArgument(0)));

        LocalPokemon result = service.sync("pikachu");

        ArgumentCaptor<LocalPokemon> captor = ArgumentCaptor.forClass(LocalPokemon.class);
        verify(repository).save(captor.capture());
        LocalPokemon toSave = captor.getValue();
        assertThat(toSave.id()).isNull();
        assertThat(toSave.pokeapiId()).isEqualTo(25);
        assertThat(toSave.name()).isEqualTo("pikachu");
        assertThat(toSave.imageUrl()).isEqualTo("https://img/official-artwork/25.png");
        assertThat(toSave.description()).isEqualTo("It keeps its tail raised to monitor its surroundings.");
        assertThat(toSave.localizedName()).isNull();
        assertThat(toSave.region()).isNull();
        assertThat(toSave.tags()).isEmpty();

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void rejectsPokemonThatIsAlreadySynced() {
        when(pokemonDetailService.getDetail("pikachu")).thenReturn(PIKACHU);
        when(repository.existsByPokeapiId(25)).thenReturn(true);

        assertThatThrownBy(() -> service.sync("pikachu"))
                .isInstanceOf(PokemonAlreadySyncedException.class)
                .hasMessageContaining("25");
        verify(repository, never()).save(any());
    }

    @Test
    void doesNotTouchTheDatabaseWhenPokemonDoesNotExist() {
        when(pokemonDetailService.getDetail("missingno"))
                .thenThrow(new PokemonNotFoundException("missingno"));

        assertThatThrownBy(() -> service.sync("missingno"))
                .isInstanceOf(PokemonNotFoundException.class);
        verifyNoInteractions(repository);
    }

    /** Simulates what the database does on save: assigns an id and timestamps. */
    private static LocalPokemon saved(LocalPokemon pokemon) {
        Instant now = Instant.parse("2026-10-10T12:00:00Z");
        return new LocalPokemon(1L, pokemon.pokeapiId(), pokemon.name(), pokemon.imageUrl(),
                pokemon.description(), pokemon.localizedName(), pokemon.region(), pokemon.tags(), now, now);
    }
}
