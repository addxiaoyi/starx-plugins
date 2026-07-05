package io.github.addxiaoyi.starx.velocity.module.security;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.common.smart.AdaptiveRateLimiter;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 智能限流模块 — 根据服务器 TPS 和内存动态调整连接/Ping 速率限制。
 *
 * <p>每 5 秒采样一次代理端指标，传入 AdaptiveRateLimiter 计算负载等级， 自动调整 maxConnectionsPerSecond 和
 * maxPingsPerSecond。
 *
 * <p>与 BotFilterModule 协同工作：SmartRateLimit 负责动态调参，BotFilter 负责执行拦截。
 */
public final class SmartRateLimitModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final AdaptiveRateLimiter rateLimiter;
  private ScheduledExecutorService scheduler;
  private final Map<String, TimeWindow> pingTracker;
  private final Map<String, TimeWindow> connectionTracker;

  // 默认值，与 BotFilterModule 一致
  private static final int DEFAULT_MAX_CONN = 10;
  private static final int DEFAULT_MAX_PING = 20;
  private static final int SAMPLE_INTERVAL_SEC = 5;

  public SmartRateLimitModule(StarxVelocityPlugin plugin, EventBus eventBus) {
    this(plugin, eventBus, DEFAULT_MAX_CONN, DEFAULT_MAX_PING);
  }

  public SmartRateLimitModule(
      StarxVelocityPlugin plugin, EventBus eventBus, int defaultMaxConn, int defaultMaxPing) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.rateLimiter = new AdaptiveRateLimiter(defaultMaxConn, defaultMaxPing);
    this.pingTracker = new ConcurrentHashMap<>();
    this.connectionTracker = new ConcurrentHashMap<>();
  }

  @Override
  public String name() {
    return "security.smart-rate";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new PingListener());
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "starx-smart-rate");
              t.setDaemon(true);
              return t;
            });
    scheduler.scheduleAtFixedRate(
        this::sampleMetrics, SAMPLE_INTERVAL_SEC, SAMPLE_INTERVAL_SEC, TimeUnit.SECONDS);
    plugin
        .logger()
        .info(
            "SmartRateLimit: started with conn=" + DEFAULT_MAX_CONN + " ping=" + DEFAULT_MAX_PING);
  }

  @Override
  public void onDisable() {
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
    plugin.proxy().getEventManager().unregisterListeners(plugin);
    pingTracker.clear();
    connectionTracker.clear();
  }

  /** 采样当前代理端指标并更新限制器 */
  private void sampleMetrics() {
    try {
      // 使用 JVM 运行时内存作为负载指标
      Runtime rt = Runtime.getRuntime();
      long used = rt.totalMemory() - rt.freeMemory();
      int memPercent = (int) (used * 100 / rt.maxMemory());

      // TPS 估算：通过在线玩家推断（实际应从 Paper 端获取）
      int playerCount = plugin.proxy().getPlayerCount();
      int estimatedTps = Math.max(5, 20 - (playerCount / 20));

      rateLimiter.updateTps(estimatedTps);
      rateLimiter.updateMemoryPercent(memPercent);

      AdaptiveRateLimiter.LoadLevel level = rateLimiter.evaluateLoad();
      if (level != AdaptiveRateLimiter.LoadLevel.NORMAL) {
        plugin
            .logger()
            .fine(
                "SmartRateLimit: load="
                    + level
                    + " tps≈"
                    + estimatedTps
                    + " mem="
                    + memPercent
                    + "%"
                    + " maxConn="
                    + rateLimiter.maxConnectionsPerSecond()
                    + " maxPing="
                    + rateLimiter.maxPingsPerSecond());
      }
    } catch (Exception e) {
      plugin.logger().log(Level.FINE, "SmartRateLimit: metric sampling failed", e);
    }
  }

  /** 检查 Ping 是否超限 */
  private boolean isPingRateLimited(String ip) {
    int max =
        rateLimiter.evaluateLoad() == AdaptiveRateLimiter.LoadLevel.CRITICAL
            ? Math.max(1, rateLimiter.maxPingsPerSecond())
            : rateLimiter.maxPingsPerSecond();
    return track(pingTracker, ip, max);
  }

  /** 检查连接是否超限 */
  private boolean isConnectionRateLimited(String ip) {
    int max = rateLimiter.maxConnectionsPerSecond();
    return track(connectionTracker, ip, max);
  }

  private boolean track(Map<String, TimeWindow> tracker, String ip, int maxPerSecond) {
    long now = System.currentTimeMillis();
    TimeWindow window =
        tracker.compute(
            ip,
            (k, v) -> {
              if (v == null || now - v.startMs > 1000) {
                return new TimeWindow(now);
              }
              v.count++;
              return v;
            });
    return window.count > maxPerSecond;
  }

  /** 获取当前负载等级（测试用） */
  AdaptiveRateLimiter.LoadLevel getCurrentLoadLevel() {
    return rateLimiter.evaluateLoad();
  }

  int getMaxConnections() {
    return rateLimiter.maxConnectionsPerSecond();
  }

  int getMaxPings() {
    return rateLimiter.maxPingsPerSecond();
  }

  private final class PingListener {
    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
      InboundConnection conn = event.getConnection();
      if (conn.getRemoteAddress() instanceof InetSocketAddress addr) {
        String ip = addr.getAddress().getHostAddress();
        if (isPingRateLimited(ip)) {
          event.setPing(
              ServerPing.builder()
                  .description(net.kyori.adventure.text.Component.text("Too many requests"))
                  .build());
          eventBus.publish(
              SecurityEvents.RATE_LIMIT_EXCEEDED,
              Map.of("ip", ip, "type", "ping", "level", rateLimiter.evaluateLoad().name()));
        }
      }
    }
  }

  private static final class TimeWindow {
    final long startMs;
    int count;

    TimeWindow(long startMs) {
      this.startMs = startMs;
      this.count = 1;
    }
  }
}
