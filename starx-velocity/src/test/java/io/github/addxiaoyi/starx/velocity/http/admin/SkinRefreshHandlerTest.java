package io.github.addxiaoyi.starx.velocity.http.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import io.github.addxiaoyi.starx.velocity.http.TestHttpServer;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SkinRefreshHandlerTest {

  private static final int PORT = 18796;

  @Mock private SkinBridgeModule skinBridge;
  @Mock private JdbcUserRepository jdbiUserRepository;

  private AutoCloseable mocks;
  private TestHttpServer server;
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
  void shouldPublishSkinRefreshRequestByUsername() throws Exception {
    UUID uuid = UUID.randomUUID();
    StarxUser user =
        new StarxUser(
            uuid,
            "grace",
            null,
            null,
            null,
            false,
            Instant.now(),
            null,
            null,
            List.of(),
            null,
            null,
            "completed",
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    when(jdbiUserRepository.findFullByUsername("grace")).thenReturn(Optional.of(user));

    server = new TestHttpServer(PORT);
    new SkinRefreshHandler(skinBridge, jdbiUserRepository).register(server);
    server.start();

    HttpResponse<String> response = post("/v1/admin/skin-refresh", "{\"username\":\"grace\"}");

    assertThat(response.statusCode()).isEqualTo(200);
    verify(skinBridge).refreshSkin(uuid);
  }

  @Test
  void shouldReturn404ForUnknownUsername() throws Exception {
    when(jdbiUserRepository.findFullByUsername("unknown")).thenReturn(Optional.empty());

    server = new TestHttpServer(PORT);
    new SkinRefreshHandler(skinBridge, jdbiUserRepository).register(server);
    server.start();

    HttpResponse<String> response = post("/v1/admin/skin-refresh", "{\"username\":\"unknown\"}");

    assertThat(response.statusCode()).isEqualTo(404);
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + PORT + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
