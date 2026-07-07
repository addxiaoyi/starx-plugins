package io.github.addxiaoyi.starx.common.model;

import java.util.UUID;

public final class PlayerBinding {

  private final UUID playerUuid;
  private final String qqId;
  private final String discordId;
  private final long createdAt;

  public PlayerBinding(UUID playerUuid, String qqId, String discordId, long createdAt) {
    this.playerUuid = playerUuid;
    this.qqId = qqId;
    this.discordId = discordId;
    this.createdAt = createdAt;
  }

  public UUID playerUuid() {
    return playerUuid;
  }

  public String qqId() {
    return qqId;
  }

  public String discordId() {
    return discordId;
  }

  public long createdAt() {
    return createdAt;
  }
}
