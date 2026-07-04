package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;

/** POST /v1/admin/ban - 管理员封禁玩家。 */
public final class BanHandler implements AdminHandler {

  private final UserRepository users;
  private final EventBus eventBus;

  public BanHandler(UserRepository users, EventBus eventBus) {
    this.users = Objects.requireNonNull(users, "users");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
  }

  @Override
  public void register(Javalin app) {
    app.post("/v1/admin/ban", this::handle);
  }

  private void handle(Context ctx) {
    BanRequest req = ctx.bodyAsClass(BanRequest.class);
    if (req.username == null || req.username.isBlank()) {
      ctx.status(400).json(Map.of("error", "username is required"));
      return;
    }

    if (!users.existsByUsername(req.username)) {
      ctx.status(404).json(Map.of("error", "User not found"));
      return;
    }

    eventBus.publish(
        EventTypes.ADMIN_BAN_PLAYER,
        Map.of(
            "username", req.username,
            "reason", req.reason == null ? "" : req.reason,
            "durationMinutes", req.durationMinutes == null ? 0 : req.durationMinutes));
    ctx.status(200).json(Map.of("success", true));
  }

  static final class BanRequest {
    public String username;
    public String reason;
    public Integer durationMinutes;
  }
}
