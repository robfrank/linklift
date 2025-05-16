package it.robfrank.linklift.adapter.out.persitence;

import com.arcadedb.exception.ArcadeDBException;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class ArcadeLinkRepository {

    private final RemoteDatabase database;
    private final LinkMapper linkMapper;

    public ArcadeLinkRepository(RemoteDatabase database, LinkMapper linkMapper) {
        this.linkMapper = linkMapper;
        this.database = database;
    }


    public Link saveLink(Link link) {
        database.begin();

        try {
            ResultSet resultSet = database.command(
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
            return resultSet.stream()
                    .findFirst()
                    .flatMap(Result::getVertex)
                    .map(linkMapper::mapToDomain)
                    .orElseThrow(() -> new DatabaseException("Failed to save link: " + link.url()));

        } catch (ArcadeDBException e) {
            database.rollback();
            throw new DatabaseException("Failed to save link: " + link.url(), e);
        } finally {
            database.commit();
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
}
