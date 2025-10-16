package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public record DownloadContentCommand(@NonNull String linkId, @NonNull String url) {}
