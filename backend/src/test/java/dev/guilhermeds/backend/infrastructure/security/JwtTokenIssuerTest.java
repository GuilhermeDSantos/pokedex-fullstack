package dev.guilhermeds.backend.infrastructure.security;

import dev.guilhermeds.backend.fixture.UserAccountFixture;
import dev.guilhermeds.backend.infrastructure.config.JwtConfig;
import dev.guilhermeds.backend.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenIssuerTest {

    private static final JwtProperties PROPERTIES =
        new JwtProperties("test-secret-with-at-least-32-bytes!!", Duration.ofHours(1), "pokemon-catalog");

    private final JwtConfig config = new JwtConfig();
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(config.jwtEncoder(PROPERTIES), PROPERTIES);

    @Test
    void shouldIssueATokenTheDecoderAcceptsWithTheUserAndExpiry() {
        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var account = UserAccountFixture.ash();

        var token = issuer.issue(account, now);
        var decoded = config.jwtDecoder(PROPERTIES).decode(token.value());

        assertThat(decoded.getSubject()).isEqualTo(account.getId().value().toString());
        assertThat(decoded.getClaimAsString("email")).isEqualTo("ash@pallet.town");
        assertThat(decoded.getIssuedAt()).isEqualTo(now);
        assertThat(decoded.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(1))).isEqualTo(token.expiresAt());
    }
}
