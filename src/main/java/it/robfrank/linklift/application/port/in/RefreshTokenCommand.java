package it.robfrank.linklift.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Command for refreshing authentication tokens.
 */
public record RefreshTokenCommand(
  @JsonProperty("refreshToken") String refreshToken,
  @JsonProperty("ipAddress") String ipAddress,
  @JsonProperty("userAgent") String userAgent
) {
  public RefreshTokenCommand {
    if (refreshToken == null || refreshToken.trim().isEmpty()) {
      throw new IllegalArgumentException("Refresh token is required");
    }
  }
}
