package it.robfrank.linklift.adapter.out.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.out.JwtTokenPort;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

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
    Instant now = Instant.now();
    Instant expirationInstant = expirationTime.toInstant(ZoneOffset.UTC);

    return JWT.create()
      .withIssuer(ISSUER)
      .withSubject(user.id())
      .withClaim("username", user.username())
      .withClaim("email", user.email())
      .withClaim("firstName", user.firstName())
      .withClaim("lastName", user.lastName())
      .withClaim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
      .withClaim("nonce", UUID.randomUUID().toString())
      .withIssuedAt(Date.from(now))
      .withExpiresAt(Date.from(expirationInstant))
      .sign(algorithm);
  }

  @Override
  public String generateRefreshToken(User user, LocalDateTime expirationTime) {
    Instant now = Instant.now();
    Instant expirationInstant = expirationTime.toInstant(ZoneOffset.UTC);

    return JWT.create()
      .withIssuer(ISSUER)
      .withSubject(user.id())
      .withClaim("username", user.username())
      .withClaim("email", user.email())
      .withClaim("firstName", user.firstName())
      .withClaim("lastName", user.lastName())
      .withClaim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
      .withClaim("nonce", UUID.randomUUID().toString())
      .withIssuedAt(Date.from(now))
      .withExpiresAt(Date.from(expirationInstant))
      .sign(algorithm);
  }

  @Override
  public Optional<TokenClaims> validateToken(String token) {
    try {
      // Use Auth0 JWT's built-in verification which handles expiration automatically
      var verifier = JWT.require(algorithm).withIssuer(ISSUER).build();

      DecodedJWT decodedJWT = verifier.verify(token);

      // Extract claims
      var customClaims = new HashMap<String, Object>();
      var firstNameClaim = decodedJWT.getClaim("firstName");
      var lastNameClaim = decodedJWT.getClaim("lastName");
      customClaims.put("firstName", firstNameClaim != null ? firstNameClaim.asString() : null);
      customClaims.put("lastName", lastNameClaim != null ? lastNameClaim.asString() : null);

      // Handle issuedAt being null when not explicitly set
      Date issuedAtDate = decodedJWT.getIssuedAt();
      LocalDateTime issuedAt = issuedAtDate != null ? LocalDateTime.ofInstant(issuedAtDate.toInstant(), ZoneOffset.UTC) : LocalDateTime.now(ZoneOffset.UTC);

      Date expirationDate = decodedJWT.getExpiresAt();
      return Optional.of(
        new TokenClaims(
          decodedJWT.getSubject(),
          decodedJWT.getClaim("username").asString(),
          decodedJWT.getClaim("email").asString(),
          issuedAt,
          LocalDateTime.ofInstant(expirationDate.toInstant(), ZoneOffset.UTC),
          decodedJWT.getClaim(TOKEN_TYPE_CLAIM).asString(),
          customClaims
        )
      );
    } catch (JWTVerificationException e) {
      // Token validation failed (expired, invalid signature, malformed, etc.)
      // Auth0 JWT throws JWTVerificationException for all validation failures including expiration
      return Optional.empty();
    } catch (Exception e) {
      // Handle any other unexpected exceptions
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
