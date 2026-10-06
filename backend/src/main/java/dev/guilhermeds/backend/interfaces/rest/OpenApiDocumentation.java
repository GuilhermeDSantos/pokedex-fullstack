package dev.guilhermeds.backend.interfaces.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Pokémon Catalog API", version = "v1"))
@SecurityScheme(name = OpenApiDocumentation.BEARER_JWT, type = SecuritySchemeType.HTTP, scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiDocumentation {

    public static final String BEARER_JWT = "bearer-jwt";
}
