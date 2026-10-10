package com.ballastlane.blaexcercise.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPokemonTest {

    private static final Instant CREATED = Instant.parse("2026-10-10T12:00:00Z");

    private static final LocalPokemon PIKACHU = new LocalPokemon(1L, 25, "pikachu", "https://img/25.png",
            "Electric mouse", null, null, Set.of(), CREATED, CREATED);

    @Test
    void changesOnlyTheCustomFields() {
        LocalPokemon updated = PIKACHU.withCustomFields("Pikachu", "Kanto", Set.of("electric"));

        assertThat(updated.localizedName()).isEqualTo("Pikachu");
        assertThat(updated.region()).isEqualTo("Kanto");
        assertThat(updated.tags()).containsExactly("electric");
        // official data and identity stay the same
        assertThat(updated.id()).isEqualTo(1L);
        assertThat(updated.pokeapiId()).isEqualTo(25);
        assertThat(updated.name()).isEqualTo("pikachu");
        assertThat(updated.imageUrl()).isEqualTo("https://img/25.png");
        assertThat(updated.description()).isEqualTo("Electric mouse");
        assertThat(updated.createdAt()).isEqualTo(CREATED);
    }

    @Test
    void normalizesTagsAndDropsBlankOnes() {
        Set<String> tags = new HashSet<>(Set.of(" Electric ", "ELECTRIC", "starter", "  "));
        tags.add(null);

        LocalPokemon updated = PIKACHU.withCustomFields("Pikachu", "Kanto", tags);

        assertThat(updated.tags()).containsExactlyInAnyOrder("electric", "starter");
    }

    @Test
    void blankTextAndNullTagsClearTheFields() {
        LocalPokemon withData = PIKACHU.withCustomFields("Pikachu", "Kanto", Set.of("electric"));

        LocalPokemon cleared = withData.withCustomFields("  ", null, null);

        assertThat(cleared.localizedName()).isNull();
        assertThat(cleared.region()).isNull();
        assertThat(cleared.tags()).isEmpty();
    }
}
