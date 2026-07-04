package io.github.addxiaoyi.starx.velocity.security;

/** Webhook 请求签名器接口。 starx-common 提供最终实现前，Velocity 端使用本地适配器。 */
public interface WebhookSigner {

  /** 对请求体进行签名并返回签名值。 */
  String sign(String payload);
}
