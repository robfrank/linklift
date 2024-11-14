package it.robfrank.linklift.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class LinkController {
  private final Javalin app;

  public LinkController(Javalin app) {
    this.app = app;
    setupRoutes();
  }

  private void setupRoutes() {
    app.get("/api/v1/lift", this::processLink);
  }

  private void processLink(Context ctx) {
    String link = ctx.queryParam("link");
    if (link == null || link.isEmpty()) {
      ctx.status(400).result("Link è obbligatorio");
      return;
    }

    // TODO: Implementare la logica di processamento del link
    ctx.json(new LinkResponse(link, "Link ricevuto e in fase di elaborazione"));
  }

  // Classe interna per la risposta
  public static class LinkResponse {
    private String originalLink;
    private String status;

    public LinkResponse(String originalLink, String status) {
      this.originalLink = originalLink;
      this.status = status;
    }

    // Getter e setter
    public String getOriginalLink() { return originalLink; }
    public void setOriginalLink(String originalLink) { this.originalLink = originalLink; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
  }
}