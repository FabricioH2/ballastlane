package com.ballastlane.blaexcercise.infra.security;

import com.ballastlane.blaexcercise.application.port.out.TokenIssuer;
import com.ballastlane.blaexcercise.domain.model.AuthToken;
import com.ballastlane.blaexcercise.domain.model.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Output adapter: implements TokenIssuer by creating a JWT signed with HS256.
 */
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public JwtTokenIssuer(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    @Override
    public AuthToken issueFor(User user) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS); // JWT dates have second precision
        Instant expiresAt = now.plus(properties.expiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.username())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthToken(token, expiresAt);
    }
}
