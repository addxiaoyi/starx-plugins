package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.github.addxiaoyi.starx.common.auth.AuthResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;

/** POST /v1/admin/reset-password - 管理员重置玩家密码。 */
public final class PasswordResetHandler implements AdminHandler {

  private final AuthService authService;

  public PasswordResetHandler(AuthService authService) {
    this.authService = Objects.requireNonNull(authService, "authService");
  }

  @Override
  public void register(Javalin app) {
    app.post("/v1/admin/reset-password", this::handle);
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

    AuthResult result = authService.resetPassword(req.username, req.newPassword);
    if (result.success()) {
      ctx.status(200).json(Map.of("success", true));
    } else {
      ctx.status(400).json(Map.of("error", result.message()));
    }
  }

  static final class PasswordResetRequest {
    public String username;
    public String newPassword;
  }
}