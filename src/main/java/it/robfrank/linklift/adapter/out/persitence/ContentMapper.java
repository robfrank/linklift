package it.robfrank.linklift.adapter.out.persitence;

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

        return new Content(id, linkId, htmlContent, textContent, contentLength, downloadedAt, mimeType, status);
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
        return vertex;
    }
}
