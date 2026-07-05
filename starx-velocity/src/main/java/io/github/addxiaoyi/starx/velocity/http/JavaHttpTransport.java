package io.github.addxiaoyi.starx.velocity.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 基于 java.net.http.HttpClient 的 Webhook 传输实现，共享单例 HttpClient 和连接池。 */
public final class JavaHttpTransport implements WebhookHttpTransport {

  private static final Logger LOGGER = Logger.getLogger(JavaHttpTransport.class.getName());

  private static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  private final HttpClient httpClient;

  public JavaHttpTransport() {
    this(SHARED_CLIENT);
  }

  public JavaHttpTransport(HttpClient httpClient) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
  }

  @Override
  public CompletableFuture<Void> post(String url, String body, Map<String, String> headers) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    headers.forEach(builder::header);
    return httpClient
        .sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding())
        .thenAccept(
            response -> {
              if (response.statusCode() >= 400) {
                LOGGER.log(Level.WARNING,
                    "Webhook delivery failed: {0} {1}", new Object[]{response.statusCode(), url});
              }
            })
        .exceptionally(ex -> {
          LOGGER.log(Level.WARNING, "Webhook delivery error: " + url, ex);
          return null;
        });
  }
}