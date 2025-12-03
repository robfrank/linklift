package it.robfrank.linklift.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.config.WebBuilder;
import org.junit.jupiter.api.Test;

class ObservabilityTest {

  @Test
  void metricsEndpoint_shouldReturnPrometheusMetrics() {
    JavalinTest.test(new WebBuilder().build(), (app, client) -> {
      var response = client.get("/metrics");
      assertThat(response.code()).isEqualTo(200);
      assertThat(response.body().string()).contains("jvm_memory_used_bytes");
    });
  }
}
