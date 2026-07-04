package io.github.addxiaoyi.starx.velocity.http.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KickHandlerTest {

  private static final int PORT = 18793;

  private final ProxyServer proxy = mock(ProxyServer.class);
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
  void shouldKickOnlinePlayerAndPublishEvent() throws Exception {
    Player player = mock(Player.class);
    when(proxy.getPlayer("frank")).thenReturn(Optional.of(player));

    AtomicReference<StarxEvent> captured = new AtomicReference<>();
    eventBus.subscribe(EventTypes.ADMIN_KICK_PLAYER, captured::set);

    app = Javalin.create(config -> config.showJavalinBanner = false);
    new KickHandler(proxy, eventBus).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response =
        post("/v1/admin/kick-online", "{\"username\":\"frank\",\"reason\":\"afk\"}");

    assertThat(response.statusCode()).isEqualTo(200);
    verify(player).disconnect(Component.text("afk"));
    assertThat(captured.get()).isNotNull();
    assertThat(captured.get().type()).isEqualTo(EventTypes.ADMIN_KICK_PLAYER);
    assertThat(captured.get().<String>get("username")).isEqualTo("frank");
    assertThat(captured.get().<String>get("reason")).isEqualTo("afk");
  }

  @Test
  void shouldReturn404WhenPlayerNotOnline() throws Exception {
    when(proxy.getPlayer("ghost")).thenReturn(Optional.empty());

    app = Javalin.create(config -> config.showJavalinBanner = false);
    new KickHandler(proxy, eventBus).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response =
        post("/v1/admin/kick-online", "{\"username\":\"ghost\",\"reason\":\"afk\"}");

    assertThat(response.statusCode()).isEqualTo(404);
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
