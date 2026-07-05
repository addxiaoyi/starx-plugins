package io.github.addxiaoyi.starx.common.smart;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自适应速率限制器，根据服务器负载动态调整连接/Ping 速率上限。
 *
 * <p>原理：维护一个滑动窗口，跟踪最近 N 秒内的 TPS 和内存使用率。当 TPS 下降或内存升高时 自动收紧限制；当服务器空闲时放宽限制。
 *
 * <p>线程安全，所有操作使用原子变量。
 */
public final class AdaptiveRateLimiter {

  /** 负载等级 */
  public enum LoadLevel {
    LOW, // 低负载：放宽限制 2x
    NORMAL, // 正常：默认限制
    MODERATE, // 中等：收紧 50%
    HIGH, // 高负载：收紧 75%
    CRITICAL // 临界：仅允许 10%
  }

  private final int defaultMaxConnectionsPerSecond;
  private final int defaultMaxPingsPerSecond;

  // 当前负载指标
  private final AtomicInteger currentTps = new AtomicInteger(20);
  private final AtomicInteger currentMemoryPercent = new AtomicInteger(50);
  private final AtomicLong lastUpdateMs = new AtomicLong(System.currentTimeMillis());

  public AdaptiveRateLimiter(int defaultMaxConnectionsPerSecond, int defaultMaxPingsPerSecond) {
    this.defaultMaxConnectionsPerSecond = defaultMaxConnectionsPerSecond;
    this.defaultMaxPingsPerSecond = defaultMaxPingsPerSecond;
  }

  /** 更新 TPS（由外部定时任务调用） */
  public void updateTps(int tps) {
    currentTps.set(Math.max(0, tps));
    lastUpdateMs.set(System.currentTimeMillis());
  }

  /** 更新内存使用率百分比（由外部定时任务调用） */
  public void updateMemoryPercent(int percent) {
    currentMemoryPercent.set(Math.max(0, Math.min(100, percent)));
    lastUpdateMs.set(System.currentTimeMillis());
  }

  /** 评估当前负载等级 */
  public LoadLevel evaluateLoad() {
    int tps = currentTps.get();
    int mem = currentMemoryPercent.get();

    if (tps < 10 || mem > 90) {
      return LoadLevel.CRITICAL;
    }
    if (tps < 15 || mem > 75) {
      return LoadLevel.HIGH;
    }
    if (tps < 18 || mem > 60) {
      return LoadLevel.MODERATE;
    }
    if (tps >= 20 && mem < 50) {
      return LoadLevel.LOW;
    }
    return LoadLevel.NORMAL;
  }

  /** 获取当前允许的最大每秒连接数 */
  public int maxConnectionsPerSecond() {
    return (int) (defaultMaxConnectionsPerSecond * multiplier());
  }

  /** 获取当前允许的最大每秒 Ping 数 */
  public int maxPingsPerSecond() {
    return (int) (defaultMaxPingsPerSecond * multiplier());
  }

  private double multiplier() {
    switch (evaluateLoad()) {
      case LOW:
        return 2.0;
      case NORMAL:
        return 1.0;
      case MODERATE:
        return 0.5;
      case HIGH:
        return 0.25;
      case CRITICAL:
        return 0.1;
      default:
        return 1.0;
    }
  }

  /** 当前是否已过期（超过 30 秒未更新） */
  public boolean isStale() {
    return System.currentTimeMillis() - lastUpdateMs.get() > 30_000;
  }

  // 测试用
  int getCurrentTps() {
    return currentTps.get();
  }

  int getCurrentMemoryPercent() {
    return currentMemoryPercent.get();
  }
}
