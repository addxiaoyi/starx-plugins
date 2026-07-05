package io.github.addxiaoyi.starx.common.auth;

/** 认证操作结果。 */
public record AuthResult(boolean success, String message, AuthSession.State state) {

  public static AuthResult success(String message) {
    return new AuthResult(true, message, AuthSession.State.AUTHENTICATED);
  }

  public static AuthResult success(String message, AuthSession.State state) {
    return new AuthResult(true, message, state);
  }

  public static AuthResult failure(String message) {
    return new AuthResult(false, message, AuthSession.State.GUEST);
  }

  public static AuthResult totpEnabled(String secret, java.util.List<String> recoveryCodes) {
    return new AuthResult(
        true,
        "二步验证已开启！密钥: " + secret + " | 恢复码: " + String.join(", ", recoveryCodes),
        AuthSession.State.AUTHENTICATED);
  }
}
