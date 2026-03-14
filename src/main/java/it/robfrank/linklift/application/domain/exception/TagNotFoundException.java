package it.robfrank.linklift.application.domain.exception;

public class TagNotFoundException extends LinkLiftException {

  public TagNotFoundException(String tagId) {
    super("Tag not found with id: " + tagId, ErrorCode.TAG_NOT_FOUND);
  }
}
