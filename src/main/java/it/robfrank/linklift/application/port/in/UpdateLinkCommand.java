package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record UpdateLinkCommand(@NonNull String id, @Nullable String title, @Nullable String description, @NonNull String userId) {}
