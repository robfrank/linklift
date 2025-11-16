package it.robfrank.linklift.application.domain.exception;

import org.jspecify.annotations.NonNull;

public class ContentNotFoundException extends LinkLiftException {

  public ContentNotFoundException(@NonNull String linkId) {
    super("Content not found for link: " + linkId, ErrorCode.CONTENT_NOT_FOUND);
  }
}
