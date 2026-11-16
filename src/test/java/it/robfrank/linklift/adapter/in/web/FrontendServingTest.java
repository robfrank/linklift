package it.robfrank.linklift.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.javalin.Javalin;
import io.javalin.testtools.Response;
import it.robfrank.linklift.application.domain.service.AuthorizationService;
import it.robfrank.linklift.config.FrontendBundle;
import it.robfrank.linklift.config.WebBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrontendServingTest {

  private static final String MARKER = "INDEX-MARKER-7f3a";

  @TempDir
  Path dir;

  private Optional<FrontendBundle> bundle;

  @BeforeEach
  void createBundle() throws IOException {
    Files.writeString(dir.resolve("index.html"), "<html>" + MARKER + "</html>");
    Files.createDirectories(dir.resolve("assets"));
    Files.writeString(dir.resolve("assets/app.js"), "console.log('app');");
    Files.createDirectories(dir.resolve("api"));
    Files.writeString(dir.resolve("api/x.txt"), "API-FILE");
    bundle = FrontendBundle.fromEnvValue(dir.toString());
    assertThat(bundle).isPresent();
  }

  private Javalin withBundle() {
    return new WebBuilder().withFrontend(bundle).build();
  }

  private Javalin withoutBundle() {
    return new WebBuilder().withFrontend(Optional.empty()).build();
  }

  private String index() throws IOException {
    return Files.readString(dir.resolve("index.html"));
  }

  @Test
  void root_withBundle_servesIndexHtml() {
    LoopbackJavalin.test(withBundle(), (app, client) -> {
      Response response = client.get("/");
      assertThat(response.code()).isEqualTo(200);
      assertThat(String.join(",", response.headers().get("Content-Type"))).startsWith("text/html");
      assertThat(response.body().string()).isEqualTo(index());
    });
  }

  @Test
  void root_withoutBundle_answersLinkLift() {
    LoopbackJavalin.test(withoutBundle(), (app, client) -> {
      Response response = client.get("/");
      assertThat(response.code()).isEqualTo(200);
      assertThat(response.body().string()).isEqualTo("LinkLift");
    });
  }

  @Test
  void appRoute_withBundle_fallsBackToIndexHtml() {
    LoopbackJavalin.test(withBundle(), (app, client) -> {
      Response response = client.get("/collections/42");
      assertThat(response.code()).isEqualTo(200);
      assertThat(String.join(",", response.headers().get("Content-Type"))).startsWith("text/html");
      assertThat(response.body().string()).isEqualTo(index());
    });
  }

  @Test
  void asset_withBundle_isServed() {
    LoopbackJavalin.test(withBundle(), (app, client) -> {
      Response response = client.get("/assets/app.js");
      assertThat(response.code()).isEqualTo(200);
      assertThat(response.body().string()).isEqualTo("console.log('app');");
    });
  }

  @Test
  void unmatchedApiPaths_answerLikeWithoutBundle() {
    for (String path : new String[] { "/api", "/api/v1/nope", "/api/x.txt" }) {
      String[] plain = new String[2];
      LoopbackJavalin.test(withoutBundle(), (app, client) -> {
        Response response = client.get(path, req -> req.header("Accept", "text/html"));
        plain[0] = String.valueOf(response.code());
        plain[1] = response.body().string();
      });
      LoopbackJavalin.test(withBundle(), (app, client) -> {
        Response response = client.get(path, req -> req.header("Accept", "text/html"));
        String body = response.body().string();
        assertThat(response.code()).as(path).isEqualTo(404);
        assertThat(String.valueOf(response.code())).isEqualTo(plain[0]);
        assertThat(body).isEqualTo(plain[1]).doesNotContain(MARKER);
      });
      assertThat(plain[0]).isEqualTo("404");
      assertThat(plain[1]).doesNotContain(MARKER);
    }
  }

  @Test
  void nonGet_onAppRoute_answers404() {
    LoopbackJavalin.test(withBundle(), (app, client) -> {
      Response response = client.post("/collections/42", "{}");
      assertThat(response.code()).isEqualTo(404);
      assertThat(response.body().string()).doesNotContain(MARKER);
    });
  }

  @Test
  void appRoute_withoutBundle_answers404() {
    LoopbackJavalin.test(withoutBundle(), (app, client) -> assertThat(client.get("/collections/42").code()).isEqualTo(404));
  }

  @Test
  void bundleFiles_needNoCredentials() {
    Javalin app = new WebBuilder().withAuthorizationService(mock(AuthorizationService.class)).withFrontend(bundle).build();
    LoopbackJavalin.test(app, (server, client) -> {
      Response route = client.get("/collections/42");
      assertThat(route.code()).isEqualTo(200);
      assertThat(route.body().string()).isEqualTo(index());
      Response asset = client.get("/assets/app.js");
      assertThat(asset.code()).isEqualTo(200);
      assertThat(asset.body().string()).isEqualTo("console.log('app');");
    });
  }

  @Test
  void upAndMetrics_areNotShadowedByBundle() {
    LoopbackJavalin.test(withBundle(), (app, client) -> {
      Response up = client.get("/up");
      assertThat(up.code()).isEqualTo(200);
      assertThat(up.body().string()).isEmpty();
      Response metrics = client.get("/metrics");
      assertThat(metrics.code()).isEqualTo(200);
      assertThat(metrics.body().string()).contains("jvm_memory_used_bytes");
    });
  }
}
