package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public record GetContentQuery(@NonNull String linkId) {}
