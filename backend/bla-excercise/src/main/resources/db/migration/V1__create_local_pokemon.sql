-- Pokemon synced from the PokeAPI into our own database (US03).
CREATE TABLE local_pokemon (
    id             BIGSERIAL                PRIMARY KEY,
    pokeapi_id     INTEGER                  NOT NULL,
    name           VARCHAR(100)             NOT NULL,
    image_url      VARCHAR(500),
    description    TEXT,
    localized_name VARCHAR(100),
    region         VARCHAR(100),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_local_pokemon_pokeapi_id UNIQUE (pokeapi_id)
);

-- Internal tags: one row per tag. Deleting a Pokemon deletes its tags.
CREATE TABLE local_pokemon_tag (
    local_pokemon_id BIGINT      NOT NULL REFERENCES local_pokemon (id) ON DELETE CASCADE,
    tag              VARCHAR(50) NOT NULL,
    PRIMARY KEY (local_pokemon_id, tag)
);
