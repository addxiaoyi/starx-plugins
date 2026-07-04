package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;

/** 用户查询处理器：/v1/user/exists、/v1/user/detail、/v1/ban。 */
public final class UserQueryHandler implements AdminHandler {

  private final UserRepository users;

  public UserQueryHandler(UserRepository users) {
    this.users = Objects.requireNonNull(users, "users");
  }

  @Override
  public void register(Javalin app) {
    app.get("/v1/user/exists", this::exists);
    app.get("/v1/user/detail", this::detail);
    app.get("/v1/ban", this::banStatus);
  }

  private void exists(Context ctx) {
    String username = ctx.queryParam("username");
    if (username == null || username.isBlank()) {
      ctx.status(400).json(Map.of("error", "username is required"));
      return;
    }
    ctx.status(200).json(Map.of("exists", users.existsByUsername(username)));
  }

  private void detail(Context ctx) {
    String username = ctx.queryParam("username");
    if (username == null || username.isBlank()) {
      ctx.status(400).json(Map.of("error", "username is required"));
      return;
    }

    UserDto user = users.findByUsername(username).orElse(null);
    if (user == null) {
      ctx.status(404).json(Map.of("error", "User not found"));
      return;
    }

    ctx.status(200)
        .json(
            Map.of(
                "uuid", user.uuid().toString(),
                "username", user.username(),
                "email", user.email() == null ? "" : user.email(),
                "premium", user.premium(),
                "externalUserId", user.externalUserId() == null ? "" : user.externalUserId()));
  }

  private void banStatus(Context ctx) {
    String username = ctx.queryParam("username");
    if (username == null || username.isBlank()) {
      ctx.status(400).json(Map.of("error", "username is required"));
      return;
    }

    // 封禁服务尚未就绪，返回占位结果。
    ctx.status(200)
        .json(
            Map.of(
                "banned", false,
                "reason", "",
                "expiresAt", ""));
  }
}
