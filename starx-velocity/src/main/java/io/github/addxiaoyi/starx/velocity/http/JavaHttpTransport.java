package io.github.addxiaoyi.starx.velocity.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** 基于 java.net.http.HttpClient 的 Webhook 传输实现。 */
public final class JavaHttpTransport implements WebhookHttpTransport {

  private final HttpClient httpClient;

  public JavaHttpTransport() {
    this(HttpClient.newHttpClient());
  }

  public JavaHttpTransport(HttpClient httpClient) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
  }

  @Override
  public CompletableFuture<Void> post(String url, String body, Map<String, String> headers) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    headers.forEach(builder::header);
    return httpClient
        .sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding())
        .thenAccept(
            response -> {
              if (response.statusCode() >= 400) {
                throw new WebhookDeliveryException(
                    "Webhook delivery failed with status " + response.statusCode());
              }
            });
  }
}
