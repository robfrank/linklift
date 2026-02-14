package it.robfrank.linklift.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CollectionSummaryServiceTest {

  @Test
  void shouldDefineInterface() {
    // This is a structural test to ensure the interface is defined as expected
    assertThat(CollectionSummaryService.class).isInterface();
  }
}
