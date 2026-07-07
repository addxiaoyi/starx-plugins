package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SkinRefreshHandler implements AdminHandler {

  private final SkinBridgeModule skinBridge;
  private final JdbcUserRepository users;

  public SkinRefreshHandler(SkinBridgeModule skinBridge, JdbcUserRepository users) {
    this.skinBridge = Objects.requireNonNull(skinBridge, "skinBridge");
    this.users = Objects.requireNonNull(users, "users");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.post("/v1/admin/skin-refresh", this::handle);
  }

  private void handle(JsonHttpExchange ctx) throws IOException {
    SkinRefreshRequest req = ctx.bodyAsClass(SkinRefreshRequest.class);
    if (req.username == null || req.username.isBlank()) {
      ctx.status(400).json(Map.of("error", "username is required"));
      return;
    }

    Optional<StarxUser> user = users.findFullByUsername(req.username);
    if (user.isEmpty()) {
      ctx.status(404).json(Map.of("error", "User not found"));
      return;
    }

    skinBridge.refreshSkin(user.get().uuid());
    ctx.status(200).json(Map.of("success", true));
  }

  static final class SkinRefreshRequest {
    public String username;
  }
}
