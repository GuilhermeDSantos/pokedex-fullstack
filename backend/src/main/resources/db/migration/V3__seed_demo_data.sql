-- Demo data (DL-2): a demo account and ten synced Pokémon, so the list starts with our fields.
-- The password is in the README; the hash was made by the app's own BCryptPasswordHasher.
-- Numbers and French names come from PokeAPI (pokemon-species); generation I's main region is Kanto.
-- Pikachu is left out on purpose: it is synced live in the demo. ON CONFLICT keeps a database that
-- already has one of these rows (a dev database) as it is.
INSERT INTO user_accounts (id, email, name, password_hash, created_at)
VALUES ('5eed0000-0000-0000-0000-000000000001', 'demo@pokemon.com', 'Demo Trainer',
        '$2a$10$BaeE0D8BuN3VGQ6vbEzkE.vI4bS9YHOUFhqDm0kh8Z2nEA3EnQenu', '2026-01-15T10:00:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO local_pokemons (id, pokedex_number, localized_name, region, synced_at, updated_at)
VALUES ('5eed0000-0000-0000-0001-000000000001', 1, 'Bulbizarre', 'Kanto', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
       ('5eed0000-0000-0000-0001-000000000004', 4, 'Salamèche', 'Kanto', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
       ('5eed0000-0000-0000-0001-000000000006', 6, 'Dracaufeu', 'Kanto', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
       ('5eed0000-0000-0000-0001-000000000007', 7, 'Carapuce', 'Kanto', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
       ('5eed0000-0000-0000-0001-000000000039', 39, 'Rondoudou', 'Kanto', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
       ('5eed0000-0000-0000-0001-000000000052', 52, 'Miaouss', 'Kanto', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
       ('5eed0000-0000-0000-0001-000000000054', 54, 'Psykokwak', NULL, '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
       ('5eed0000-0000-0000-0001-000000000094', 94, 'Ectoplasma', 'Kanto', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
       ('5eed0000-0000-0000-0001-000000000143', 143, NULL, 'Kanto', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
       ('5eed0000-0000-0000-0001-000000000149', 149, 'Dracolosse', 'Kanto', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z')
ON CONFLICT DO NOTHING;

-- Joined to local_pokemons, so a record skipped above gets no tags of ours.
INSERT INTO local_pokemon_tags (local_pokemon_id, tag)
SELECT seeded.local_pokemon_id, seeded.tag
FROM (VALUES ('5eed0000-0000-0000-0001-000000000001'::uuid, 'starter'),
             ('5eed0000-0000-0000-0001-000000000004'::uuid, 'starter'),
             ('5eed0000-0000-0000-0001-000000000006'::uuid, 'fan-favorite'),
             ('5eed0000-0000-0000-0001-000000000007'::uuid, 'starter'),
             ('5eed0000-0000-0000-0001-000000000039'::uuid, 'fan-favorite'),
             ('5eed0000-0000-0000-0001-000000000039'::uuid, 'singer'),
             ('5eed0000-0000-0000-0001-000000000052'::uuid, 'team-rocket'),
             ('5eed0000-0000-0000-0001-000000000094'::uuid, 'fan-favorite'),
             ('5eed0000-0000-0000-0001-000000000143'::uuid, 'blocker'),
             ('5eed0000-0000-0000-0001-000000000143'::uuid, 'sleepy'),
             ('5eed0000-0000-0000-0001-000000000149'::uuid, 'pseudo-legendary')) AS seeded (local_pokemon_id, tag)
JOIN local_pokemons ON local_pokemons.id = seeded.local_pokemon_id
ON CONFLICT DO NOTHING;
