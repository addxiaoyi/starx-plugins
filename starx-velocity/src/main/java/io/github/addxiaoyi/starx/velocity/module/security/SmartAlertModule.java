package io.github.addxiaoyi.starx.velocity.module.security;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 智能告警模块 — 事件聚合去重 + 时间窗口抑制 + 分级推送。
 *
 * <p>订阅所有安全事件，对同类事件在时间窗口内聚合去重，仅当超过阈值时才推送告警。 避免"告警风暴"（同一 IP 短时间内触发大量同类事件时只推送一次汇总）。
 *
 * <p>告警分级：
 *
 * <ul>
 *   <li>INFO — 单个事件，首次出现
 *   <li>WARNING — 同类事件 5 分钟内 ≥ 3 次
 *   <li>CRITICAL — 同类事件 5 分钟内 ≥ 10 次
 * </ul>
 */
public final class SmartAlertModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;

  // 事件聚合：key="事件类型:IP"，value=聚合记录
  private final Map<String, AlertBucket> buckets;
  private ScheduledExecutorService cleaner;

  // 聚合窗口和阈值
  private static final long WINDOW_MS = TimeUnit.MINUTES.toMillis(5);
  private static final int WARNING_THRESHOLD = 3;
  private static final int CRITICAL_THRESHOLD = 10;
  private static final long CLEAN_INTERVAL_SEC = 60;

  // 订阅的安全事件类型
  private static final String[] SUBSCRIBED_EVENTS = {
    SecurityEvents.SECURITY_ALERT,
    SecurityEvents.BOT_DETECTED,
    SecurityEvents.CRASH_ATTEMPT,
    SecurityEvents.RISK_HIGH,
    SecurityEvents.RISK_VERIFY_REQUIRED,
    SecurityEvents.RATE_LIMIT_EXCEEDED,
    SecurityEvents.SUSPICIOUS_PACKET,
    SecurityEvents.ANTICHEAT_DETECTION
  };

  public SmartAlertModule(StarxVelocityPlugin plugin, EventBus eventBus) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.buckets = new ConcurrentHashMap<>();
  }

  @Override
  public String name() {
    return "starx.security.smart-alert";
  }

  @Override
  public void onEnable() {
    for (String eventType : SUBSCRIBED_EVENTS) {
      eventBus.subscribe(eventType, this::onSecurityEvent);
    }
    cleaner =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "starx-smart-alert-cleaner");
              t.setDaemon(true);
              return t;
            });
    cleaner.scheduleAtFixedRate(
        this::cleanExpired, CLEAN_INTERVAL_SEC, CLEAN_INTERVAL_SEC, TimeUnit.SECONDS);
    plugin.logger().info("SmartAlert: monitoring " + SUBSCRIBED_EVENTS.length + " event types");
  }

  @Override
  public void onDisable() {
    if (cleaner != null) {
      cleaner.shutdownNow();
    }
  }

  private void onSecurityEvent(StarxEvent event) {
    try {
      String rawIp = event.<String>get("ip");
      final String ip = rawIp != null ? rawIp : "unknown";
      String bucketKey = event.type() + ":" + ip;

      AlertBucket bucket =
          buckets.compute(
              bucketKey,
              (k, v) -> {
                if (v == null || v.isExpired()) {
                  return new AlertBucket(event.type(), ip);
                }
                v.increment();
                return v;
              });

      // 根据累积次数分级推送
      if (bucket.count >= CRITICAL_THRESHOLD && !bucket.criticalEmitted) {
        bucket.criticalEmitted = true;
        plugin
            .logger()
            .log(
                Level.SEVERE,
                "CRITICAL: {0} from {1} ({2} times in {3}m)",
                new Object[] {event.type(), ip, bucket.count, WINDOW_MS / 60000});
        eventBus.publish(
            SecurityEvents.SECURITY_ALERT,
            Map.of(
                "ip",
                ip,
                "type",
                event.type(),
                "severity",
                "CRITICAL",
                "count",
                String.valueOf(bucket.count)));
      } else if (bucket.count >= WARNING_THRESHOLD && !bucket.warningEmitted) {
        bucket.warningEmitted = true;
        plugin
            .logger()
            .log(
                Level.WARNING,
                "WARNING: {0} from {1} ({2} times)",
                new Object[] {event.type(), ip, bucket.count});
      }
    } catch (Exception e) {
      plugin.logger().log(Level.FINE, "SmartAlert: processing failed", e);
    }
  }

  private void cleanExpired() {
    buckets.entrySet().removeIf(e -> e.getValue().isExpired());
  }

  // 测试用
  int getBucketCount() {
    return buckets.size();
  }

  void clearBuckets() {
    buckets.clear();
  }

  /** 单个告警桶 */
  private static final class AlertBucket {
    final String eventType;
    final String ip;
    final long startMs;
    int count;
    boolean warningEmitted;
    boolean criticalEmitted;

    AlertBucket(String eventType, String ip) {
      this.eventType = eventType;
      this.ip = ip;
      this.startMs = System.currentTimeMillis();
      this.count = 1;
    }

    void increment() {
      count++;
    }

    boolean isExpired() {
      return System.currentTimeMillis() - startMs > WINDOW_MS;
    }
  }
}
