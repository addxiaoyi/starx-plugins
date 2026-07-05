package io.github.addxiaoyi.starx.velocity.http.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import io.javalin.Javalin;
import java.io.IOException;
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

class UserQueryHandlerTest {

  private static final int PORT = 18797;

  @Mock private JdbiUserRepository jdbiUserRepository;

  private AutoCloseable mocks;
  private Javalin app;
  private HttpClient client;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (app != null) {
      app.stop();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  void shouldReturnExistsTrue() throws Exception {
    when(jdbiUserRepository.existsByUsername("henry")).thenReturn(true);

    app = Javalin.create(config -> config.showJavalinBanner = false);
    new UserQueryHandler(jdbiUserRepository).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response = get("/v1/user/exists?name=henry");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"exists\":true");
  }

  @Test
  void shouldReturnUserDetail() throws Exception {
    StarxUser user =
        new StarxUser(
            UUID.randomUUID(),
            "henry",
            "henry@example.com",
            null,
            null,
            false,
            Instant.now(),
            null,
            null,
            List.of());
    when(jdbiUserRepository.findFullByUsername("henry")).thenReturn(Optional.of(user));

    app = Javalin.create(config -> config.showJavalinBanner = false);
    new UserQueryHandler(jdbiUserRepository).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response = get("/v1/user/detail?name=henry");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("henry").contains("henry@example.com");
  }

  @Test
  void shouldReturnBanPlaceholder() throws Exception {
    app = Javalin.create(config -> config.showJavalinBanner = false);
    new UserQueryHandler(jdbiUserRepository).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response = get("/v1/ban?name=henry");

    assertThat(response.statusCode()).isEqualTo(404);
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + PORT + path)).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
