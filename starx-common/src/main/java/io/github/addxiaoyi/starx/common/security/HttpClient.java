package io.github.addxiaoyi.starx.common.security;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 轻量级 HTTP 请求工具，基于 java.net.http.HttpClient。 */
public final class HttpClient {

  private static final Logger logger = Logger.getLogger(HttpClient.class.getName());
  private static final Gson gson = new Gson();
  private static final java.net.http.HttpClient sharedClient =
      java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private final java.net.http.HttpClient client;
  private final String url;
  private final String method;
  private String bearerToken;
  private HttpRequest.BodyPublisher bodyPublisher;

  private HttpClient(java.net.http.HttpClient client, String method, String url) {
    this.client = Objects.requireNonNull(client, "client");
    this.method = Objects.requireNonNull(method, "method");
    this.url = Objects.requireNonNull(url, "url");
  }

  public static HttpClient get(String url) {
    return new HttpClient(sharedClient, "GET", url);
  }

  public static HttpClient post(String url) {
    return new HttpClient(sharedClient, "POST", url);
  }

  public HttpClient bearer(String token) {
    this.bearerToken = token;
    return this;
  }

  public HttpClient bodyJson(Object body) {
    this.bodyPublisher =
        HttpRequest.BodyPublishers.ofString(
            gson.toJson(body), java.nio.charset.StandardCharsets.UTF_8);
    return this;
  }

  public <T> T sendJson(Class<T> responseType) {
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofSeconds(10))
              .header("Content-Type", "application/json")
              .header("Accept", "application/json");

      if (bearerToken != null) {
        builder.header("Authorization", "Bearer " + bearerToken);
      }

      if (bodyPublisher != null) {
        builder.method(method, bodyPublisher);
      } else {
        builder.method(method, HttpRequest.BodyPublishers.noBody());
      }

      HttpRequest request = builder.build();
      HttpResponse<String> response =
          client.send(
              request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        return gson.fromJson(response.body(), responseType);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "HTTP request failed: {0} {1}", new Object[] {method, url});
    }
    return null;
  }
}
