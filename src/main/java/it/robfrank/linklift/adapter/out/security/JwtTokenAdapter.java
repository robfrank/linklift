package it.robfrank.linklift.adapter.out.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.out.JwtTokenPort;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

/**
 * Infrastructure adapter implementing JWT token operations using Auth0 JWT library.
 */
public class JwtTokenAdapter implements JwtTokenPort {

    private static final String ISSUER = "linklift";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final Algorithm algorithm;

    public JwtTokenAdapter(String secretKey) {
        this.algorithm = Algorithm.HMAC256(secretKey);
    }

    // Default constructor with fallback secret key (for backward compatibility)
    public JwtTokenAdapter() {
        this("default-secret-key-change-in-production");
    }

    @Override
    public String generateAccessToken(User user, LocalDateTime expirationTime) {
        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(user.id())
            .withClaim("username", user.username())
            .withClaim("email", user.email())
            .withClaim("firstName", user.firstName())
            .withClaim("lastName", user.lastName())
            .withClaim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .withIssuedAt(new Date())
            .withExpiresAt(Date.from(expirationTime.toInstant(ZoneOffset.UTC)))
            .sign(algorithm);
    }

    @Override
    public String generateRefreshToken(User user, LocalDateTime expirationTime) {
        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(user.id())
            .withClaim("username", user.username())
            .withClaim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
            .withIssuedAt(new Date())
            .withExpiresAt(Date.from(expirationTime.toInstant(ZoneOffset.UTC)))
            .sign(algorithm);
    }

    @Override
    public Optional<TokenClaims> validateToken(String token) {
        try {
            var verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();

            DecodedJWT decodedJWT = verifier.verify(token);

            // Extract claims
            var customClaims = new HashMap<String, Object>();
            customClaims.put("firstName", decodedJWT.getClaim("firstName").asString());
            customClaims.put("lastName", decodedJWT.getClaim("lastName").asString());

            return Optional.of(new TokenClaims(
                    decodedJWT.getSubject(),
                    decodedJWT.getClaim("username").asString(),
                    decodedJWT.getClaim("email").asString(),
                    LocalDateTime.ofInstant(decodedJWT.getIssuedAt().toInstant(), ZoneOffset.UTC),
                    LocalDateTime.ofInstant(decodedJWT.getExpiresAt().toInstant(), ZoneOffset.UTC),
                    decodedJWT.getClaim(TOKEN_TYPE_CLAIM).asString(),
                    customClaims
            ));

        } catch (JWTVerificationException e) {
            System.out.println("e.getMessage() = " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> extractUserIdFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return Optional.ofNullable(decodedJWT.getSubject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LocalDateTime> getTokenExpiration(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            Date expiresAt = decodedJWT.getExpiresAt();
            if (expiresAt != null) {
                return Optional.of(LocalDateTime.ofInstant(expiresAt.toInstant(), ZoneOffset.UTC));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
