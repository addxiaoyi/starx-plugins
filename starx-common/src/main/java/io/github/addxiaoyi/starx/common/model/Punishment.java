package io.github.addxiaoyi.starx.common.model;

import java.util.UUID;

public final class Punishment {

  private final String id;
  private final UUID targetUuid;
  private final String targetName;
  private final String type;
  private final String reason;
  private final UUID staffUuid;
  private final String staffName;
  private final long createdAt;
  private final Long expiresAt;
  private final boolean active;

  public Punishment(
      String id,
      UUID targetUuid,
      String targetName,
      String type,
      String reason,
      UUID staffUuid,
      String staffName,
      long createdAt,
      Long expiresAt,
      boolean active) {
    this.id = id;
    this.targetUuid = targetUuid;
    this.targetName = targetName;
    this.type = type;
    this.reason = reason;
    this.staffUuid = staffUuid;
    this.staffName = staffName;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
    this.active = active;
  }

  public String id() {
    return id;
  }

  public UUID targetUuid() {
    return targetUuid;
  }

  public String targetName() {
    return targetName;
  }

  public String type() {
    return type;
  }

  public String reason() {
    return reason;
  }

  public UUID staffUuid() {
    return staffUuid;
  }

  public String staffName() {
    return staffName;
  }

  public long createdAt() {
    return createdAt;
  }

  public Long expiresAt() {
    return expiresAt;
  }

  public boolean active() {
    return active;
  }
}
