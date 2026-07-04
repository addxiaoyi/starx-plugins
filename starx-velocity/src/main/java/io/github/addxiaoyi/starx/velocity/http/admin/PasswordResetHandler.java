package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;

/** POST /v1/admin/password - 管理员重置玩家密码。 */
public final class PasswordResetHandler implements AdminHandler {

  private final UserRepository users;
  private final EventBus eventBus;

  public PasswordResetHandler(UserRepository users, EventBus eventBus) {
    this.users = Objects.requireNonNull(users, "users");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
  }

  @Override
  public void register(Javalin app) {
    app.post("/v1/admin/password", this::handle);
  }

  private void handle(Context ctx) {
    PasswordResetRequest req = ctx.bodyAsClass(PasswordResetRequest.class);
    if (req.username == null
        || req.username.isBlank()
        || req.newPassword == null
        || req.newPassword.isBlank()) {
      ctx.status(400).json(Map.of("error", "username and newPassword are required"));
      return;
    }

    if (!users.existsByUsername(req.username)) {
      ctx.status(404).json(Map.of("error", "User not found"));
      return;
    }

    eventBus.publish(
        EventTypes.ADMIN_RESET_PASSWORD,
        Map.of("username", req.username, "newPassword", req.newPassword));
    ctx.status(200).json(Map.of("success", true));
  }

  static final class PasswordResetRequest {
    public String username;
    public String newPassword;
  }
}
