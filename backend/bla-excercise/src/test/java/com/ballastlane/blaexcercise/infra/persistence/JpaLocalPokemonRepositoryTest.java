package com.ballastlane.blaexcercise.infra.persistence;

import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
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

    private static LocalPokemon pikachu(Set<String> tags) {
        return new LocalPokemon(null, 25, "pikachu", "https://img/25.png", "Electric mouse",
                "Pikachu", "Kanto", tags, null, null);
    }
}
