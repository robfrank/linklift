package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.remote.RemoteMutableVertex;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;

public class ContentMapper {

  public @NonNull Content mapToDomain(@NonNull Vertex vertex) {
    String id = vertex.getString("id");
    String linkId = vertex.getString("linkId");
    String htmlContent = vertex.getString("htmlContent");
    String textContent = vertex.getString("textContent");
    Integer contentLength = vertex.getInteger("contentLength");
    LocalDateTime downloadedAt = vertex.getLocalDateTime("downloadedAt");
    String mimeType = vertex.getString("mimeType");
    String statusStr = vertex.getString("status");
    DownloadStatus status = statusStr != null ? DownloadStatus.valueOf(statusStr) : DownloadStatus.PENDING;

    // New fields for Phase 1 Feature 1
    String summary = vertex.has("summary") ? vertex.getString("summary") : null;
    String heroImageUrl = vertex.has("heroImageUrl") ? vertex.getString("heroImageUrl") : null;
    String extractedTitle = vertex.has("extractedTitle") ? vertex.getString("extractedTitle") : null;
    String extractedDescription = vertex.has("extractedDescription") ? vertex.getString("extractedDescription") : null;
    String author = vertex.has("author") ? vertex.getString("author") : null;
    LocalDateTime publishedDate = vertex.has("publishedDate") ? vertex.getLocalDateTime("publishedDate") : null;

    return new Content(
      id,
      linkId,
      htmlContent,
      textContent,
      contentLength,
      downloadedAt,
      mimeType,
      status,
      summary,
      heroImageUrl,
      extractedTitle,
      extractedDescription,
      author,
      publishedDate
    );
  }

  public @NonNull MutableVertex mapToVertex(@NonNull Content content, @NonNull RemoteMutableVertex vertex) {
    vertex.set("id", content.id());
    vertex.set("linkId", content.linkId());
    vertex.set("htmlContent", content.htmlContent());
    vertex.set("textContent", content.textContent());
    vertex.set("contentLength", content.contentLength());
    vertex.set("downloadedAt", content.downloadedAt());
    vertex.set("mimeType", content.mimeType());
    vertex.set("status", content.status().name());

    // New fields for Phase 1 Feature 1
    if (content.summary() != null) vertex.set("summary", content.summary());
    if (content.heroImageUrl() != null) vertex.set("heroImageUrl", content.heroImageUrl());
    if (content.extractedTitle() != null) vertex.set("extractedTitle", content.extractedTitle());
    if (content.extractedDescription() != null) vertex.set("extractedDescription", content.extractedDescription());
    if (content.author() != null) vertex.set("author", content.author());
    if (content.publishedDate() != null) vertex.set("publishedDate", content.publishedDate());

    return vertex;
  }
}
