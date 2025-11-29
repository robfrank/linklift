package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

/**
 * Command to add a link to a collection.
 */
public record AddLinkToCollectionCommand(@NonNull String collectionId, @NonNull String linkId, @NonNull String userId) {}
