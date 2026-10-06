package dev.guilhermeds.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

@Configuration
public class SecurityConfig {

    // Health is Docker's healthcheck: once this chain exists, Boot's default actuator security backs off.
    private static final RequestMatcher PUBLIC_ROUTES = new OrRequestMatcher(
        withDefaults().matcher("/actuator/health"),
        withDefaults().matcher("/actuator/health/**"),
        withDefaults().matcher(HttpMethod.GET, "/v3/api-docs/**"),
        withDefaults().matcher(HttpMethod.GET, "/swagger-ui.html"),
        withDefaults().matcher(HttpMethod.GET, "/swagger-ui/**"),
        withDefaults().matcher(HttpMethod.GET, "/api/v1/pokemon/**"),
        withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/register"),
        withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/login"));

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint)
        throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(routes -> routes
                .requestMatchers(PUBLIC_ROUTES).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer
                .bearerTokenResolver(ignoringPublicRoutes())
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(authenticationEntryPoint))
            .build();
    }

    // Otherwise an expired token left in the browser turns a public page into a 401 (D-036).
    private static BearerTokenResolver ignoringPublicRoutes() {
        var defaultResolver = new DefaultBearerTokenResolver();
        return request -> PUBLIC_ROUTES.matches(request) ? null : defaultResolver.resolve(request);
    }
}
