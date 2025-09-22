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

import java.time.LocalDateTime;
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
                                contentType = ?
                                """,
                        link.id(),
                        link.url(),
                        link.title(),
                        link.description(),
                        link.extractedAt()
                                .truncatedTo(ChronoUnit.SECONDS)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        link.contentType()
                );
            });
            return link;
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to save link: " + link.url(), e);
        }
    }


    /**
     * Save a link and create an OwnsLink relationship to the specified user.
     * This method properly uses ArcadeDB's graph capabilities.
     */
    public Link saveLinkForUser(Link link, String userId) {
        try {
            database.transaction(() -> {
                // First, create the Link vertex
                database.command(
                        "sql",
                        """
                                INSERT INTO Link SET
                                id= ?,
                                url = ?,
                                title = ?,
                                description = ?,
                                extractedAt = ?,
                                contentType = ?
                                """,
                        link.id(),
                        link.url(),
                        link.title(),
                        link.description(),
                        link.extractedAt()
                                .truncatedTo(ChronoUnit.SECONDS)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        link.contentType()
                );

                // Then, create the OwnsLink relationship
                ResultSet resultSet = database.command(
                        "sql",
                        """
                                CREATE EDGE OwnsLink
                                FROM (SELECT FROM User WHERE id = ?)
                                TO (SELECT FROM Link WHERE id = ?)
                                SET createdAt = ?, accessLevel = 'OWNER'
                                """,
                        userId,
                        link.id(),
                        link.extractedAt()
                                .truncatedTo(ChronoUnit.SECONDS)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );

                System.out.println("resultSet.next().getEdge().get().getIn().asVertex().toJSON(true) = " + resultSet.next().getEdge().get().getInVertex().toJSON(true));
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
            // First, get the total count using graph traversal
            long totalCount = getTotalLinkCountForUser(userId);

            // Build the ORDER BY clause
            String orderClause = buildOrderClause(query.sortBy(), query.sortDirection());

            // Calculate offset
            int offset = query.page() * query.size();

            // Query for the actual data using graph traversal
            List<Link> links;
            if (userId != null) {
                // Use graph traversal to get user's links
                String sql = String.format("""
                                SELECT expand(out('OwnsLink'))
                                FROM User
                                WHERE id = ?
                                %s
                                SKIP %d
                                LIMIT %d
                                """,
                        orderClause, offset, query.size()
                );
                links = database
                        .query("sql", sql, userId)
                        .stream()
                        .map(Result::getVertex)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(linkMapper::mapToDomain)
                        .toList();
            } else {
                // Query all links (admin use case)
                String sql = String.format(
                        "SELECT FROM Link %s SKIP %d LIMIT %d",
                        orderClause, offset, query.size()
                );
                links = database
                        .query("sql", sql)
                        .stream()
                        .map(Result::getVertex)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
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
                // Use graph traversal to count user's links
                return database.query("sql",
                                """
                                        SELECT count(out('OwnsLink')) as count
                                        FROM User
                                        WHERE id = ?
                                        """,
                                userId)
                        .stream()
                        .findFirst()
                        .map(result -> result.getProperty("count"))
                        .map(count -> ((Number) count).longValue())
                        .orElse(0L);
            } else {
                // Count all links (admin use case)
                return database.query("sql", "SELECT count(*) as count FROM Link")
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

    /**
     * Find links owned by a specific user using graph traversal.
     * This method leverages ArcadeDB's graph capabilities for optimal performance.
     */
    public List<Link> findLinksByUserId(String userId) {
        try {
            return database.query("sql",
                            """
                                    SELECT expand(out('OwnsLink'))
                                    FROM User
                                    WHERE id = ?
                                    ORDER BY extractedAt DESC
                                    """,
                            userId)
                    .stream()
                    .map(Result::getVertex)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(linkMapper::mapToDomain)
                    .toList();
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to find links for user: " + userId, e);
        }
    }

    /**
     * Check if a user owns a specific link using graph traversal.
     */
    public boolean userOwnsLink(String userId, String linkId) {
        try {
            return database.query("sql",
                            """
                                    SELECT count(*) as count
                                    FROM User
                                    WHERE id = ?
                                    AND out('OwnsLink').id CONTAINS ?
                                    """,
                            userId, linkId)
                    .stream()
                    .findFirst()
                    .map(result -> result.<Integer>getProperty("count"))
                    .map(count -> count > 0)
                    .orElse(false);
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to check link ownership", e);
        }
    }

    /**
     * Get the owner of a specific link using graph traversal.
     */
    public Optional<String> getLinkOwner(String linkId) {
        try {
            return database
                    .query("sql",
                            """
                                    SELECT expand(in('OwnsLink').id)
                                    FROM Link
                                    WHERE id = ?
                                    """,
                            linkId)
                    .stream()
                    .findFirst()
                    .map(result -> result.getProperty("value"))
                    .map(String::valueOf);
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to get link owner for: " + linkId, e);
        }
    }

    /**
     * Delete a link and its relationships.
     */
    public void deleteLink(String linkId) {
        try {
            database.transaction(() -> {
                // Delete the link vertex (edges will be cascade deleted)
                database.command("sql", "DELETE FROM Link WHERE id = ?", linkId);
            });
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to delete link: " + linkId, e);
        }
    }

    /**
     * Transfer ownership of a link from one user to another.
     */
    public void transferLinkOwnership(String linkId, String fromUserId, String toUserId) {
        try {
//            "7666936a-4c4e-4acd-8146-ab1bf60088ec"
            System.out.println("linkId = " + linkId);
            database.transaction(() -> {
                // Delete existing ownership
                ResultSet resultSet = database.query("sql",
                        """
                                SELECT FROM OwnsLink
                                WHERE @out in (SELECT FROM User where id = '?')
                                AND @in in (SELECT FROM Link where id  = '?')
                                """,
                        fromUserId, linkId);

                System.out.println("resultSet.hasNext() = " + resultSet.next().getEdge().get().toJSON(true));
            });
            database.transaction(() -> {
                database.command("sql",
                        """
                                DELETE FROM OwnsLink
                                WHERE @in in (SELECT FROM Link WHERE id = '?')
                                AND @out in (SELECT FROM User WHERE id = '?')
                                """,
                        linkId, fromUserId);
            });

            database.transaction(() -> {
                // Create new ownership
                database.command("sql",
                        """
                                CREATE EDGE OwnsLink
                                FROM (SELECT FROM User WHERE id = '?')
                                TO (SELECT FROM Link WHERE id = '?')
                                SET createdAt = ?, accessLevel = 'OWNER'
                                """,
                        toUserId, linkId,
                        LocalDateTime.now()
                                .truncatedTo(ChronoUnit.SECONDS)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            });
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to transfer link ownership", e);
        }
    }

    private String buildOrderClause(String sortBy, String sortDirection) {
        // Map domain fields to database fields if needed
        String dbField = mapSortField(sortBy);
        return String.format("ORDER BY %s %s", dbField, sortDirection.toUpperCase());
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
