package io.github.addxiaoyi.starx.api.messaging;

import java.util.Map;
import java.util.Objects;

/** Velocity 与 Paper 之间传输的消息 envelope。 */
public final class PluginMessage {

  private final String command;
  private final Map<String, Object> payload;

  public PluginMessage(String command, Map<String, Object> payload) {
    this.command = Objects.requireNonNull(command, "command");
    this.payload = payload == null ? Map.of() : Map.copyOf(payload);
  }

  public String command() {
    return command;
  }

  public Map<String, Object> payload() {
    return payload;
  }
}
