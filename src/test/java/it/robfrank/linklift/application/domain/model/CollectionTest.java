package it.robfrank.linklift.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CollectionTest {

  @Test
  void shouldCreateCollection() {
    Collection collection = new Collection("id", "name", "description", "userId", null, "AI summary");

    assertThat(collection.id()).isEqualTo("id");
    assertThat(collection.name()).isEqualTo("name");
    assertThat(collection.description()).isEqualTo("description");
    assertThat(collection.userId()).isEqualTo("userId");
    assertThat(collection.summary()).isEqualTo("AI summary");
  }

  @Test
  void shouldIdentifySmartCollection() {
    Collection smart = new Collection("id", "name", "description", "userId", "query", null);
    Collection normal = new Collection("id", "name", "description", "userId", null, null);

    assertThat(smart.isSmart()).isTrue();
    assertThat(normal.isSmart()).isFalse();
  }
}
