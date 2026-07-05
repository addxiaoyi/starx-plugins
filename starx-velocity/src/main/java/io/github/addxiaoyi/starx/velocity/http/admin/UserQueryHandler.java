package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** GET /v1/user/exists /v1/user/detail - 查询玩家是否存在和详情。 */
public final class UserQueryHandler implements AdminHandler {

  private final JdbiUserRepository users;

  public UserQueryHandler(JdbiUserRepository users) {
    this.users = Objects.requireNonNull(users, "users");
  }

  @Override
  public void register(Javalin app) {
    app.get("/v1/user/exists", this::handleExists);
    app.get("/v1/user/detail", this::handleDetail);
  }

  private void handleExists(Context ctx) {
    String name = ctx.queryParam("name");
    if (name == null || name.isBlank()) {
      ctx.status(400).json(Map.of("error", "name is required"));
      return;
    }
    boolean exists = users.existsByUsername(name);
    ctx.status(200).json(Map.of("exists", exists, "name", name));
  }

  private void handleDetail(Context ctx) {
    String name = ctx.queryParam("name");
    if (name == null || name.isBlank()) {
      ctx.status(400).json(Map.of("error", "name is required"));
      return;
    }
    Optional<StarxUser> user = users.findFullByUsername(name);
    if (user.isEmpty()) {
      ctx.status(404).json(Map.of("error", "User not found"));
      return;
    }
    StarxUser u = user.get();
    ctx.status(200)
        .json(
            Map.of(
                "uuid",
                u.uuid().toString(),
                "username",
                u.username(),
                "registered",
                true,
                "email",
                u.email() == null ? "" : u.email(),
                "totpEnabled",
                u.totpSecret() != null,
                "lastLoginAt",
                u.lastLoginAt() == null ? "" : u.lastLoginAt().toString(),
                "externalUserId",
                u.externalUserId() == null ? "" : u.externalUserId()));
  }
}
