package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;

/** POST /v1/admin/delete-user - 管理员删除玩家账户。 */
public final class DeleteUserHandler implements AdminHandler {

  private final UserRepository users;

  public DeleteUserHandler(UserRepository users) {
    this.users = Objects.requireNonNull(users, "users");
  }

  @Override
  public void register(Javalin app) {
    app.post("/v1/admin/delete-user", this::handle);
  }

  private void handle(Context ctx) {
    DeleteUserRequest req = ctx.bodyAsClass(DeleteUserRequest.class);
    if (req.username == null || req.username.isBlank()) {
      ctx.status(400).json(Map.of("error", "username is required"));
      return;
    }

    UserDto existing = users.findByUsername(req.username).orElse(null);
    if (existing == null) {
      ctx.status(404).json(Map.of("error", "User not found"));
      return;
    }

    users.delete(existing.uuid());
    ctx.status(200).json(Map.of("success", true));
  }

  static final class DeleteUserRequest {
    public String username;
  }
}
