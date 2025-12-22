package it.robfrank.linklift.adapter.out.http;

import it.robfrank.linklift.application.port.out.ContentExtractorPort;
import java.net.URL;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jsoup-based implementation of ContentExtractorPort.
 */
public class JsoupContentExtractor implements ContentExtractorPort {

  private static final Logger logger = LoggerFactory.getLogger(JsoupContentExtractor.class);
  private static final int MAX_EXTRACTED_URLS = 200;

  @Override
  public ExtractedMetadata extractMetadata(@NonNull String html, @NonNull String url) {
    try {
      Document doc = Jsoup.parse(html, url);

      String title = extractTitle(doc);
      String description = extractDescription(doc);
      String author = extractAuthor(doc);
      String publishedDate = extractPublishedDate(doc);
      String heroImageUrl = extractHeroImage(doc, url);
      String mainContent = extractMainContent(doc);
      String textContent = extractTextContent(doc);
      List<String> extractedUrls = extractUrls(doc, url);

      return new ExtractedMetadata(title, description, author, publishedDate, heroImageUrl, mainContent, textContent, extractedUrls);
    } catch (Exception e) {
      logger.error("Failed to extract metadata from HTML", e);
      return new ExtractedMetadata(null, null, null, null, null, null, null, null);
    }
  }

  private @Nullable String extractTitle(@NonNull Document doc) {
    String ogTitle = doc.select("meta[property=og:title]").attr("content");
    if (!ogTitle.isEmpty()) return ogTitle;

    String twitterTitle = doc.select("meta[name=twitter:title]").attr("content");
    if (!twitterTitle.isEmpty()) return twitterTitle;

    String articleTitle = doc.select("meta[property=article:title]").attr("content");
    if (!articleTitle.isEmpty()) return articleTitle;

    String htmlTitle = doc.title();
    return htmlTitle.isEmpty() ? null : htmlTitle;
  }

  private @Nullable String extractDescription(@NonNull Document doc) {
    String ogDescription = doc.select("meta[property=og:description]").attr("content");
    if (!ogDescription.isEmpty()) return ogDescription;

    String twitterDescription = doc.select("meta[name=twitter:description]").attr("content");
    if (!twitterDescription.isEmpty()) return twitterDescription;

    String metaDescription = doc.select("meta[name=description]").attr("content");
    return metaDescription.isEmpty() ? null : metaDescription;
  }

  private @Nullable String extractAuthor(@NonNull Document doc) {
    String articleAuthor = doc.select("meta[property=article:author]").attr("content");
    if (!articleAuthor.isEmpty()) return articleAuthor;

    String metaAuthor = doc.select("meta[name=author]").attr("content");
    if (!metaAuthor.isEmpty()) return metaAuthor;

    String schemaAuthor = doc.select("[itemprop=author]").attr("content");
    if (!schemaAuthor.isEmpty()) return schemaAuthor;

    String relAuthor = doc.select("a[rel=author]").text();
    return relAuthor.isEmpty() ? null : relAuthor;
  }

  private @Nullable String extractPublishedDate(@NonNull Document doc) {
    String articlePublished = doc.select("meta[property=article:published_time]").attr("content");
    if (!articlePublished.isEmpty()) return articlePublished;

    String schemaPublished = doc.select("[itemprop=datePublished]").attr("content");
    if (!schemaPublished.isEmpty()) return schemaPublished;

    String timePublished = doc.select("time[datetime]").attr("datetime");
    return timePublished.isEmpty() ? null : timePublished;
  }

  private @Nullable String extractHeroImage(@NonNull Document doc, @NonNull String baseUrl) {
    String ogImage = doc.select("meta[property=og:image]").attr("content");
    if (!ogImage.isEmpty()) return resolveUrl(ogImage, baseUrl);

    String twitterImage = doc.select("meta[name=twitter:image]").attr("content");
    if (!twitterImage.isEmpty()) return resolveUrl(twitterImage, baseUrl);

    Element firstImg = doc.selectFirst("article img, main img, img[src]");
    if (firstImg != null) {
      String src = firstImg.attr("src");
      return resolveUrl(src, baseUrl);
    }

    return null;
  }

  private @Nullable String extractMainContent(@NonNull Document doc) {
    List<String> selectors = List.of("article", "[role=main]", ".article-content", ".post-content", "main", ".content");

    for (String selector : selectors) {
      Elements elements = doc.select(selector);
      if (!elements.isEmpty()) {
        Element content = elements.first();
        if (content != null) {
          content.select("script, style, nav, header, footer, aside, .ad, .advertisement").remove();
          return content.html();
        }
      }
    }

    Element body = doc.body();
    if (body != null) {
      Element bodyClone = body.clone();
      bodyClone.select("script, style, nav, header, footer, aside, .ad, .advertisement").remove();
      return bodyClone.html();
    }

    return null;
  }

  private @Nullable String extractTextContent(@NonNull Document doc) {
    String mainContent = extractMainContent(doc);
    if (mainContent != null) {
      return Jsoup.parse(mainContent).text();
    }
    return doc.body() != null ? doc.body().text() : null;
  }

  private String resolveUrl(@NonNull String url, @NonNull String baseUrl) {
    if (url.startsWith("http://") || url.startsWith("https://")) return url;
    try {
      return new URL(new URL(baseUrl), url).toString();
    } catch (Exception e) {
      return url;
    }
  }

  private @NonNull List<String> extractUrls(@NonNull Document doc, @NonNull String baseUrl) {
    Elements links = doc.select("a[href]");
    List<String> urls = links
      .stream()
      .map(link -> link.attr("abs:href"))
      .filter(href -> !href.isEmpty())
      .filter(href -> href.startsWith("http"))
      .distinct()
      .limit(MAX_EXTRACTED_URLS)
      .toList();

    if (links.size() > MAX_EXTRACTED_URLS) {
      logger.warn("Extracted URLs limited to {} from {} total links found in {}", MAX_EXTRACTED_URLS, links.size(), baseUrl);
    }

    return urls;
  }
}
