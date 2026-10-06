package dev.guilhermeds.backend.infrastructure.security;

import dev.guilhermeds.backend.application.port.AccessToken;
import dev.guilhermeds.backend.application.port.TokenIssuer;
import dev.guilhermeds.backend.domain.model.UserAccount;
import dev.guilhermeds.backend.infrastructure.config.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public AccessToken issue(UserAccount account, Instant now) {
        var expiresAt = now.plus(properties.ttl());
        var claims = JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(account.getId().value().toString())
            .claim("email", account.getEmail().value())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, expiresAt);
    }
}
