package com.ballastlane.blaexcercise.infra.persistence;

import com.ballastlane.blaexcercise.application.port.out.LocalPokemonRepository;
import com.ballastlane.blaexcercise.domain.exception.LocalPokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Output adapter: implements the LocalPokemonRepository port with Spring Data JPA.
 * Transactions are an infrastructure detail, so @Transactional lives here and not in the core.
 */
public class JpaLocalPokemonRepository implements LocalPokemonRepository {

    private final SpringDataLocalPokemonRepository jpaRepository;

    public JpaLocalPokemonRepository(SpringDataLocalPokemonRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public LocalPokemon save(LocalPokemon pokemon) {
        return pokemon.id() == null ? insert(pokemon) : updateCustomFields(pokemon);
    }

    private LocalPokemon insert(LocalPokemon pokemon) {
        try {
            LocalPokemonEntity saved = jpaRepository.saveAndFlush(LocalPokemonMapper.toEntity(pokemon));
            return LocalPokemonMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            // The unique constraint on pokeapi_id protects us when two requests sync the same Pokemon at once.
            throw new PokemonAlreadySyncedException(pokemon.pokeapiId());
        }
    }

    /**
     * Loads the row and changes only our own fields. Hibernate detects the changes ("dirty checking")
     * and runs the UPDATE; @PreUpdate refreshes updated_at. Official data and created_at are never touched.
     */
    private LocalPokemon updateCustomFields(LocalPokemon pokemon) {
        LocalPokemonEntity entity = jpaRepository.findById(pokemon.id())
                .orElseThrow(() -> new LocalPokemonNotFoundException(pokemon.id()));

        entity.setLocalizedName(pokemon.localizedName());
        entity.setRegion(pokemon.region());
        entity.getTags().clear();
        entity.getTags().addAll(pokemon.tags());

        return LocalPokemonMapper.toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    public boolean existsByPokeapiId(int pokeapiId) {
        return jpaRepository.existsByPokeapiId(pokeapiId);
    }

    @Override
    public Optional<LocalPokemon> findById(long id) {
        return jpaRepository.findById(id).map(LocalPokemonMapper::toDomain);
    }

    @Override
    public PageResult<LocalPokemon> findAll(int page, int size) {
        Page<LocalPokemonEntity> result = jpaRepository.findAll(PageRequest.of(page, size, Sort.by("id")));
        return new PageResult<>(
                result.getContent().stream().map(LocalPokemonMapper::toDomain).toList(),
                page,
                size,
                result.getTotalElements());
    }

    @Override
    @Transactional
    public boolean deleteById(long id) {
        if (!jpaRepository.existsById(id)) {
            return false;
        }
        jpaRepository.deleteById(id); // Hibernate also deletes the rows in local_pokemon_tag
        return true;
    }
}
