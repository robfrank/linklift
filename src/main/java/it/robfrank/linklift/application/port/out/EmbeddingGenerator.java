package it.robfrank.linklift.application.port.out;

import java.util.List;
import org.jspecify.annotations.NonNull;

public interface EmbeddingGenerator {
  @NonNull
  List<Float> generateEmbedding(@NonNull String text);
}
