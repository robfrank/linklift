package it.robfrank.linklift.repository;
import com.arcadedb.database.Database;
import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import it.robfrank.linklift.config.DatabaseConfig;
import it.robfrank.linklift.model.Link;

import java.util.Optional;

public class LinkRepository {
  private final Database database;

  public LinkRepository() {
    this.database = DatabaseConfig.getDatabase();
  }

  public Link saveLink(Link link) {
    database.begin();

    // Verifica se il link esiste già
    Optional<Vertex> existingLink = findLinkByUrl(link.url());

    if (existingLink.isPresent()) {
      database.rollback();
      return existingLink.get();
    }

    // Crea un nuovo vertice Link
    MutableVertex vertex = database.newVertex("Link");
    vertex.set("url", link.url());
    vertex.set("title", link.title());
    vertex.set("description", link.description());
    vertex.set("extractedAt", link.extractedAt());
    vertex.set("contentType", link.contentType());

    vertex.save();
    database.commit();

    return link;
  }

  public Optional<L> findLinkByUrl(String url) {
    try {
      var result = database.query(
          "SELECT FROM Link WHERE url = ?", url
      );

      return result.hasNext()
          ? result.next().getVertex()
          : Optional.empty();
    } catch (Exception e) {
      // Log dell'errore
      return Optional.empty();
    }
  }