package it.robfrank.linklift.adapter.out.persitence;

import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.remote.RemoteMutableVertex;
import it.robfrank.linklift.application.domain.model.Link;
import java.util.UUID;

public class LinkMapper {

  Link mapToDomain(Vertex vertex) {
    return new Link(
      vertex.getString("id"),
      vertex.getString("url"),
      vertex.getString("title"),
      vertex.getString("description"),
      vertex.getLocalDateTime("extractedAt"),
      vertex.getString("contentType")
    );
  }

  MutableVertex mapToVertex(Link link, RemoteMutableVertex vertex) {
    vertex.set("id", link.id());
    vertex.set("url", link.url());
    vertex.set("title", link.title());
    vertex.set("description", link.description());
    vertex.set("extractedAt", link.extractedAt());
    vertex.set("contentType", link.contentType());
    return vertex;
  }
}
