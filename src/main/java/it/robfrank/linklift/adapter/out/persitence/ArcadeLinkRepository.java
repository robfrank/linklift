package it.robfrank.linklift.adapter.out.persitence;

import com.arcadedb.exception.ArcadeDBException;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class ArcadeLinkRepository {

    private final RemoteDatabase database;
    private final LinkMapper linkMapper;

    public ArcadeLinkRepository(RemoteDatabase database, LinkMapper linkMapper) {
        this.linkMapper = linkMapper;
        this.database = database;
    }


    public Link saveLink(Link link) {
        try {
            database.transaction(() -> {
                database.command(
                        "sql",
                        """
                                INSERT INTO Link SET
                                id= ?,
                                url = ?,
                                title = ?,
                                description = ?,
                                extractedAt = ?,
                                contentType = ?,
                                userId = ?
                                """,
                        link.id(),
                        link.url(),
                        link.title(),
                        link.description(),
                        link.extractedAt()
                                .truncatedTo(ChronoUnit.SECONDS)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        link.contentType(),
                        link.userId()
                );
            });
            return link;
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to save link: " + link.url(), e);
        }
    }


    public Link saveLink2(Link link) {
        try {


            database.transaction(() -> {
                ResultSet resultSet = database.command(
                        "sql",
                        """
                                INSERT INTO Link SET
                                id= ?,
                                url = ?,
                                title = ?,
                                description = ?,
                                extractedAt = ?,
                                contentType = ?,
                                userId = ?
                                """,
                        link.id(),
                        link.url(),
                        link.title(),
                        link.description(),
                        link.extractedAt()
                                .truncatedTo(ChronoUnit.SECONDS)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        link.contentType(),
                        link.userId()
                );
            });
            return link;
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to save link: " + link.url(), e);
        }
    }

    public Optional<Link> findLinkByUrl(String url) {
        try {
            return database
                    .query("sql", "SELECT FROM Link WHERE url = ?", url)
                    .stream()
                    .findFirst()
                    .flatMap(Result::getVertex)
                    .map(linkMapper::mapToDomain)
                    .or(Optional::empty);
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to find link by URL: " + url, e);
        }
    }

    public Link getLinkByUrl(String url) {
        return findLinkByUrl(url).orElseThrow(() -> new LinkNotFoundException("No link found with URL: " + url));
    }

    public Optional<Link> findLinkById(String id) {
        try {
            return database
                    .query("sql", "SELECT FROM Link WHERE id = ?", id)
                    .stream()
                    .findFirst()
                    .flatMap(Result::getVertex)
                    .map(linkMapper::mapToDomain)
                    .or(Optional::empty);
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to find link by ID: " + id, e);
        }
    }

    public Link getLinkById(String id) {
        return findLinkById(id).orElseThrow(() -> new LinkNotFoundException(id));
    }

    public LinkPage findLinksWithPagination(ListLinksQuery query) {
        // Use the userId from the query to filter user-specific links
        return findLinksWithPaginationForUser(query, query.userId());
    }

    public LinkPage findLinksWithPaginationForUser(ListLinksQuery query, String userId) {
        try {
            // First, get the total count
            long totalCount = getTotalLinkCountForUser(userId);

            // Build the ORDER BY clause
            String orderClause = buildOrderClause(query.sortBy(), query.sortDirection());

            // Calculate offset
            int offset = query.page() * query.size();

            // Query for the actual data
            List<Link> links;
            if (userId != null) {
                String sql = "SELECT FROM Link WHERE userId = ? %s SKIP %d LIMIT %d".formatted(
                    orderClause, offset, query.size()
                );
                links = database
                    .query("sql", sql, userId)
                    .stream()
                    .map(Result::getVertex)
                    .flatMap(Optional::stream)
                    .map(linkMapper::mapToDomain)
                    .toList();
            } else {
                String sql = "SELECT FROM Link %s SKIP %d LIMIT %d".formatted(
                    orderClause, offset, query.size()
                );
                links = database
                    .query("sql", sql)
                    .stream()
                    .map(Result::getVertex)
                    .flatMap(Optional::stream)
                    .map(linkMapper::mapToDomain)
                    .toList();
            }

            return new LinkPage(
                links,
                query.page(),
                query.size(),
                totalCount,
                0, // Will be calculated in constructor
                false, // Will be calculated in constructor
                false // Will be calculated in constructor
            );

        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to load links with pagination", e);
        }
    }

    private long getTotalLinkCount() {
        return getTotalLinkCountForUser(null);
    }

    private long getTotalLinkCountForUser(String userId) {
        try {
            if (userId != null) {
                return database
                    .query("sql", "SELECT count(*) as count FROM Link WHERE userId = ?", userId)
                    .stream()
                    .findFirst()
                    .map(result -> result.getProperty("count"))
                    .map(count -> ((Number) count).longValue())
                    .orElse(0L);
            } else {
                return database
                    .query("sql", "SELECT count(*) as count FROM Link")
                    .stream()
                    .findFirst()
                    .map(result -> result.getProperty("count"))
                    .map(count -> ((Number) count).longValue())
                    .orElse(0L);
            }
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to count total links", e);
        }
    }

    private String buildOrderClause(String sortBy, String sortDirection) {
        // Map domain fields to database fields if needed
        String dbField = mapSortField(sortBy);
        return "ORDER BY %s %s".formatted(dbField, sortDirection.toUpperCase());
    }

    private String mapSortField(String sortBy) {
        // In ArcadeDB, we can directly use the field names as they match our domain model
        return switch (sortBy) {
            case "id" -> "id";
            case "url" -> "url";
            case "title" -> "title";
            case "description" -> "description";
            case "extractedAt" -> "extractedAt";
            case "contentType" -> "contentType";
            default -> "extractedAt"; // Default fallback
        };
    }
}
