package io.github.addxiaoyi.starx.api.dto;

import java.util.Objects;
import java.util.UUID;

/** 皮肤数据传输对象。用于跨模块和网站后端交换皮肤信息。 */
public final class SkinDto {

  private final UUID ownerUuid;
  private final String ownerName;
  private final String skinId;
  private final String value;
  private final String signature;
  private final String textureUrl;

  public SkinDto(
      UUID ownerUuid,
      String ownerName,
      String skinId,
      String value,
      String signature,
      String textureUrl) {
    this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid");
    this.ownerName = Objects.requireNonNull(ownerName, "ownerName");
    this.skinId = skinId;
    this.value = value;
    this.signature = signature;
    this.textureUrl = textureUrl;
  }

  public UUID ownerUuid() {
    return ownerUuid;
  }

  public String ownerName() {
    return ownerName;
  }

  public String skinId() {
    return skinId;
  }

  public String value() {
    return value;
  }

  public String signature() {
    return signature;
  }

  public String textureUrl() {
    return textureUrl;
  }
}
