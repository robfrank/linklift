package it.robfrank.linklift.application.port.in;

public record NewLinkCommand(String url, String title, String description, String userId) {
  /**
   * Creates a NewLinkCommand without user context (for backward compatibility).
   */
  public NewLinkCommand(String url, String title, String description) {
    this(url, title, description, null);
  }
}
