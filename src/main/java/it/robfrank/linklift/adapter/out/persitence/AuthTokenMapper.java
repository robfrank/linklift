package it.robfrank.linklift.adapter.out.persitence;

import com.arcadedb.graph.Vertex;
import it.robfrank.linklift.application.domain.model.AuthToken;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Maps AuthToken domain objects from ArcadeDB Vertex format.
 * Follows the established mapper pattern in the codebase.
 */
public class AuthTokenMapper {

    public AuthToken toDomainModel(Vertex vertex) {
        return new AuthToken(
            vertex.getString("id"),
            vertex.getString("token"),
            AuthToken.TokenType.valueOf(vertex.getString("tokenType")),
            vertex.getString("userId"),
            parseDateTime(vertex.getLocalDateTime("createdAt")),
            parseDateTime(vertex.getLocalDateTime("expiresAt")),
            parseDateTime(vertex.getLocalDateTime("usedAt")),
            vertex.getBoolean("isRevoked"),
            vertex.getString("ipAddress"),
            vertex.getString("userAgent")
        );
    }

    private LocalDateTime parseDateTime(Object value) {
        return switch (value) {
            case null -> null;
            case LocalDateTime localDateTime -> localDateTime;
            case String s -> LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            default -> throw new IllegalArgumentException("Cannot parse datetime from " + value.getClass());
        };
    }
}
