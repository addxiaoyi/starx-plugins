package io.github.addxiaoyi.starx.velocity.http.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import io.github.addxiaoyi.starx.velocity.http.TestHttpServer;
import io.github.addxiaoyi.starx.velocity.repository.InMemoryUserRepository;
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

@SuppressWarnings("removal")
class LinkExternalUserHandlerTest {

  private final UserRepository users = new InMemoryUserRepository();
  private final EventBus eventBus = new VelocityEventBus();
  private TestHttpServer server;
  private HttpClient client;

  @BeforeEach
  void setUp() {
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  void shouldUpdateExternalUserIdAndPublishEvent() throws Exception {
    users.save(UserDto.builder().uuid(UUID.randomUUID()).username("eve").build());

    AtomicReference<StarxEvent> captured = new AtomicReference<>();
    eventBus.subscribe(EventTypes.LINK_EXTERNAL_USER, captured::set);

    server = new TestHttpServer(0);
    new LinkExternalUserHandler(users, eventBus).register(server);
    server.start();

    HttpResponse<String> response =
        post(
            "/v1/admin/link-external-user", "{\"username\":\"eve\",\"externalUserId\":\"ext-42\"}");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(users.findByUsername("eve")).isPresent();
    assertThat(users.findByUsername("eve").get().externalUserId()).isEqualTo("ext-42");
    Thread.sleep(200);
    assertThat(captured.get()).isNotNull();
    assertThat(captured.get().type()).isEqualTo(EventTypes.LINK_EXTERNAL_USER);
    assertThat(captured.get().<String>get("username")).isEqualTo("eve");
    assertThat(captured.get().<String>get("externalUserId")).isEqualTo("ext-42");
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
