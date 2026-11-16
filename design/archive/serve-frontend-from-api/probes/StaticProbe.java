import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.staticfiles.Location;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/** Falsifiers for the Javalin premises of serve-frontend-from-api. Exits 1 when the claim is false. */
public class StaticProbe {

  static void check(boolean ok, String what) {
    if (!ok) {
      System.out.println("FALSE: " + what);
      System.exit(1);
    }
    System.out.println("ok: " + what);
  }

  public static void main(String[] args) throws Exception {
    String mode = args[0];
    // A temp dir under java.io.tmpdir (a symlinked /var/folders path on macOS), resolved to its real path.
    Path dir = Files.createTempDirectory("sffa").toRealPath();
    Files.writeString(dir.resolve("index.html"), "INDEX-MARKER");
    Files.writeString(dir.resolve("up"), "STATIC-UP");
    Files.createDirectories(dir.resolve("api"));
    Files.writeString(dir.resolve("api/x.txt"), "API-FILE");
    Files.createDirectories(dir.resolve("assets"));
    Files.writeString(dir.resolve("assets/app.js"), "APP-JS");

    Javalin app = Javalin.create(config -> {
      config.staticFiles.add(sf -> {
        sf.hostedPath = "/";
        sf.directory = dir.toString();
        sf.location = Location.EXTERNAL;
        if (mode.equals("skip")) {
          sf.skipFileFunction = req -> req.getRequestURI().equals("/api") || req.getRequestURI().startsWith("/api/");
        }
      });
      config.routes.get("/up", ctx -> ctx.result("ROUTE-UP"));
      if (mode.equals("error404")) {
        config.routes.error(404, ctx -> {
          ctx.status(200);
          ctx.contentType(ContentType.TEXT_HTML);
          ctx.result(Files.readAllBytes(dir.resolve("index.html")));
        });
      }
    });
    app.unsafe.jetty.host = "127.0.0.1";
    app.start(0);
    try {
      HttpClient client = HttpClient.newHttpClient();
      String base = "http://127.0.0.1:" + app.port();
      java.util.function.Function<String, HttpResponse<String>> get = path -> {
        try {
          return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      };
      switch (mode) {
        case "route-first" -> {
          HttpResponse<String> r = get.apply("/up");
          check(r.statusCode() == 200 && r.body().equals("ROUTE-UP"), "a matched route answers before a static file at the same path");
        }
        case "welcome" -> {
          HttpResponse<String> r = get.apply("/");
          check(r.statusCode() == 200 && r.body().equals("INDEX-MARKER"), "GET / serves the directory's index.html");
          HttpResponse<String> a = get.apply("/assets/app.js");
          check(a.statusCode() == 200 && a.body().equals("APP-JS"), "a nested file is served from the real-path directory");
        }
        case "error404" -> {
          HttpResponse<String> r = get.apply("/collections/42");
          check(
            r.statusCode() == 200 && r.body().equals("INDEX-MARKER") && r.headers().firstValue("Content-Type").orElse("").startsWith("text/html"),
            "a 404 error handler runs for an unmatched GET and replaces status, content type and body"
          );
        }
        case "skip" -> {
          HttpResponse<String> r = get.apply("/api/x.txt");
          check(r.statusCode() == 404 && !r.body().contains("API-FILE"), "skipFileFunction keeps a file under /api/ from being served");
          HttpResponse<String> a = get.apply("/assets/app.js");
          check(a.statusCode() == 200 && a.body().equals("APP-JS"), "skipFileFunction leaves other files served");
        }
        default -> check(false, "unknown mode " + mode);
      }
    } finally {
      app.stop();
    }
  }
}
