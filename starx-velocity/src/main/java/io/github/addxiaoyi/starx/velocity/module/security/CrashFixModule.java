package io.github.addxiaoyi.starx.velocity.module.security;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Map;
import java.util.Objects;

/** 崩溃修复模块：防止非法包、NBT 溢出、过大数据包导致服务器崩溃。 */
public final class CrashFixModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;

  public CrashFixModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "security.crash";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new PluginMessageListener());
  }

  @Override
  public void onDisable() {}

  /** 检查数据包大小是否超过限制。 */
  boolean checkPacketSize(int size) {
    if (size > config.maxPacketSize()) {
      eventBus.publish(
          new StarxEvent(
              SecurityEvents.CRASH_ATTEMPT,
              Map.of("reason", "oversized_packet", "size", size, "limit", config.maxPacketSize())));
      return true;
    }
    return false;
  }

  /** 检查 NBT 嵌套深度是否溢出。 */
  boolean checkNbtDepth(int depth) {
    if (depth > config.maxNbtDepth()) {
      eventBus.publish(
          new StarxEvent(
              SecurityEvents.CRASH_ATTEMPT,
              Map.of("reason", "nbt_overflow", "depth", depth, "limit", config.maxNbtDepth())));
      return true;
    }
    return false;
  }

  /** 检查数组大小是否合法（防止负数或过大）。 */
  boolean checkArraySize(int size) {
    if (size < 0 || size > config.maxArraySize()) {
      eventBus.publish(
          new StarxEvent(
              SecurityEvents.SUSPICIOUS_PACKET,
              Map.of(
                  "reason", "invalid_array_size", "size", size, "limit", config.maxArraySize())));
      return true;
    }
    return false;
  }

  public interface Config {
    int maxPacketSize();

    int maxNbtDepth();

    int maxArraySize();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public int maxPacketSize() {
          return 1024 * 1024 * 5;
        }

        @Override
        public int maxNbtDepth() {
          return 128;
        }

        @Override
        public int maxArraySize() {
          return 256;
        }
      };
    }
  }

  private final class PluginMessageListener {
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
      byte[] data = event.getData();
      if (data != null) {
        CrashFixModule.this.checkPacketSize(data.length);
      }
    }
  }
}
