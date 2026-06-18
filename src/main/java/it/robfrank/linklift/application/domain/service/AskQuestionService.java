package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.AnswerSource;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.QuestionAnswer;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.AskQuestionCommand;
import it.robfrank.linklift.application.port.in.AskQuestionUseCase;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.QuestionAnswerPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AskQuestionService implements AskQuestionUseCase {

  private static final Logger logger = LoggerFactory.getLogger(AskQuestionService.class);
  private static final int TOP_K = 5;
  private static final int EXCERPT_LENGTH = 300;
  private static final int MAX_QUESTION_LENGTH = 2000;

  private final EmbeddingGenerator embeddingGenerator;
  private final LoadContentPort loadContentPort;
  private final LoadLinksPort loadLinksPort;
  private final QuestionAnswerPort questionAnswerPort;

  public AskQuestionService(
    EmbeddingGenerator embeddingGenerator,
    LoadContentPort loadContentPort,
    LoadLinksPort loadLinksPort,
    QuestionAnswerPort questionAnswerPort
  ) {
    this.embeddingGenerator = embeddingGenerator;
    this.loadContentPort = loadContentPort;
    this.loadLinksPort = loadLinksPort;
    this.questionAnswerPort = questionAnswerPort;
  }

  @Override
  @NonNull
  public QuestionAnswer ask(@NonNull AskQuestionCommand command) {
    ValidationUtils.requireNotEmpty(command.question(), "question");
    ValidationUtils.requireMaxLength(command.question(), MAX_QUESTION_LENGTH, "question");

    List<Float> questionVector = embeddingGenerator.generateEmbedding(command.question());
    List<Content> similarContent = loadContentPort.findSimilar(questionVector, TOP_K, command.userId());

    if (similarContent.isEmpty()) {
      return new QuestionAnswer(command.question(), "I could not find any relevant content in your saved links to answer this question.", List.of());
    }

    StringBuilder contextBuilder = new StringBuilder();
    List<AnswerSource> sources = new ArrayList<>();

    // Batch-load the links for all similar content in a single query instead of one per item.
    List<String> linkIds = similarContent.stream().map(Content::linkId).distinct().toList();
    Map<String, Link> linksById = loadLinksPort.findLinksByIds(linkIds).stream().collect(Collectors.toMap(Link::id, Function.identity(), (a, b) -> a));

    for (Content content : similarContent) {
      Link link = linksById.get(content.linkId());
      if (link == null) {
        logger.warn("Could not load link for content {}", content.linkId());
        continue;
      }

      String text = content.textContent() != null ? content.textContent() : "";
      String excerpt = text.length() > EXCERPT_LENGTH ? text.substring(0, EXCERPT_LENGTH) + "..." : text;

      String title = link.title() != null ? link.title() : link.url();
      contextBuilder.append("Source: ").append(title).append("\n");
      contextBuilder.append("URL: ").append(link.url()).append("\n");
      if (!text.isEmpty()) {
        contextBuilder.append("Content: ").append(excerpt).append("\n\n");
      }

      sources.add(new AnswerSource(link.id(), title, link.url(), excerpt.isEmpty() ? null : excerpt));
    }

    String answer = questionAnswerPort.generateAnswer(command.question(), contextBuilder.toString());
    return new QuestionAnswer(command.question(), answer, sources);
  }
}
