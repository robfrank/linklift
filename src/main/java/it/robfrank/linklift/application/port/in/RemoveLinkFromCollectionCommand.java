package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

/**
 * Command to remove a link from a collection.
 */
public record RemoveLinkFromCollectionCommand(@NonNull String collectionId, @NonNull String linkId, @NonNull String userId) {}
