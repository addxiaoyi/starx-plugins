package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.auth.AuthResult;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public final class BindEmailHandler implements AdminHandler {

  private final AuthService authService;

  public BindEmailHandler(AuthService authService) {
    this.authService = Objects.requireNonNull(authService, "authService");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.post("/v1/admin/bind-email", this::handle);
  }

  private void handle(JsonHttpExchange ctx) throws IOException {
    BindEmailRequest req = ctx.bodyAsClass(BindEmailRequest.class);
    if (req.username == null
        || req.username.isBlank()
        || req.email == null
        || req.email.isBlank()) {
      ctx.status(400).json(Map.of("error", "username and email are required"));
      return;
    }

    AuthResult result = authService.bindEmail(req.username, req.email);
    if (result.success()) {
      ctx.status(200).json(Map.of("success", true));
    } else {
      ctx.status(400).json(Map.of("error", result.message()));
    }
  }

  static final class BindEmailRequest {
    public String username;
    public String email;
  }
}
