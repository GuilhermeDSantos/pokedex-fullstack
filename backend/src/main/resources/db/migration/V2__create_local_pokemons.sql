-- Our records of Pokémon: the Pokédex number links to the canonical data, the rest is ours (D-039).
CREATE TABLE local_pokemons (
    id             UUID         NOT NULL,
    pokedex_number INTEGER      NOT NULL,
    localized_name VARCHAR(100),
    region         VARCHAR(100),
    synced_at      TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_local_pokemons PRIMARY KEY (id),
    CONSTRAINT uk_local_pokemons_pokedex_number UNIQUE (pokedex_number),
    CONSTRAINT ck_local_pokemons_pokedex_number CHECK (pokedex_number >= 1)
);

CREATE TABLE local_pokemon_tags (
    local_pokemon_id UUID        NOT NULL,
    tag              VARCHAR(30) NOT NULL,
    CONSTRAINT pk_local_pokemon_tags PRIMARY KEY (local_pokemon_id, tag),
    CONSTRAINT fk_local_pokemon_tags_local_pokemon FOREIGN KEY (local_pokemon_id)
        REFERENCES local_pokemons (id) ON DELETE CASCADE
);

-- Searching Pokémon by tag stays one indexed query.
CREATE INDEX ix_local_pokemon_tags_tag ON local_pokemon_tags (tag);
