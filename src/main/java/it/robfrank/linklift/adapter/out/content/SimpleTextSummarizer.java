package it.robfrank.linklift.adapter.out.content;

import it.robfrank.linklift.application.port.out.ContentSummarizerPort;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Simple extractive summarizer implementation.
 * Uses word frequency scoring to identify important sentences.
 */
public class SimpleTextSummarizer implements ContentSummarizerPort {

  @Override
  public @Nullable String generateSummary(@NonNull String textContent, int maxLength) {
    if (textContent.isBlank()) {
      return null;
    }

    // Split into sentences
    List<String> sentences = Arrays.asList(textContent.split("(?<=[.!?])\\s+"));
    if (sentences.isEmpty()) {
      return null;
    }

    // If text is short enough, return as is
    if (textContent.length() <= maxLength) {
      return textContent;
    }

    // Calculate word frequencies
    Map<String, Integer> wordFrequencies = new HashMap<>();
    Arrays.stream(textContent.toLowerCase().split("\\W+"))
      .filter(w -> w.length() > 3) // Filter short words
      .forEach(w -> wordFrequencies.merge(w, 1, Integer::sum));

    // Score sentences
    Map<String, Double> sentenceScores = new HashMap<>();
    for (String sentence : sentences) {
      double score = 0;
      String[] words = sentence.toLowerCase().split("\\W+");
      for (String word : words) {
        if (wordFrequencies.containsKey(word)) {
          score += wordFrequencies.get(word);
        }
      }
      // Normalize by sentence length to avoid favoring long sentences
      if (words.length > 0) {
        sentenceScores.put(sentence, score / words.length);
      }
    }

    // Select top sentences
    List<String> topSentences = sentenceScores
      .entrySet()
      .stream()
      .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
      .limit(5) // Take top 5 sentences
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());

    // Reorder sentences as they appear in the text
    StringBuilder summary = new StringBuilder();
    for (String sentence : sentences) {
      if (topSentences.contains(sentence)) {
        if (summary.length() + sentence.length() + 1 > maxLength) {
          break;
        }
        if (summary.length() > 0) {
          summary.append(" ");
        }
        summary.append(sentence);
      }
    }

    String result = summary.toString();
    return result.isEmpty() ? sentences.getFirst() : result;
  }
}
