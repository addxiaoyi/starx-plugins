package io.github.addxiaoyi.starx.common.auth;

import java.net.InetAddress;
import java.util.UUID;

/** 处理认证相关命令的纯逻辑。 */
public final class AuthCommandHandler {

  private final AuthService authService;

  public AuthCommandHandler(AuthService authService) {
    this.authService = authService;
  }

  public AuthResult handle(
      UUID uuid, String username, String rawCommand, InetAddress address, String deviceId) {
    if (rawCommand == null || rawCommand.isBlank()) {
      return AuthResult.failure("未知命令");
    }
    String[] parts = rawCommand.trim().split("\\s+", 3);
    String label = parts[0].toLowerCase();
    return switch (label) {
      case "/login", "login" -> handleLogin(uuid, username, parts, address, deviceId);
      case "/register", "register" -> handleRegister(uuid, username, parts);
      case "/2fa", "2fa" -> handle2fa(uuid, parts);
      default -> AuthResult.failure("未知命令");
    };
  }

  private AuthResult handleLogin(
      UUID uuid, String username, String[] parts, InetAddress address, String deviceId) {
    if (parts.length < 2) {
      return AuthResult.failure("用法: /login <密码>");
    }
    return authService.login(uuid, username, parts[1], null, address, deviceId);
  }

  private AuthResult handleRegister(UUID uuid, String username, String[] parts) {
    if (parts.length < 2) {
      return AuthResult.failure("用法: /register <密码> [邮箱]");
    }
    String email = parts.length >= 3 ? parts[2] : null;
    return authService.register(uuid, username, parts[1], email);
  }

  private AuthResult handle2fa(UUID uuid, String[] parts) {
    if (parts.length < 2) {
      return AuthResult.failure("用法: /2fa <验证码>");
    }
    return authService.verifyTotp(uuid, parts[1]);
  }
}
