package io.github.addxiaoyi.starx.velocity.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 基于 HMAC-SHA256 的 Webhook 签名实现。 */
public final class HmacWebhookSigner implements WebhookSigner {

  private static final String ALGORITHM = "HmacSHA256";

  private final String secret;

  public HmacWebhookSigner(String secret) {
    this.secret = secret;
  }

  @Override
  public String sign(String payload) {
    if (secret == null || secret.isBlank()) {
      return "";
    }
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(signature);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Failed to sign webhook payload", e);
    }
  }
}
