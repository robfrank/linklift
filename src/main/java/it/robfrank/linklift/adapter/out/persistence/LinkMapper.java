package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.remote.RemoteMutableVertex;
import it.robfrank.linklift.application.domain.model.Link;

public class LinkMapper {

  Link mapToDomain(Vertex vertex) {
    return new Link(
      vertex.getString("id"),
      vertex.getString("url"),
      vertex.getString("title"),
      vertex.getString("description"),
      vertex.getLocalDateTime("extractedAt"),
      vertex.getString("contentType"),
      (java.util.List<String>) (Object) vertex.getList("extractedUrls")
    );
  }

  MutableVertex mapToVertex(Link link, RemoteMutableVertex vertex) {
    vertex.set("id", link.id());
    vertex.set("url", link.url());
    vertex.set("title", link.title());
    vertex.set("description", link.description());
    vertex.set("extractedAt", link.extractedAt());
    vertex.set("contentType", link.contentType());
    vertex.set("extractedUrls", link.extractedUrls());
    return vertex;
  }
}
