package it.robfrank.linklift.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.javalin.Javalin;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

class LoopbackJavalinTest {

  @Test
  void should_bind_app_to_loopback_and_serve_requests() {
    Javalin app = Javalin.create(cfg -> cfg.routes.get("/ping", ctx -> ctx.result("pong")));

    LoopbackJavalin.test(app, (server, client) -> {
      Connector[] connectors = server.jettyServer().server().getConnectors();
      assertThat(connectors).hasSize(1);
      assertThat(((ServerConnector) connectors[0]).getHost()).isEqualTo("127.0.0.1");

      var response = client.get("/ping");
      assertThat(response.code()).isEqualTo(200);
      assertThat(response.body().string()).isEqualTo("pong");
    });
  }

  @Test
  void should_not_let_stray_listener_on_same_port_take_connections() {
    Javalin app = Javalin.create(cfg -> cfg.routes.get("/ping", ctx -> ctx.result("pong")));
    AtomicReference<ServerSocket> stray = new AtomicReference<>();

    LoopbackJavalin.test(app, (server, client) -> {
      try {
        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), server.port()));
        stray.set(socket);
        Thread.ofVirtual().start(() -> answer404(socket));
      } catch (BindException expected) {} catch (IOException e) {
        throw new IllegalStateException(e);
      }

      var response = client.get("/ping");
      assertThat(response.code()).isEqualTo(200);
      assertThat(response.body().string()).isEqualTo("pong");
    });

    if (stray.get() != null) {
      try {
        stray.get().close();
      } catch (IOException ignored) {
        // closing a test socket
      }
    }
  }

  private static void answer404(ServerSocket socket) {
    try {
      while (!socket.isClosed()) {
        try (var connection = socket.accept()) {
          connection.getOutputStream().write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
        }
      }
    } catch (IOException ignored) {
      // socket closed
    }
  }
}
