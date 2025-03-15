package it.robfrank.linklift.adapter.out.persitence;

import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.model.Link;
import java.util.Optional;

public class ArcadeLinkRepository {

  private final RemoteDatabase database;
  private final LinkMapper linkMapper;

  public ArcadeLinkRepository(RemoteDatabase database, LinkMapper linkMapper) {
    this.linkMapper = linkMapper;
    this.database = database;
  }

  public Link saveLink(Link link) {
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
        link.extractedAt(),
        link.contentType()
      );
    });
    return link;
  }

  public Optional<Link> findLinkByUrl(String url) {
    return database
      .query("sql", "SELECT FROM Link WHERE url = ?", url)
      .stream()
      .findFirst()
      .flatMap(Result::getVertex)
      .map(linkMapper::mapToDomain)
      .or(Optional::empty);
  }

  public Optional<Link> findLinkById(String id) {
    return database
      .query("sql", "SELECT FROM Link WHERE id = ?", id)
      .stream()
      .findFirst()
      .flatMap(Result::getVertex)
      .map(linkMapper::mapToDomain)
      .or(Optional::empty);
  }
}
