package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;

/** POST /v1/link/external-user - 关联外部网站用户 ID。 */
public final class LinkExternalUserHandler implements AdminHandler {

  private final UserRepository users;
  private final EventBus eventBus;

  public LinkExternalUserHandler(UserRepository users, EventBus eventBus) {
    this.users = Objects.requireNonNull(users, "users");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
  }

  @Override
  public void register(Javalin app) {
    app.post("/v1/link/external-user", this::handle);
  }

  private void handle(Context ctx) {
    LinkExternalUserRequest req = ctx.bodyAsClass(LinkExternalUserRequest.class);
    if (req.username == null
        || req.username.isBlank()
        || req.externalUserId == null
        || req.externalUserId.isBlank()) {
      ctx.status(400).json(Map.of("error", "username and externalUserId are required"));
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
            .email(existing.email())
            .premium(existing.premium())
            .createdAt(existing.createdAt())
            .lastLoginAt(existing.lastLoginAt())
            .externalUserId(req.externalUserId)
            .build();
    users.save(updated);

    eventBus.publish(
        EventTypes.LINK_EXTERNAL_USER,
        Map.of("username", req.username, "externalUserId", req.externalUserId));
    ctx.status(200).json(Map.of("success", true));
  }

  static final class LinkExternalUserRequest {
    public String username;
    public String externalUserId;
  }
}
