package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.auth.AuthResult;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public final class PasswordResetHandler implements AdminHandler {

  private final AuthService authService;

  public PasswordResetHandler(AuthService authService) {
    this.authService = Objects.requireNonNull(authService, "authService");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.post("/v1/admin/reset-password", this::handle);
  }

  private void handle(JsonHttpExchange ctx) throws IOException {
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
