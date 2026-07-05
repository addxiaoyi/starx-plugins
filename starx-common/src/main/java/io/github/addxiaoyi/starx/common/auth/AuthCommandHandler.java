package io.github.addxiaoyi.starx.common.auth;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

/** 处理认证相关命令的纯逻辑。 玩家在 Limbo 内直接输入密码即可，系统自动判断是注册还是登录。 */
public final class AuthCommandHandler {

  private final AuthService authService;

  public AuthCommandHandler(AuthService authService) {
    this.authService = authService;
  }

  /**
   * 处理玩家在聊天框中的输入，自动判断意图：
   *
   * <ul>
   *   <li>未注册 → 自动注册
   *   <li>已注册且会话为 GUEST → 自动登录
   *   <li>会话为 AUTHENTICATING → 二步验证
   * </ul>
   */
  public AuthResult handle(
      UUID uuid, String username, String rawInput, InetAddress address, String deviceId) {
    if (rawInput == null || rawInput.isBlank()) {
      return AuthResult.failure("密码不能为空");
    }
    String input = rawInput.trim();

    if (!authService.isUserRegistered(uuid)) {
      return handleRegister(uuid, username, input);
    }

    Optional<AuthSession.State> sessionState = authService.getSessionState(uuid);
    if (sessionState.isPresent() && sessionState.get() == AuthSession.State.AUTHENTICATING) {
      return handle2fa(uuid, input);
    }

    return handleLogin(uuid, username, input, address, deviceId);
  }

  private AuthResult handleLogin(
      UUID uuid, String username, String password, InetAddress address, String deviceId) {
    return authService.login(uuid, username, password, null, address, deviceId);
  }

  private AuthResult handleRegister(UUID uuid, String username, String password) {
    return authService.register(uuid, username, password, null);
  }

  private AuthResult handle2fa(UUID uuid, String code) {
    return authService.verifyTotp(uuid, code);
  }
}
