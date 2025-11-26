package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.event.LinkCreatedEvent;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.out.SaveLinkPort;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkContentExtractorService {

  private static final Logger log = LoggerFactory.getLogger(LinkContentExtractorService.class);
  private final ExecutorService executorService;
  private final SaveLinkPort saveLinkPort;

  public LinkContentExtractorService(ExecutorService executorService, SaveLinkPort saveLinkPort) {
    this.executorService = executorService;
    this.saveLinkPort = saveLinkPort;
  }

  public void handle(LinkCreatedEvent event) {
    log.atInfo().addArgument(() -> event.getLink().url()).log("Received LinkCreatedEvent for link: {}");
    executorService.submit(() -> {
      try {
        Link originalLink = event.getLink();
        Document document = Jsoup.connect(originalLink.url()).timeout(30000).get();

        String extractedText = extractMainContent(document);
        String imageUrl = extractImageUrl(document);
        String summary = generateSummary(extractedText); // Placeholder for actual summary generation

        Link updatedLink = new Link(
          originalLink.id(),
          originalLink.url(),
          originalLink.title(),
          originalLink.description(),
          originalLink.extractedAt(),
          originalLink.contentType()
        );
        saveLinkPort.save(updatedLink, event.getUserId());
        log.atInfo().addArgument(() -> originalLink.url()).log("Successfully extracted content and updated link for: {}");
      } catch (IOException e) {
        log.error("Error extracting content for link: {}", event.getLink().url(), e);
      }
    });
  }

  private String extractMainContent(Document document) {
    // This is a simplified extraction. A more robust solution might use a library like boilerpipe or custom logic.
    // For now, let's try to get content from common article tags.
    Elements article = document.select("article, .article, .post-content, .entry-content");
    if (!article.isEmpty()) {
      return article.first().text();
    }
    return document.body().text(); // Fallback to full body text
  }

  private String extractImageUrl(Document document) {
    // Try to find opengraph image
    Elements ogImage = document.select("meta[property=og:image]");
    if (!ogImage.isEmpty()) {
      return ogImage.attr("content");
    }

    // Try to find a prominent image in the article
    Elements articleImages = document.select("article img, .article img, .post-content img, .entry-content img");
    if (!articleImages.isEmpty()) {
      Optional<String> src = articleImages.stream().map(img -> img.attr("src")).filter(s -> !s.isEmpty()).findFirst();
      if (src.isPresent()) {
        String relativeUrl = src.get();
        try {
          URL docUrl = new URL(document.baseUri());
          return new URL(docUrl, relativeUrl).toString();
        } catch (MalformedURLException e) {
          log.error("Invalid image URL or base URI: {}", relativeUrl, e);
        }
      }
    }
    return null;
  }

  private String generateSummary(String text) {
    // This is a placeholder. Real summary generation would involve NLP.
    if (text == null || text.isEmpty()) {
      return null;
    }
    int summaryLength = Math.min(text.length(), 200);
    return text.substring(0, summaryLength) + (text.length() > 200 ? "..." : "");
  }
}
