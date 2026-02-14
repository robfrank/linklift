package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public record MergeCollectionsCommand(@NonNull String sourceCollectionId, @NonNull String targetCollectionId, @NonNull String userId) {}
