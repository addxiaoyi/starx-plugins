package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.auth.AuthResult;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;

/** POST /v1/admin/delete-user - 管理员删除玩家账户。 */
public final class DeleteUserHandler implements AdminHandler {

  private final AuthService authService;

  public DeleteUserHandler(AuthService authService) {
    this.authService = Objects.requireNonNull(authService, "authService");
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

    AuthResult result = authService.deleteUser(req.username);
    if (result.success()) {
      ctx.status(200).json(Map.of("success", true));
    } else {
      ctx.status(400).json(Map.of("error", result.message()));
    }
  }

  static final class DeleteUserRequest {
    public String username;
  }
}