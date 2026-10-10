package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.out.LocalPokemonRepository;
import com.ballastlane.blaexcercise.domain.exception.InvalidPaginationException;
import com.ballastlane.blaexcercise.domain.exception.LocalPokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalPokemonServiceTest {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Instant CREATED = Instant.parse("2026-10-10T12:00:00Z");
    private static final LocalPokemon PIKACHU = new LocalPokemon(1L, 25, "pikachu", "https://img/25.png",
            "Electric mouse", null, null, Set.of(), CREATED, CREATED);

    private final LocalPokemonRepository repository = mock(LocalPokemonRepository.class);
    private final LocalPokemonServiceImpl service = new LocalPokemonServiceImpl(repository, MAX_PAGE_SIZE);

    @Test
    void listReturnsThePageFromTheRepository() {
        var page = new PageResult<>(List.of(PIKACHU), 0, 20, 1);
        when(repository.findAll(0, 20)).thenReturn(page);

        assertThat(service.list(0, 20)).isEqualTo(page);
    }

    @Test
    void listRejectsInvalidPaginationWithoutQueryingTheDatabase() {
        assertThatThrownBy(() -> service.list(0, MAX_PAGE_SIZE + 1)).isInstanceOf(InvalidPaginationException.class);
        assertThatThrownBy(() -> service.list(-1, 20)).isInstanceOf(InvalidPaginationException.class);
        verify(repository, never()).findAll(anyInt(), anyInt());
    }

    @Test
    void getReturnsThePokemon() {
        when(repository.findById(1L)).thenReturn(Optional.of(PIKACHU));

        assertThat(service.get(1L)).isEqualTo(PIKACHU);
    }

    @Test
    void getThrowsNotFoundWhenItDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(LocalPokemonNotFoundException.class);
    }

    @Test
    void updateSavesNewCustomFieldsAndKeepsOfficialData() {
        when(repository.findById(1L)).thenReturn(Optional.of(PIKACHU));
        when(repository.save(any(LocalPokemon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalPokemon result = service.updateCustomFields(1L, "Pikachu", "Kanto", Set.of("Electric"));

        ArgumentCaptor<LocalPokemon> saved = ArgumentCaptor.forClass(LocalPokemon.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(1L);
        assertThat(saved.getValue().name()).isEqualTo("pikachu");
        assertThat(saved.getValue().localizedName()).isEqualTo("Pikachu");
        assertThat(saved.getValue().region()).isEqualTo("Kanto");
        assertThat(saved.getValue().tags()).containsExactly("electric");
        assertThat(result).isEqualTo(saved.getValue());
    }

    @Test
    void updateThrowsNotFoundAndSavesNothing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCustomFields(99L, "X", null, Set.of()))
                .isInstanceOf(LocalPokemonNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRemovesThePokemon() {
        when(repository.deleteById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteThrowsNotFoundWhenNothingWasDeleted() {
        when(repository.deleteById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(LocalPokemonNotFoundException.class);
    }
}
