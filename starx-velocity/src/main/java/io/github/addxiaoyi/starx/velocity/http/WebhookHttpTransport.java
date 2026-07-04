package io.github.addxiaoyi.starx.velocity.http;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Webhook HTTP 传输抽象，便于单元测试替换为内存实现。 */
public interface WebhookHttpTransport {

  CompletableFuture<Void> post(String url, String body, Map<String, String> headers);
}
