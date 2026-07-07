package io.github.addxiaoyi.starx.velocity.http;

public interface RouteRegistrar {

  void get(String path, RouteHandler handler);
  void post(String path, RouteHandler handler);

  @FunctionalInterface
  interface RouteHandler {
    void handle(JsonHttpExchange ctx) throws Exception;
  }
}
