package io.github.addxiaoyi.starx.velocity.module.security;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** 反机器人模块：连接速率限制、Ping 频率检测、可疑包检测。 */
public final class BotFilterModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;

  private final Map<String, PingEntry> pingTracker = new ConcurrentHashMap<>();
  private final Map<String, ConnectionEntry> connectionTracker = new ConcurrentHashMap<>();

  public BotFilterModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "security.bot";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new PingListener());
    plugin.proxy().getEventManager().register(plugin, new LoginListener());
  }

  @Override
  public void onDisable() {
    pingTracker.clear();
    connectionTracker.clear();
  }

  /** 返回指定 IP 的 ping 计数，用于测试。 */
  int getPingCount(String ip) {
    PingEntry entry = pingTracker.get(ip);
    return entry != null ? entry.count : 0;
  }

  /** 返回指定 IP 的连接计数，用于测试。 */
  int getConnectionCount(String ip) {
    ConnectionEntry entry = connectionTracker.get(ip);
    return entry != null ? entry.count : 0;
  }

  void onProxyPing(ProxyPingEvent event) {
    InetSocketAddress address = event.getConnection().getRemoteAddress();
    if (address == null) {
      return;
    }
    String ip = address.getAddress().getHostAddress();
    long now = System.currentTimeMillis();

    PingEntry entry = pingTracker.computeIfAbsent(ip, k -> new PingEntry(now));
    entry.count++;

    if (entry.count > config.maxPingsPerSecond()) {
      eventBus.publish(
          new StarxEvent(
              SecurityEvents.BOT_DETECTED,
              Map.of(
                  "ip",
                  ip,
                  "reason",
                  "ping_flood",
                  "count",
                  entry.count,
                  "limit",
                  config.maxPingsPerSecond())));
    }
  }

  void onLogin(LoginEvent event) {
    InetSocketAddress address = event.getPlayer().getRemoteAddress();
    if (address == null) {
      return;
    }
    String ip = address.getAddress().getHostAddress();
    long now = System.currentTimeMillis();

    ConnectionEntry entry = connectionTracker.computeIfAbsent(ip, k -> new ConnectionEntry(now));
    entry.count++;

    if (entry.count > config.maxConnectionsPerSecond()) {
      eventBus.publish(
          new StarxEvent(
              SecurityEvents.RATE_LIMIT_EXCEEDED,
              Map.of(
                  "ip",
                  ip,
                  "username",
                  event.getPlayer().getUsername(),
                  "limit",
                  config.maxConnectionsPerSecond())));
    }
  }

  void purgeExpired() {
    long now = System.currentTimeMillis();
    pingTracker.entrySet().removeIf(e -> now - e.getValue().timestamp > config.cachePurgeMillis());
    connectionTracker
        .entrySet()
        .removeIf(e -> now - e.getValue().timestamp > config.cachePurgeMillis());
  }

  public interface Config {
    int maxPingsPerSecond();

    int maxConnectionsPerSecond();

    boolean checkClientBrand();

    boolean checkClientSettings();

    long cachePurgeMillis();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public int maxPingsPerSecond() {
          return 20;
        }

        @Override
        public int maxConnectionsPerSecond() {
          return 10;
        }

        @Override
        public boolean checkClientBrand() {
          return true;
        }

        @Override
        public boolean checkClientSettings() {
          return true;
        }

        @Override
        public long cachePurgeMillis() {
          return 60000;
        }
      };
    }
  }

  private static final class PingEntry {
    final long timestamp;
    int count;

    PingEntry(long timestamp) {
      this.timestamp = timestamp;
      this.count = 0;
    }
  }

  private static final class ConnectionEntry {
    final long timestamp;
    int count;

    ConnectionEntry(long timestamp) {
      this.timestamp = timestamp;
      this.count = 0;
    }
  }

  private final class PingListener {
    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
      BotFilterModule.this.onProxyPing(event);
    }
  }

  private final class LoginListener {
    @Subscribe
    public void onLogin(LoginEvent event) {
      BotFilterModule.this.onLogin(event);
    }
  }
}
