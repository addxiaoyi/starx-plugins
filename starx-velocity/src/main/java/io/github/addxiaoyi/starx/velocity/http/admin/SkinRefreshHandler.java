package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;

/** POST /v1/skin/refresh - 请求刷新玩家皮肤。 */
public final class SkinRefreshHandler implements AdminHandler {

  private final UserRepository users;
  private final EventBus eventBus;

  public SkinRefreshHandler(UserRepository users, EventBus eventBus) {
    this.users = Objects.requireNonNull(users, "users");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
  }

  @Override
  public void register(Javalin app) {
    app.post("/v1/skin/refresh", this::handle);
  }

  private void handle(Context ctx) {
    SkinRefreshRequest req = ctx.bodyAsClass(SkinRefreshRequest.class);
    if (req.username == null || req.username.isBlank()) {
      ctx.status(400).json(Map.of("error", "username is required"));
      return;
    }

    UserDto user = users.findByUsername(req.username).orElse(null);
    if (user == null) {
      ctx.status(404).json(Map.of("error", "User not found"));
      return;
    }

    eventBus.publish(
        EventTypes.SKIN_REFRESH_REQUEST,
        Map.of("username", req.username, "uuid", user.uuid().toString()));
    ctx.status(200).json(Map.of("success", true));
  }

  static final class SkinRefreshRequest {
    public String username;
  }
}
