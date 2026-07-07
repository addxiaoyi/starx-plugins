package io.github.addxiaoyi.starx.velocity.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public final class TestHttpServer implements RouteRegistrar {

  private final HttpServer server;

  public TestHttpServer(int port) throws IOException {
    this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    server.setExecutor(null);
  }

  public int port() {
    return server.getAddress().getPort();
  }

  @Override
  public void get(String path, RouteHandler handler) {
    server.createContext(path, exchange -> handleMethod(exchange, "GET", handler));
  }

  @Override
  public void post(String path, RouteHandler handler) {
    server.createContext(path, exchange -> handleMethod(exchange, "POST", handler));
  }

  public void start() {
    server.start();
  }

  public void stop() {
    server.stop(0);
  }

  private static void handleMethod(
      HttpExchange exchange, String method, RouteHandler handler)
      throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
      exchange.sendResponseHeaders(405, -1);
      return;
    }
    try {
      handler.handle(new JsonHttpExchange(exchange));
    } catch (Exception e) {
      throw new RuntimeException("Handler error for " + method + " " + exchange.getRequestURI(), e);
    }
  }
}
