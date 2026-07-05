package io.github.addxiaoyi.starx.common.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 密码爆破防护：基于 UUID 的失败尝试计数 + 递增延迟。
 *
 * <ul>
 *   <li>前 5 次失败：递增延迟 1s → 2s → 4s → 8s → 16s
 *   <li>第 6 次起：锁定 5 分钟
 *   <li>15 分钟内无新失败则自动重置计数
 * </ul>
 */
public final class BruteForceProtector {

  private static final int MAX_ATTEMPTS = 5;
  private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000;
  private static final long RESET_WINDOW_MS = 15 * 60 * 1000;

  private final Map<UUID, FailEntry> attempts = new ConcurrentHashMap<>();

  /** 检查是否被锁定，未锁定则返回需要等待的延迟（毫秒），-1 表示已锁定。 */
  public BruteForceStatus check(UUID uuid) {
    FailEntry entry = attempts.get(uuid);
    if (entry == null) {
      return BruteForceStatus.ALLOWED;
    }

    long now = System.currentTimeMillis();

    if (now - entry.firstFailMs > RESET_WINDOW_MS) {
      attempts.remove(uuid);
      return BruteForceStatus.ALLOWED;
    }

    if (entry.count >= MAX_ATTEMPTS) {
      long remainingMs = LOCKOUT_DURATION_MS - (now - entry.lastFailMs);
      if (remainingMs > 0) {
        return BruteForceStatus.locked(remainingMs);
      }
      attempts.remove(uuid);
      return BruteForceStatus.ALLOWED;
    }

    long delayMs = (1L << (entry.count - 1)) * 1000L;
    long elapsed = now - entry.lastFailMs;
    if (elapsed < delayMs) {
      return BruteForceStatus.delayed(delayMs - elapsed);
    }
    return BruteForceStatus.ALLOWED;
  }

  /** 记录一次失败尝试。 */
  public void recordFailure(UUID uuid) {
    long now = System.currentTimeMillis();
    attempts.compute(
        uuid,
        (k, entry) -> {
          if (entry == null || now - entry.firstFailMs > RESET_WINDOW_MS) {
            return new FailEntry(1, now, now);
          }
          return new FailEntry(entry.count + 1, entry.firstFailMs, now);
        });
  }

  /** 登录成功后清除失败记录。 */
  public void clear(UUID uuid) {
    attempts.remove(uuid);
  }

  public int getAttemptCount(UUID uuid) {
    FailEntry entry = attempts.get(uuid);
    return entry == null ? 0 : entry.count;
  }

  private static final class FailEntry {
    final int count;
    final long firstFailMs;
    final long lastFailMs;

    FailEntry(int count, long firstFailMs, long lastFailMs) {
      this.count = count;
      this.firstFailMs = firstFailMs;
      this.lastFailMs = lastFailMs;
    }
  }

  public enum BruteForceStatus {
    ALLOWED,
    DELAYED,
    LOCKED;

    private long waitMs;

    BruteForceStatus() {
      this.waitMs = 0;
    }

    static BruteForceStatus delayed(long waitMs) {
      BruteForceStatus s = DELAYED;
      s.waitMs = waitMs;
      return s;
    }

    static BruteForceStatus locked(long waitMs) {
      BruteForceStatus s = LOCKED;
      s.waitMs = waitMs;
      return s;
    }

    public long waitMs() {
      return waitMs;
    }
  }
}
