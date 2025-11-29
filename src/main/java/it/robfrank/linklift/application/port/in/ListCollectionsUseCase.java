package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Collection;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Use case for listing all collections for a user.
 */
public interface ListCollectionsUseCase {
  @NonNull
  List<Collection> listCollections(@NonNull String userId);
}
