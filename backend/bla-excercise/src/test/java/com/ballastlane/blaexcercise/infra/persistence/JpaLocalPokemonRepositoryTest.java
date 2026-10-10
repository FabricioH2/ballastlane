package com.ballastlane.blaexcercise.infra.persistence;

import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against a real PostgreSQL in Docker, with the real Flyway migrations.
 * It also proves that the JPA entity matches the tables (ddl-auto: validate).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaLocalPokemonRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));

    @Autowired
    private SpringDataLocalPokemonRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private JpaLocalPokemonRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaLocalPokemonRepository(springDataRepository);
    }

    @Test
    void savesNewPokemonAndReadsItBackFromTheDatabase() {
        LocalPokemon saved = repository.save(pikachu(Set.of("electric", "starter")));

        entityManager.clear(); // forget the cached objects, so the next read really hits the database
        LocalPokemonEntity row = springDataRepository.findById(saved.id()).orElseThrow();

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.updatedAt()).isNotNull();
        assertThat(row.getPokeapiId()).isEqualTo(25);
        assertThat(row.getName()).isEqualTo("pikachu");
        assertThat(row.getLocalizedName()).isEqualTo("Pikachu");
        assertThat(row.getRegion()).isEqualTo("Kanto");
        assertThat(row.getTags()).containsExactlyInAnyOrder("electric", "starter");
    }

    @Test
    void knowsWhichPokemonAreAlreadySynced() {
        repository.save(pikachu(Set.of()));

        assertThat(repository.existsByPokeapiId(25)).isTrue();
        assertThat(repository.existsByPokeapiId(1)).isFalse();
    }

    @Test
    void rejectsTheSamePokemonTwice() {
        repository.save(pikachu(Set.of()));

        assertThatThrownBy(() -> repository.save(pikachu(Set.of())))
                .isInstanceOf(PokemonAlreadySyncedException.class);
    }

    @Test
    void findsPokemonById() {
        LocalPokemon saved = repository.save(pikachu(Set.of("electric")));
        entityManager.clear();

        assertThat(repository.findById(saved.id()))
                .hasValueSatisfying(found -> {
                    assertThat(found.name()).isEqualTo("pikachu");
                    assertThat(found.tags()).containsExactly("electric");
                });
        assertThat(repository.findById(999_999L)).isEmpty();
    }

    @Test
    void returnsPagesOrderedById() {
        LocalPokemon first = repository.save(pokemon(1, "bulbasaur"));
        LocalPokemon second = repository.save(pokemon(4, "charmander"));
        LocalPokemon third = repository.save(pokemon(7, "squirtle"));
        entityManager.clear();

        PageResult<LocalPokemon> page0 = repository.findAll(0, 2);
        PageResult<LocalPokemon> page1 = repository.findAll(1, 2);

        assertThat(page0.items()).extracting(LocalPokemon::id).containsExactly(first.id(), second.id());
        assertThat(page1.items()).extracting(LocalPokemon::id).containsExactly(third.id());
        assertThat(page0.totalElements()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);
    }

    @Test
    void updatesOnlyTheCustomFields() {
        LocalPokemon saved = repository.save(pikachu(Set.of("electric")));
        entityManager.clear();
        Instant createdAtInDatabase = springDataRepository.findById(saved.id()).orElseThrow().getCreatedAt();
        entityManager.clear();

        // even if someone sends other official data, the adapter only copies our own fields
        LocalPokemon changed = new LocalPokemon(saved.id(), 999, "hacked", null, null,
                "Pika", "Johto", Set.of("mouse", "yellow"), null, null);
        LocalPokemon updated = repository.save(changed);
        entityManager.clear();
        LocalPokemonEntity row = springDataRepository.findById(saved.id()).orElseThrow();

        assertThat(row.getLocalizedName()).isEqualTo("Pika");
        assertThat(row.getRegion()).isEqualTo("Johto");
        assertThat(row.getTags()).containsExactlyInAnyOrder("mouse", "yellow");
        assertThat(row.getPokeapiId()).isEqualTo(25);
        assertThat(row.getName()).isEqualTo("pikachu");
        assertThat(row.getImageUrl()).isEqualTo("https://img/25.png");
        assertThat(row.getCreatedAt()).isEqualTo(createdAtInDatabase);
        assertThat(updated.updatedAt()).isAfter(saved.updatedAt());
    }

    @Test
    void deletesThePokemonAndItsTags() {
        LocalPokemon saved = repository.save(pikachu(Set.of("electric", "starter")));
        entityManager.clear();

        assertThat(repository.deleteById(saved.id())).isTrue();
        entityManager.flush();

        assertThat(springDataRepository.findById(saved.id())).isEmpty();
        Number tagRows = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM local_pokemon_tag WHERE local_pokemon_id = :id")
                .setParameter("id", saved.id())
                .getSingleResult();
        assertThat(tagRows.longValue()).isZero();
    }

    @Test
    void deleteReturnsFalseWhenThePokemonDoesNotExist() {
        assertThat(repository.deleteById(999_999L)).isFalse();
    }

    private static LocalPokemon pikachu(Set<String> tags) {
        return new LocalPokemon(null, 25, "pikachu", "https://img/25.png", "Electric mouse",
                "Pikachu", "Kanto", tags, null, null);
    }

    private static LocalPokemon pokemon(int pokeapiId, String name) {
        return new LocalPokemon(null, pokeapiId, name, null, null, null, null, Set.of(), null, null);
    }
}
