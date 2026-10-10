package com.ballastlane.blaexcercise.infra.persistence;

import com.ballastlane.blaexcercise.application.port.out.UserRepository;
import com.ballastlane.blaexcercise.domain.exception.UsernameAlreadyTakenException;
import com.ballastlane.blaexcercise.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

/**
 * Output adapter: implements the UserRepository port with Spring Data JPA.
 */
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository jpaRepository;

    public JpaUserRepository(SpringDataUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        try {
            UserEntity saved = jpaRepository.saveAndFlush(new UserEntity(user.username(), user.passwordHash()));
            return toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            // The unique constraint on username protects us when two people register the same name at once.
            throw new UsernameAlreadyTakenException(user.username());
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(JpaUserRepository::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    private static User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getUsername(), entity.getPasswordHash(), entity.getCreatedAt());
    }
}
