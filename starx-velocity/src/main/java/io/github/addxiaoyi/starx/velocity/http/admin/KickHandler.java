package io.github.addxiaoyi.starx.velocity.http.admin;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;

public final class KickHandler implements AdminHandler {

  private final ProxyServer proxy;
  private final EventBus eventBus;

  public KickHandler(ProxyServer proxy, EventBus eventBus) {
    this.proxy = Objects.requireNonNull(proxy, "proxy");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.post("/v1/admin/kick-online", this::handle);
  }

  private void handle(JsonHttpExchange ctx) throws IOException {
    KickRequest req = ctx.bodyAsClass(KickRequest.class);
    if (req.username == null || req.username.isBlank()) {
      ctx.status(400).json(Map.of("error", "username is required"));
      return;
    }

    Optional<Player> online = proxy.getPlayer(req.username);
    if (online.isEmpty()) {
      ctx.status(404).json(Map.of("error", "Player not online"));
      return;
    }

    String reason = req.reason == null || req.reason.isBlank() ? "Kicked by admin" : req.reason;
    online.get().disconnect(Component.text(reason));

    eventBus.publish(
        EventTypes.ADMIN_KICK_PLAYER, Map.of("username", req.username, "reason", reason));
    ctx.status(200).json(Map.of("success", true));
  }

  static final class KickRequest {
    public String username;
    public String reason;
  }
}
