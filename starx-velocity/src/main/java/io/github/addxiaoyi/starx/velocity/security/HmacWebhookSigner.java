package io.github.addxiaoyi.starx.velocity.security;

import io.github.addxiaoyi.starx.common.crypto.HmacSigner;

/** 基于 HMAC-SHA256 的 Webhook 签名实现，输出十六进制与网站 {@code crypto.createHmac('sha256', secret).update(rawPayload, 'utf8').digest('hex')} 一致。 */
public final class HmacWebhookSigner implements WebhookSigner {

  private final String secret;

  public HmacWebhookSigner(String secret) {
    this.secret = secret;
  }

  @Override
  public String sign(String payload) {
    if (secret == null || secret.isBlank()) {
      return "";
    }
    return HmacSigner.sign(secret, payload);
  }
}
