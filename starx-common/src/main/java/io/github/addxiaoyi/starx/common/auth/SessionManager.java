package io.github.addxiaoyi.starx.common.auth;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 认证会话管理器，带后台过期清理与会话数上限。 */
public final class SessionManager {

  private static final int DEFAULT_MAX_SESSIONS = 10_000;
  private static final int INITIAL_CAPACITY = 1024;
  private static final long CLEANUP_INTERVAL_SECONDS = 30;

  private final Duration timeout;
  private final Supplier<Instant> clock;
  private final int maxSessions;
  private final Map<UUID, AuthSession> sessions;
  private final ScheduledExecutorService cleanupExecutor;

  public SessionManager(Duration timeout, Supplier<Instant> clock) {
    this(timeout, clock, DEFAULT_MAX_SESSIONS);
  }

  public SessionManager(Duration timeout, Supplier<Instant> clock, int maxSessions) {
    this.timeout = timeout;
    this.clock = clock;
    this.maxSessions = maxSessions;
    this.sessions = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    this.cleanupExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "starx-session-cleanup");
              t.setDaemon(true);
              return t;
            });
    cleanupExecutor.scheduleWithFixedDelay(
        this::cleanup, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  public AuthSession getOrCreate(UUID uuid, String username, InetAddress address) {
    return sessions.compute(
        uuid,
        (k, existing) -> {
          Instant now = clock.get();
          if (existing != null && !existing.isExpired(now, timeout)) {
            existing.touch(now);
            return existing;
          }
          if (sessions.size() >= maxSessions && existing == null) {
            return null;
          }
          return new AuthSession(uuid, username, address, now);
        });
  }

  public Optional<AuthSession> get(UUID uuid) {
    return Optional.ofNullable(
        sessions.computeIfPresent(
            uuid,
            (k, session) -> {
              Instant now = clock.get();
              if (session.isExpired(now, timeout)) {
                return null;
              }
              return session;
            }));
  }

  public void remove(UUID uuid) {
    sessions.remove(uuid);
  }

  public int size() {
    return sessions.size();
  }

  public void shutdown() {
    cleanupExecutor.shutdown();
    sessions.clear();
  }

  private void cleanup() {
    Instant now = clock.get();
    sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now, timeout));
  }
}
