package com.genai.tasks.user.infrastructure.persistence;

import com.genai.tasks.user.application.port.out.UserRepository;
import com.genai.tasks.user.domain.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpa;

    JpaUserRepositoryAdapter(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(UserJpaEntity::toDomain);
    }
}
