package io.github.addxiaoyi.starx.common.security;

/** 密码复杂度校验工具。 */
public final class PasswordValidator {

  private PasswordValidator() {}

  private static final int MIN_LENGTH = 6;
  private static final int MAX_LENGTH = 128;

  /**
   * 校验密码复杂度。
   *
   * @return null 表示通过，否则返回错误消息
   */
  public static String validate(String password) {
    if (password == null || password.isEmpty()) {
      return "密码不能为空";
    }
    if (password.length() < MIN_LENGTH) {
      return "密码长度至少" + MIN_LENGTH + "位";
    }
    if (password.length() > MAX_LENGTH) {
      return "密码长度不能超过" + MAX_LENGTH + "位";
    }

    boolean hasLetter = false;
    boolean hasDigit = false;
    for (int i = 0; i < password.length(); i++) {
      char c = password.charAt(i);
      if (Character.isLetter(c)) {
        hasLetter = true;
      } else if (Character.isDigit(c)) {
        hasDigit = true;
      }
      if (hasLetter && hasDigit) {
        break;
      }
    }

    if (!hasLetter || !hasDigit) {
      return "密码必须包含字母和数字";
    }
    return null;
  }
}
