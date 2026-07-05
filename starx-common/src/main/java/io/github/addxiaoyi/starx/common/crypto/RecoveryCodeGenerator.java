package io.github.addxiaoyi.starx.common.crypto;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/** 2FA 恢复码生成器：生成 8 个一次性恢复码，BCrypt 哈希存储。 */
public final class RecoveryCodeGenerator {

  private RecoveryCodeGenerator() {}

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int CODE_COUNT = 8;
  private static final int CODE_LENGTH = 10;
  private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  /** 生成 8 个恢复码（明文，仅展示一次）。 */
  public static List<String> generate() {
    List<String> codes = new ArrayList<>();
    for (int i = 0; i < CODE_COUNT; i++) {
      StringBuilder sb = new StringBuilder(CODE_LENGTH);
      for (int j = 0; j < CODE_LENGTH; j++) {
        sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
      }
      codes.add(sb.toString());
    }
    return codes;
  }
}
