package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public record CreateNoteCommand(@NonNull String linkId, @NonNull String userId, @NonNull String content) {}
