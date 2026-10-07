package dev.guilhermeds.backend.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

// @Cacheable is allowed only on PokeApiClient (D-012); the caches themselves are set up in application.yaml.
@Configuration
@EnableCaching
public class CacheConfig {
}
