package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;

/** POST /v1/admin/bind-email - 管理员绑定玩家邮箱。 */
public final class BindEmailHandler implements AdminHandler {

  private final UserRepository users;
  private final EventBus eventBus;

  public BindEmailHandler(UserRepository users, EventBus eventBus) {
    this.users = Objects.requireNonNull(users, "users");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
  }

  @Override
  public void register(Javalin app) {
    app.post("/v1/admin/bind-email", this::handle);
  }

  private void handle(Context ctx) {
    BindEmailRequest req = ctx.bodyAsClass(BindEmailRequest.class);
    if (req.username == null
        || req.username.isBlank()
        || req.email == null
        || req.email.isBlank()) {
      ctx.status(400).json(Map.of("error", "username and email are required"));
      return;
    }

    UserDto existing = users.findByUsername(req.username).orElse(null);
    if (existing == null) {
      ctx.status(404).json(Map.of("error", "User not found"));
      return;
    }

    UserDto updated =
        UserDto.builder()
            .uuid(existing.uuid())
            .username(existing.username())
            .email(req.email)
            .premium(existing.premium())
            .createdAt(existing.createdAt())
            .lastLoginAt(existing.lastLoginAt())
            .externalUserId(existing.externalUserId())
            .build();
    users.save(updated);

    eventBus.publish(
        EventTypes.ADMIN_BIND_EMAIL, Map.of("username", req.username, "email", req.email));
    ctx.status(200).json(Map.of("success", true));
  }

  static final class BindEmailRequest {
    public String username;
    public String email;
  }
}
