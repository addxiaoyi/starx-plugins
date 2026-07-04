package io.github.addxiaoyi.starx.velocity.http.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.github.addxiaoyi.starx.velocity.repository.InMemoryUserRepository;
import io.javalin.Javalin;
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

class UserQueryHandlerTest {

  private static final int PORT = 18797;

  private final UserRepository users = new InMemoryUserRepository();
  private Javalin app;
  private HttpClient client;

  @BeforeEach
  void setUp() {
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  @AfterEach
  void tearDown() {
    if (app != null) {
      app.stop();
    }
  }

  @Test
  void shouldReturnExistsTrue() throws Exception {
    users.save(UserDto.builder().uuid(UUID.randomUUID()).username("henry").build());

    app = Javalin.create(config -> config.showJavalinBanner = false);
    new UserQueryHandler(users).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response = get("/v1/user/exists?username=henry");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"exists\":true");
  }

  @Test
  void shouldReturnUserDetail() throws Exception {
    users.save(
        UserDto.builder()
            .uuid(UUID.randomUUID())
            .username("henry")
            .email("henry@example.com")
            .build());

    app = Javalin.create(config -> config.showJavalinBanner = false);
    new UserQueryHandler(users).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response = get("/v1/user/detail?username=henry");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("henry").contains("henry@example.com");
  }

  @Test
  void shouldReturnBanPlaceholder() throws Exception {
    users.save(UserDto.builder().uuid(UUID.randomUUID()).username("henry").build());

    app = Javalin.create(config -> config.showJavalinBanner = false);
    new UserQueryHandler(users).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response = get("/v1/ban?username=henry");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"banned\":false");
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + PORT + path)).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
