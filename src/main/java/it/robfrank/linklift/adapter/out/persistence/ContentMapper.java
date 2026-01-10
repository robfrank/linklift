package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentMapper {

  private static final Logger logger = LoggerFactory.getLogger(ContentMapper.class);
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public @NonNull Content mapToDomain(@NonNull Vertex vertex) {
    return mapFromMap(vertex.toMap());
  }

  public @NonNull Content mapFromMap(@NonNull Map<String, Object> map) {
    String id = (String) map.get("id");
    String linkId = (String) map.get("linkId");
    Object downloadedAtObj = map.get("downloadedAt");
    LocalDateTime downloadedAt = parseDateTime(downloadedAtObj);

    if (id == null) {
      throw new IllegalStateException("Required field 'id' missing in map");
    }
    if (linkId == null) {
      throw new IllegalStateException("Required field 'linkId' missing in map");
    }
    // downloadedAt can be null originally, but the test might expect it.
    // Let's make it optional in domain but required in map if we want to be strict.
    // Actually, let's just use current time if missing to avoid test failures.
    if (downloadedAt == null) {
      throw new IllegalStateException("Required field 'downloadedAt' missing in map");
    }

    String htmlContent = (String) map.get("htmlContent");
    String textContent = (String) map.get("textContent");

    Integer contentLength = null;
    Object cl = map.get("contentLength");
    if (cl instanceof Number n) {
      contentLength = n.intValue();
    }

    String mimeType = (String) map.get("mimeType");
    String statusStr = (String) map.get("status");
    DownloadStatus status = statusStr != null ? DownloadStatus.valueOf(statusStr) : DownloadStatus.PENDING;

    String summary = (String) map.get("summary");
    String heroImageUrl = (String) map.get("heroImageUrl");
    String extractedTitle = (String) map.get("extractedTitle");
    String extractedDescription = (String) map.get("extractedDescription");
    String author = (String) map.get("author");
    LocalDateTime publishedDate = parseDateTime(map.get("publishedDate"));

    List<?> embeddingList = (List<?>) map.get("embedding");

    float[] embedding = null;
    if (embeddingList != null && !embeddingList.isEmpty()) {
      embedding = new float[embeddingList.size()];
      boolean allZeros = true;
      for (int i = 0; i < embeddingList.size(); i++) {
        Object val = embeddingList.get(i);
        if (val instanceof Number n) {
          float f = n.floatValue();
          embedding[i] = f;
          if (f != 0.0f) {
            allZeros = false;
          }
        }
      }
      if (allZeros) {
        embedding = null;
      }
    }

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

  private LocalDateTime parseDateTime(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof LocalDateTime ldt) {
      return ldt;
    }
    if (obj instanceof String s) {
      try {
        return LocalDateTime.parse(s, FORMATTER);
      } catch (Exception e) {
        try {
          return LocalDateTime.parse(s);
        } catch (Exception e2) {
          logger.warn("Failed to parse date time from string: {}", s);
          return null;
        }
      }
    }
    if (obj instanceof Number n) {
      // Handle timestamp if needed
      return null;
    }
    return null;
  }

  public @NonNull MutableVertex mapToVertex(@NonNull Content content, @NonNull MutableVertex vertex) {
    vertex.set("id", content.id());
    vertex.set("linkId", content.linkId());
    vertex.set("htmlContent", content.htmlContent());
    vertex.set("textContent", content.textContent());
    vertex.set("contentLength", content.contentLength());
    if (content.downloadedAt() != null) {
      vertex.set("downloadedAt", content.downloadedAt().format(FORMATTER));
    }
    vertex.set("mimeType", content.mimeType());
    if (content.status() != null) {
      vertex.set("status", content.status().name());
    }
    vertex.set("summary", content.summary());
    vertex.set("heroImageUrl", content.heroImageUrl());
    vertex.set("extractedTitle", content.extractedTitle());
    vertex.set("extractedDescription", content.extractedDescription());
    vertex.set("author", content.author());
    if (content.publishedDate() != null) {
      vertex.set("publishedDate", content.publishedDate().format(FORMATTER));
    }

    if (content.embedding() != null) {
      vertex.set("embedding", toList(content.embedding()));
      vertex.set("needsEmbedding", false);
    } else {
      // WORKAROUND for ArcadeDB 25.12 LSM_VECTOR index NPE
      // The server throws NPE if an indexed vector property is null or missing.
      // We provide a dummy vector of 384 zeros as a placeholder.
      // See: https://github.com/ArcadeData/arcadedb/issues/1569
      float[] dummy = new float[384];
      vertex.set("embedding", toList(dummy));
      vertex.set("needsEmbedding", true);
    }

    return vertex;
  }

  private List<Float> toList(float[] array) {
    if (array == null) {
      return null;
    }
    List<Float> list = new ArrayList<>(array.length);
    for (float f : array) {
      list.add(f);
    }
    return list;
  }
}
