package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.remote.RemoteMutableVertex;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class ContentMapper {

  public @NonNull Content mapToDomain(@NonNull Vertex vertex) {
    String id = vertex.getString("id");
    String linkId = vertex.getString("linkId");
    LocalDateTime downloadedAt = vertex.getLocalDateTime("downloadedAt");

    if (id == null) throw new IllegalStateException("Required field 'id' missing in vertex");
    if (linkId == null) throw new IllegalStateException("Required field 'linkId' missing in vertex");
    if (downloadedAt == null) throw new IllegalStateException("Required field 'downloadedAt' missing in vertex");

    String htmlContent = vertex.getString("htmlContent");
    String textContent = vertex.getString("textContent");
    Integer contentLength = vertex.getInteger("contentLength");
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

    @SuppressWarnings("unchecked")
    List<Float> embedding = vertex.has("embedding") ? (List<Float>) vertex.get("embedding") : null;

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
      publishedDate,
      embedding
    );
  }

  public @NonNull MutableVertex mapToVertex(@NonNull Content content, @NonNull RemoteMutableVertex vertex) {
    // Set required fields
    vertex.set("id", content.id());
    vertex.set("linkId", content.linkId());
    vertex.set("downloadedAt", content.downloadedAt());
    vertex.set("status", content.status().name());

    // Set nullable base fields - only set if not null to avoid ArcadeDB serialization issues
    if (content.htmlContent() != null) vertex.set("htmlContent", content.htmlContent());
    if (content.textContent() != null) vertex.set("textContent", content.textContent());
    if (content.contentLength() != null) vertex.set("contentLength", content.contentLength());
    if (content.mimeType() != null) vertex.set("mimeType", content.mimeType());

    // Set nullable Phase 1 Feature 1 fields
    if (content.summary() != null) vertex.set("summary", content.summary());
    if (content.heroImageUrl() != null) vertex.set("heroImageUrl", content.heroImageUrl());
    if (content.extractedTitle() != null) vertex.set("extractedTitle", content.extractedTitle());
    if (content.extractedDescription() != null) vertex.set("extractedDescription", content.extractedDescription());
    if (content.author() != null) vertex.set("author", content.author());
    if (content.publishedDate() != null) vertex.set("publishedDate", content.publishedDate());
    if (content.embedding() != null) vertex.set("embedding", content.embedding());

    return vertex;
  }
}
