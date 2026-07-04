package io.github.addxiaoyi.starx.common.auth;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

/** 玩家认证会话。 */
public final class AuthSession {

  public enum State {
    GUEST,
    AUTHENTICATING,
    AUTHENTICATED
  }

  private final UUID uuid;
  private final String username;
  private final InetAddress address;
  private State state;
  private final Instant createdAt;
  private Instant lastActivityAt;

  public AuthSession(UUID uuid, String username, InetAddress address, Instant createdAt) {
    this.uuid = uuid;
    this.username = username;
    this.address = address;
    this.state = State.GUEST;
    this.createdAt = createdAt;
    this.lastActivityAt = createdAt;
  }

  public UUID uuid() {
    return uuid;
  }

  public String username() {
    return username;
  }

  public InetAddress address() {
    return address;
  }

  public State state() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant lastActivityAt() {
    return lastActivityAt;
  }

  public void touch(Instant now) {
    this.lastActivityAt = now;
  }

  public boolean isExpired(Instant now, java.time.Duration timeout) {
    return now.isAfter(lastActivityAt.plus(timeout));
  }
}
