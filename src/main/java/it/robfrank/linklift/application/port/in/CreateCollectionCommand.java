package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record CreateCollectionCommand(@NonNull String name, @Nullable String description, @NonNull String userId, @Nullable String query) {}
