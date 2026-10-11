-- Demo data for the presentation. Only loaded with the "demo" profile (see application-demo.yaml),
-- so the tests always start with an empty database.
--
-- "R__" = repeatable migration: Flyway runs it after the versioned ones (V1, V2...) and runs it
-- again only if this file changes. ON CONFLICT DO NOTHING makes it safe to run more than once.

-- Demo user: username "user", password "user" (stored as a BCrypt hash, like any registered user).
INSERT INTO users (username, password_hash, created_at)
VALUES ('user', '$2a$10$HeDq2HNepS5QjXIM.WENPe5o7s1bm9/NLHTn.IENOD3TfOUwGHoY.', now())
ON CONFLICT (username) DO NOTHING;

-- Onix (#95) already in the local collection, with our own fields filled in.
INSERT INTO local_pokemon (pokeapi_id, name, image_url, description, localized_name, region, created_at, updated_at)
VALUES (95,
        'onix',
        'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/95.png',
        'As it grows, the stone portions of its body harden to become similar to a diamond, but colored black.',
        'Iwark',
        'Kanto',
        now(),
        now())
ON CONFLICT (pokeapi_id) DO NOTHING;

INSERT INTO local_pokemon_tag (local_pokemon_id, tag)
SELECT p.id, t.tag
FROM local_pokemon p
CROSS JOIN (VALUES ('rock'), ('ground'), ('gym-brock')) AS t(tag)
WHERE p.pokeapi_id = 95
ON CONFLICT DO NOTHING;
