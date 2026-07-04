package io.github.addxiaoyi.starx.api.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 平台无关的事件 envelope。所有跨模块/跨进程通信事件都包装在此对象中。 */
public final class StarxEvent {

  private final String type;
  private final UUID eventId;
  private final Instant timestamp;
  private final Map<String, Object> payload;

  public StarxEvent(String type, Map<String, Object> payload) {
    this(type, UUID.randomUUID(), Instant.now(), payload);
  }

  public StarxEvent(String type, UUID eventId, Instant timestamp, Map<String, Object> payload) {
    this.type = Objects.requireNonNull(type, "type");
    this.eventId = Objects.requireNonNull(eventId, "eventId");
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    this.payload = payload == null ? Map.of() : Map.copyOf(payload);
  }

  public String type() {
    return type;
  }

  public UUID eventId() {
    return eventId;
  }

  public Instant timestamp() {
    return timestamp;
  }

  public Map<String, Object> payload() {
    return payload;
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    return (T) payload.get(key);
  }
}
