package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Represents a collection with its associated links.
 * This is used when retrieving collection details.
 */
public record CollectionWithLinks(@JsonProperty("collection") @NonNull Collection collection, @JsonProperty("links") @NonNull List<Link> links) {}
