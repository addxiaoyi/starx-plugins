package io.github.addxiaoyi.starx.velocity.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JsonHttpExchange {

  private static final Gson GSON = new Gson();

  private final HttpExchange exchange;
  private byte[] rawBody;
  private int responseStatus = 200;

  public JsonHttpExchange(HttpExchange exchange) {
    this.exchange = exchange;
  }

  public JsonHttpExchange status(int code) {
    this.responseStatus = code;
    return this;
  }

  public void json(Object data) throws IOException {
    String json = GSON.toJson(data);
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(responseStatus, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.getResponseBody().close();
  }

  public <T> T bodyAsClass(Class<T> clazz) {
    return GSON.fromJson(bodyString(), clazz);
  }

  public String bodyString() {
    if (rawBody == null) {
      try (InputStream is = exchange.getRequestBody()) {
        rawBody = is.readAllBytes();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return new String(rawBody, StandardCharsets.UTF_8);
  }

  public String queryParam(String name) {
    String query = exchange.getRequestURI().getRawQuery();
    if (query == null) return null;
    for (String param : query.split("&")) {
      String[] parts = param.split("=", 2);
      if (parts.length == 2 && parts[0].equals(name)) {
        return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
      }
    }
    return null;
  }

  public String header(String name) {
    return exchange.getRequestHeaders().getFirst(name);
  }

  public void result(String text) throws IOException {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(responseStatus, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.getResponseBody().close();
  }

  public void sendError(int code, String message) throws IOException {
    status(code);
    json(Map.of("error", message));
  }
}
