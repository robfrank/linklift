package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.remote.RemoteMutableVertex;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.ReadStatus;
import java.util.List;

public class LinkMapper {

  Link mapToDomain(Vertex vertex) {
    String readStatusStr = vertex.getString("readStatus");
    ReadStatus readStatus = readStatusStr != null ? ReadStatus.valueOf(readStatusStr) : ReadStatus.UNREAD;
    Boolean archived = vertex.getBoolean("archived");
    Boolean favorited = vertex.getBoolean("favorited");
    return new Link(
      vertex.getString("id"),
      vertex.getString("url"),
      vertex.getString("title"),
      vertex.getString("description"),
      vertex.getLocalDateTime("extractedAt"),
      vertex.getString("contentType"),
      (List<String>) (Object) vertex.getList("extractedUrls"),
      readStatus,
      archived != null && archived,
      favorited != null && favorited
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
    vertex.set("readStatus", link.readStatus().name());
    vertex.set("archived", link.archived());
    vertex.set("favorited", link.favorited());
    return vertex;
  }
}
