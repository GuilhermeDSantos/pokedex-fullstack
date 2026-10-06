package dev.guilhermeds.backend.domain.model;

// The brief's "skills" (D-010). Comes from PokeAPI, so a blank name is a mapping bug, not a 400.
public record Ability(String name, boolean hidden) {

    public Ability {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ability name must not be blank");
        }
    }
}
