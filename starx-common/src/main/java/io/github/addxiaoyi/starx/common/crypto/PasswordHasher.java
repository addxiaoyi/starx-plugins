package io.github.addxiaoyi.starx.common.crypto;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** BCrypt 密码哈希包装类。 */
public final class PasswordHasher {

  private static final int COST_FACTOR = 12;

  private PasswordHasher() {}

  /**
   * 对明文密码进行哈希。
   *
   * @param password 明文密码
   * @return BCrypt 哈希字符串
   */
  public static String hash(String password) {
    return BCrypt.withDefaults().hashToString(COST_FACTOR, password.toCharArray());
  }

  /**
   * 验证明文密码是否与哈希匹配。
   *
   * @param password 明文密码
   * @param hash 已有的 BCrypt 哈希
   * @return 是否匹配
   */
  public static boolean verify(String password, String hash) {
    return BCrypt.verifyer().verify(password.toCharArray(), hash.toCharArray()).verified;
  }
}
