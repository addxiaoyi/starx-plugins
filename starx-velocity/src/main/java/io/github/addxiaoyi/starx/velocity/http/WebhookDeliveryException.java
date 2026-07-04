package io.github.addxiaoyi.starx.velocity.http;

/** Webhook 投递失败时抛出的异常。 */
public final class WebhookDeliveryException extends RuntimeException {

  public WebhookDeliveryException(String message) {
    super(message);
  }
}
