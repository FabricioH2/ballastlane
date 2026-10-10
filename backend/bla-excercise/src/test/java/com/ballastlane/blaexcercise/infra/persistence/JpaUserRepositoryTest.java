package com.ballastlane.blaexcercise.infra.persistence;

import com.ballastlane.blaexcercise.domain.exception.UsernameAlreadyTakenException;
import com.ballastlane.blaexcercise.domain.model.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaUserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));

    @Autowired
    private SpringDataUserRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private JpaUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaUserRepository(springDataRepository);
    }

    @Test
    void savesUserAndFindsItByUsername() {
        User saved = repository.save(User.newUser("ash", "hashed-password"));

        entityManager.clear(); // the next read really hits the database
        User found = repository.findByUsername("ash").orElseThrow();

        assertThat(saved.id()).isNotNull();
        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.passwordHash()).isEqualTo("hashed-password");
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void knowsWhichUsernamesExist() {
        repository.save(User.newUser("ash", "hashed-password"));

        assertThat(repository.existsByUsername("ash")).isTrue();
        assertThat(repository.existsByUsername("misty")).isFalse();
        assertThat(repository.findByUsername("misty")).isEmpty();
    }

    @Test
    void rejectsTheSameUsernameTwice() {
        repository.save(User.newUser("ash", "hashed-password"));

        assertThatThrownBy(() -> repository.save(User.newUser("ash", "another-hash")))
                .isInstanceOf(UsernameAlreadyTakenException.class);
    }
}
