package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.ReadStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record UpdateLinkStatusCommand(
  @NonNull String id,
  @Nullable ReadStatus readStatus,
  @Nullable Boolean archived,
  @Nullable Boolean favorited,
  @NonNull String userId
) {}
