package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public record AddTagToLinkCommand(@NonNull String linkId, @NonNull String tagId, @NonNull String userId) {}
