package io.github.addxiaoyi.starx.velocity.http;

import com.google.gson.Gson;
import io.github.addxiaoyi.starx.api.dto.WebhookPayload;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.security.WebhookSigner;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** 向网站后端发送带签名的 Webhook 请求。 */
public final class WebhookClient {

  public static final String SIGNATURE_HEADER = "X-StarX-Signature";

  private final StarxConfig.WebhookConfig config;
  private final WebhookSigner signer;
  private final WebhookHttpTransport transport;
  private final Gson gson = new Gson();

  public WebhookClient(StarxConfig.WebhookConfig config, WebhookSigner signer) {
    this(config, signer, new JavaHttpTransport());
  }

  public WebhookClient(
      StarxConfig.WebhookConfig config, WebhookSigner signer, WebhookHttpTransport transport) {
    this.config = Objects.requireNonNull(config, "config");
    this.signer = Objects.requireNonNull(signer, "signer");
    this.transport = Objects.requireNonNull(transport, "transport");
  }

  /** 发送 WebhookPayload；若未配置回调地址则立即返回已完成 future。 */
  public CompletableFuture<Void> send(WebhookPayload payload) {
    Objects.requireNonNull(payload, "payload");
    if (!config.isConfigured()) {
      return CompletableFuture.completedFuture(null);
    }

    String body = serialize(payload);
    String signature = signer.sign(body);
    Map<String, String> headers =
        signature.isBlank()
            ? Map.of("Content-Type", "application/json")
            : Map.of("Content-Type", "application/json", SIGNATURE_HEADER, signature);

    return transport.post(config.url(), body, headers);
  }

  /** 向任意 URL 发送 JSON body，返回 CompletableFuture。 */
  public CompletableFuture<Void> post(String url, Map<String, Object> body) {
    Objects.requireNonNull(url, "url");
    Objects.requireNonNull(body, "body");
    String json = gson.toJson(body);
    Map<String, String> headers = Map.of("Content-Type", "application/json");
    return transport.post(url, json, headers);
  }

  private String serialize(WebhookPayload payload) {
    Map<String, Object> map =
        Map.of(
            "eventId", payload.eventId(),
            "eventType", payload.eventType(),
            "timestamp", payload.timestamp().toString(),
            "data", payload.data());
    return gson.toJson(map);
  }
}
