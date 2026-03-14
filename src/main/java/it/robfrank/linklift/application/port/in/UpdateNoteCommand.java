package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public record UpdateNoteCommand(@NonNull String noteId, @NonNull String userId, @NonNull String content) {}
