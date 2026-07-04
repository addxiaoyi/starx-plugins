package io.github.addxiaoyi.starx.velocity.http.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import io.github.addxiaoyi.starx.velocity.repository.InMemoryUserRepository;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkExternalUserHandlerTest {

  private static final int PORT = 18795;

  private final UserRepository users = new InMemoryUserRepository();
  private final EventBus eventBus = new VelocityEventBus();
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
  void shouldUpdateExternalUserIdAndPublishEvent() throws Exception {
    users.save(UserDto.builder().uuid(UUID.randomUUID()).username("eve").build());

    AtomicReference<StarxEvent> captured = new AtomicReference<>();
    eventBus.subscribe(EventTypes.LINK_EXTERNAL_USER, captured::set);

    app = Javalin.create(config -> config.showJavalinBanner = false);
    new LinkExternalUserHandler(users, eventBus).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response =
        post("/v1/link/external-user", "{\"username\":\"eve\",\"externalUserId\":\"ext-42\"}");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(users.findByUsername("eve")).isPresent();
    assertThat(users.findByUsername("eve").get().externalUserId()).isEqualTo("ext-42");
    assertThat(captured.get()).isNotNull();
    assertThat(captured.get().type()).isEqualTo(EventTypes.LINK_EXTERNAL_USER);
    assertThat(captured.get().<String>get("username")).isEqualTo("eve");
    assertThat(captured.get().<String>get("externalUserId")).isEqualTo("ext-42");
  }

  private HttpResponse<String> post(String path, String body)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + PORT + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
