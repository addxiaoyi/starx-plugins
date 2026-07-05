package io.github.addxiaoyi.starx.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 签名工具。签名原文为原始 JSON 正文，与网站后端
 * {@code crypto.createHmac('sha256', secret).update(rawPayload, 'utf8').digest('hex')} 保持一致。
 */
public final class HmacSigner {

  private static final String ALGORITHM = "HmacSHA256";
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private HmacSigner() {}

  /**
   * 对原始 JSON 正文进行 HMAC-SHA256 签名，返回小写十六进制字符串。
   *
   * @param secret 签名密钥
   * @param rawBody 原始 JSON 正文
   * @return 十六进制签名
   */
  public static String sign(String secret, String rawBody) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      return bytesToHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute HMAC-SHA256 signature", e);
    }
  }

  /**
   * 验证签名是否匹配。
   *
   * @param secret 签名密钥
   * @param rawBody 原始 JSON 正文
   * @param signature 待验证的十六进制签名
   * @return 是否匹配
   */
  public static boolean verify(String secret, String rawBody, String signature) {
    String expected = sign(secret, rawBody);
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
  }

  private static String bytesToHex(byte[] bytes) {
    char[] chars = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      chars[i * 2] = HEX[v >>> 4];
      chars[i * 2 + 1] = HEX[v & 0x0F];
    }
    return new String(chars);
  }
}