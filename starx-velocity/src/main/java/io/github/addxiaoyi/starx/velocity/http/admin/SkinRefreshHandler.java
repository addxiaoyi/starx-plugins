package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** POST /v1/admin/skin-refresh - 请求刷新玩家皮肤。 */
public final class SkinRefreshHandler implements AdminHandler {

  private final SkinBridgeModule skinBridge;
  private final JdbiUserRepository users;

  public SkinRefreshHandler(SkinBridgeModule skinBridge, JdbiUserRepository users) {
    this.skinBridge = Objects.requireNonNull(skinBridge, "skinBridge");
    this.users = Objects.requireNonNull(users, "users");
  }

  @Override
  public void register(Javalin app) {
    app.post("/v1/admin/skin-refresh", this::handle);
  }

  private void handle(Context ctx) {
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