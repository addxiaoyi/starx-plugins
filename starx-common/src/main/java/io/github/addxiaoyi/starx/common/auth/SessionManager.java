package io.github.addxiaoyi.starx.common.auth;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** 认证会话管理器。 */
public final class SessionManager {

  private final Duration timeout;
  private final Supplier<Instant> clock;
  private final Map<UUID, AuthSession> sessions = new ConcurrentHashMap<>();

  public SessionManager(Duration timeout, Supplier<Instant> clock) {
    this.timeout = timeout;
    this.clock = clock;
  }

  /**
   * 获取或创建指定玩家的会话。
   *
   * @param uuid 玩家 UUID
   * @param username 玩家名称
   * @param address 玩家地址
   * @return 当前会话
   */
  public AuthSession getOrCreate(UUID uuid, String username, InetAddress address) {
    Instant now = clock.get();
    AuthSession existing = sessions.get(uuid);
    if (existing != null && !existing.isExpired(now, timeout)) {
      existing.touch(now);
      return existing;
    }
    AuthSession session = new AuthSession(uuid, username, address, now);
    sessions.put(uuid, session);
    return session;
  }

  /**
   * 获取会话；若已过期则自动移除。
   *
   * @param uuid 玩家 UUID
   * @return 当前会话，若不存在或已过期则为空
   */
  public Optional<AuthSession> get(UUID uuid) {
    Instant now = clock.get();
    AuthSession session = sessions.get(uuid);
    if (session == null) {
      return Optional.empty();
    }
    if (session.isExpired(now, timeout)) {
      sessions.remove(uuid);
      return Optional.empty();
    }
    return Optional.of(session);
  }

  /**
   * 移除会话。
   *
   * @param uuid 玩家 UUID
   */
  public void remove(UUID uuid) {
    sessions.remove(uuid);
  }
}
