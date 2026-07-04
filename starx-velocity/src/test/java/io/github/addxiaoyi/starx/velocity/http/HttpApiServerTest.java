package io.github.addxiaoyi.starx.velocity.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.github.addxiaoyi.starx.common.crypto.HmacSigner;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import io.github.addxiaoyi.starx.velocity.repository.InMemoryUserRepository;
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
  private static final String API_KEY = "secret";

  @Mock SkinBridgeModule skinBridge;
  @Mock ProxyServer proxy;

  private AutoCloseable mocks;
  private HttpApiServer server;
  private HttpClient client;
  private UserRepository userRepository;
  private EventBus eventBus;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    userRepository = new InMemoryUserRepository();
    eventBus = new VelocityEventBus();
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
    server = new HttpApiServer(configWithApiKey(null), eventBus, proxy, userRepository, skinBridge);
    server.start();

    HttpResponse<String> response = get("/v1/health", null);

    assertThat(response.statusCode()).isEqualTo(503);
    assertThat(response.body()).contains("API key not configured");
  }

  @Test
  void shouldReturn401WhenApiKeyIsWrong() throws Exception {
    server =
        new HttpApiServer(configWithApiKey(API_KEY), eventBus, proxy, userRepository, skinBridge);
    server.start();

    HttpResponse<String> response = get("/v1/health", "wrong");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void shouldReturn200ForHealthWithValidKey() throws Exception {
    server =
        new HttpApiServer(configWithApiKey(API_KEY), eventBus, proxy, userRepository, skinBridge);
    server.start();

    HttpResponse<String> response = get("/v1/health", API_KEY);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("OK");
  }

  @Test
  void shouldAuthenticateWithValidHmacSignature() throws Exception {
    server =
        new HttpApiServer(configWithApiKey(API_KEY), eventBus, proxy, userRepository, skinBridge);
    server.start();

    String timestamp = String.valueOf(System.currentTimeMillis());
    String signature = HmacSigner.sign(API_KEY, timestamp, "");

    HttpResponse<String> response = getHmac("/v1/health", timestamp, signature);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("OK");
  }

  @Test
  void shouldReturn401ForInvalidHmacSignature() throws Exception {
    server =
        new HttpApiServer(configWithApiKey(API_KEY), eventBus, proxy, userRepository, skinBridge);
    server.start();

    HttpResponse<String> response = getHmac("/v1/health", "123", "invalid-signature");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void shouldReturnUserExists() throws Exception {
    userRepository.save(UserDto.builder().uuid(UUID.randomUUID()).username("test").build());
    server =
        new HttpApiServer(configWithApiKey(API_KEY), eventBus, proxy, userRepository, skinBridge);
    server.start();

    HttpResponse<String> response = get("/v1/user/exists?username=test", API_KEY);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"exists\":true");
  }

  @Test
  void shouldRefreshSkinWhenValidRequest() throws Exception {
    userRepository.save(UserDto.builder().uuid(UUID.randomUUID()).username("alice").build());
    server =
        new HttpApiServer(configWithApiKey(API_KEY), eventBus, proxy, userRepository, skinBridge);
    server.start();

    HttpResponse<String> response = post("/v1/skin/refresh", API_KEY, "{\"username\":\"alice\"}");

    assertThat(response.statusCode()).isEqualTo(200);
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

  private HttpResponse<String> getHmac(String path, String timestamp, String signature)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + TEST_PORT + path))
            .header(HttpApiServer.TIMESTAMP_HEADER, timestamp)
            .header(HttpApiServer.SIGNATURE_HEADER, signature)
            .GET()
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
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
