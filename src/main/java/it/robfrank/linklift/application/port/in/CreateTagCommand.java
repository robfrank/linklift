package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public record CreateTagCommand(@NonNull String name, @NonNull String userId) {}
