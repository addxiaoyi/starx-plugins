package io.github.addxiaoyi.starx.common.crypto;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;

/** 基于 java-otp 的 TOTP 生成器。秘密以 Base32 编码字符串传入。 */
public final class TotpGenerator {

  private static final TimeBasedOneTimePasswordGenerator GENERATOR =
      new TimeBasedOneTimePasswordGenerator();
  private static final String ALGORITHM = "HmacSHA1";
  private static final String ALIAS = "HmacSHA1";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

  private TotpGenerator() {}

  /**
   * 生成随机的 Base32 编码 TOTP 密钥（20 字节 → 32 字符）。
   *
   * @return Base32 编码的密钥字符串
   */
  public static String generateSecret() {
    byte[] bytes = new byte[20];
    SECURE_RANDOM.nextBytes(bytes);
    return encodeBase32(bytes);
  }

  /**
   * 生成指定时间点的 TOTP 码。
   *
   * @param base32Secret Base32 编码的秘密
   * @param instant 时间点
   * @return 6 位数字验证码
   */
  public static String generate(String base32Secret, Instant instant) {
    try {
      return GENERATOR.generateOneTimePasswordString(buildKey(base32Secret), instant);
    } catch (InvalidKeyException e) {
      throw new IllegalStateException("Failed to generate TOTP", e);
    }
  }

  /**
   * 验证给定 TOTP 码是否在指定时间点有效。
   *
   * @param base32Secret Base32 编码的秘密
   * @param code 待验证的验证码
   * @param instant 时间点
   * @return 是否有效
   */
  public static boolean verify(String base32Secret, String code, Instant instant) {
    return generate(base32Secret, instant).equals(code);
  }

  /**
   * 生成 otpauth 配置 URI。
   *
   * @param issuer 服务名称
   * @param account 用户账号
   * @param base32Secret Base32 编码的秘密
   * @return otpauth URI
   */
  public static String provisioningUri(String issuer, String account, String base32Secret) {
    String encodedIssuer = encode(issuer);
    String encodedAccount = encode(account);
    return "otpauth://totp/"
        + encodedIssuer
        + ":"
        + encodedAccount
        + "?secret="
        + base32Secret
        + "&issuer="
        + encodedIssuer
        + "&algorithm=SHA1&digits=6&period=30";
  }

  private static Key buildKey(String base32Secret) {
    return new SecretKeySpec(Base32.decode(base32Secret), ALIAS);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static String encodeBase32(byte[] data) {
    StringBuilder sb = new StringBuilder();
    int buffer = 0;
    int bitsLeft = 0;
    for (byte b : data) {
      buffer = (buffer << 8) | (b & 0xFF);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        bitsLeft -= 5;
        sb.append(BASE32_ALPHABET.charAt((buffer >> bitsLeft) & 0x1F));
      }
    }
    if (bitsLeft > 0) {
      sb.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
    }
    return sb.toString();
  }

  /** 简化的 RFC 4648 Base32 编解码工具（仅提供本类所需的解码能力）。 */
  private static final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {}

    static byte[] decode(String input) {
      String normalized = input.toUpperCase().replace("=", "");
      int outputLength = normalized.length() * 5 / 8;
      byte[] output = new byte[outputLength];
      int buffer = 0;
      int bitsLeft = 0;
      int index = 0;
      for (char c : normalized.toCharArray()) {
        int value = ALPHABET.indexOf(c);
        if (value < 0) {
          throw new IllegalArgumentException("Invalid Base32 character: " + c);
        }
        buffer = (buffer << 5) | value;
        bitsLeft += 5;
        if (bitsLeft >= 8) {
          bitsLeft -= 8;
          output[index++] = (byte) ((buffer >> bitsLeft) & 0xFF);
        }
      }
      return output;
    }
  }
}
