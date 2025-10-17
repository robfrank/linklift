package it.robfrank.linklift.application.domain.exception;

import org.jspecify.annotations.NonNull;

public class ContentDownloadException extends LinkLiftException {

    public ContentDownloadException(@NonNull String message) {
        super(message, ErrorCode.CONTENT_DOWNLOAD_FAILED);
    }

    public ContentDownloadException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause, ErrorCode.CONTENT_DOWNLOAD_FAILED);
    }
}
