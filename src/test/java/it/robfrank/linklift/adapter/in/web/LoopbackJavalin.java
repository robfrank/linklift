package it.robfrank.linklift.adapter.in.web;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import io.javalin.testtools.TestCase;

/**
 * Starts a Javalin test app bound to the loopback address only, so another listener on the same port
 * (on another interface) cannot take the test client's connections.
 */
public final class LoopbackJavalin {

  private static final String LOOPBACK = "127.0.0.1";

  private LoopbackJavalin() {}

  public static void test(Javalin app, TestCase testCase) {
    app.unsafe.jetty.host = LOOPBACK;
    JavalinTest.test(app, testCase);
  }
}
