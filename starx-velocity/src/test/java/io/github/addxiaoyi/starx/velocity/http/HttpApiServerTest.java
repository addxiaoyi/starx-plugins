package io.github.addxiaoyi.starx.velocity.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class HttpApiServerTest {

  private static final int TEST_PORT = 18788;

  @Mock SkinBridgeModule skinBridge;

  private AutoCloseable mocks;
  private HttpApiServer server;
  private HttpClient client;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  void shouldReturn503WhenApiKeyNotConfigured() throws Exception {
    server = new HttpApiServer(configWithApiKey(null), skinBridge);
    server.start();

    HttpResponse<String> response = get("/v1/health", null);

    assertThat(response.statusCode()).isEqualTo(503);
    assertThat(response.body()).contains("API key not configured");
  }

  @Test
  void shouldReturn401WhenApiKeyIsWrong() throws Exception {
    server = new HttpApiServer(configWithApiKey("secret"), skinBridge);
    server.start();

    HttpResponse<String> response = get("/v1/health", "wrong");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void shouldReturn200ForHealthWithValidKey() throws Exception {
    server = new HttpApiServer(configWithApiKey("secret"), skinBridge);
    server.start();

    HttpResponse<String> response = get("/v1/health", "secret");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("OK");
  }

  @Test
  void shouldReturn501ForUserExists() throws Exception {
    server = new HttpApiServer(configWithApiKey("secret"), skinBridge);
    server.start();

    HttpResponse<String> response = get("/v1/user/exists?username=test", "secret");

    assertThat(response.statusCode()).isEqualTo(501);
  }

  @Test
  void shouldRefreshSkinWhenValidRequest() throws Exception {
    server = new HttpApiServer(configWithApiKey("secret"), skinBridge);
    server.start();

    UUID uuid = UUID.randomUUID();
    HttpResponse<String> response =
        post("/v1/skin/refresh", "secret", "{\"uuid\":\"" + uuid + "\"}");

    assertThat(response.statusCode()).isEqualTo(200);
    verify(skinBridge).refreshSkin(uuid);
  }

  @Test
  void shouldReturn400ForInvalidUuid() throws Exception {
    server = new HttpApiServer(configWithApiKey("secret"), skinBridge);
    server.start();

    HttpResponse<String> response = post("/v1/skin/refresh", "secret", "{\"uuid\":\"not-a-uuid\"}");

    assertThat(response.statusCode()).isEqualTo(400);
  }

  private StarxConfig configWithApiKey(String apiKey) {
    return new StarxConfig(
        apiKey,
        new StarxConfig.HttpConfig("127.0.0.1", TEST_PORT),
        new StarxConfig.WebhookConfig("", ""),
        java.util.Map.of());
  }

  private HttpResponse<String> get(String path, String apiKey)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + TEST_PORT + path));
    if (apiKey != null) {
      builder.header(HttpApiServer.API_KEY_HEADER, apiKey);
    }
    return client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, String apiKey, String body)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + TEST_PORT + path))
            .header("Content-Type", "application/json")
            .header(HttpApiServer.API_KEY_HEADER, apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
