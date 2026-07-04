package io.github.addxiaoyi.starx.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 出站 Webhook 请求体。与网站后端 webhook 接收端契约对齐。 */
public final class WebhookPayload {

  private final UUID eventId;
  private final String eventType;
  private final Instant timestamp;
  private final Map<String, Object> data;

  public WebhookPayload(String eventType, Map<String, Object> data) {
    this(UUID.randomUUID(), eventType, Instant.now(), data);
  }

  public WebhookPayload(
      UUID eventId, String eventType, Instant timestamp, Map<String, Object> data) {
    this.eventId = Objects.requireNonNull(eventId, "eventId");
    this.eventType = Objects.requireNonNull(eventType, "eventType");
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    this.data = data == null ? Map.of() : Map.copyOf(data);
  }

  public UUID eventId() {
    return eventId;
  }

  public String eventType() {
    return eventType;
  }

  public Instant timestamp() {
    return timestamp;
  }

  public Map<String, Object> data() {
    return data;
  }
}
