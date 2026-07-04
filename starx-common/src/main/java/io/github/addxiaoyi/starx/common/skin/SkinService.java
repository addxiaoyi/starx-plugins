package io.github.addxiaoyi.starx.common.skin;

import io.github.addxiaoyi.starx.api.dto.SkinDto;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.repository.SkinRepository;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** 皮肤业务服务，屏蔽底层存储细节并发布皮肤相关事件。 */
public final class SkinService {

  private final SkinRepository skinRepository;
  private final EventBus eventBus;

  public SkinService(SkinRepository skinRepository, EventBus eventBus) {
    this.skinRepository = Objects.requireNonNull(skinRepository, "skinRepository");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
  }

  /**
   * 刷新指定玩家的皮肤。
   *
   * <p>如果仓库中能找到皮肤数据，则写入并应用；否则发布刷新请求事件，由外部系统补充数据。
   *
   * @param uuid 玩家 UUID
   * @param name 玩家名称
   */
  public void refreshSkin(UUID uuid, String name) {
    Objects.requireNonNull(uuid, "uuid");
    Objects.requireNonNull(name, "name");

    Optional<SkinDto> skin = skinRepository.findByPlayer(uuid, name);
    if (skin.isPresent()) {
      SkinDto data = skin.get();
      if (data.skinId() != null) {
        skinRepository.setSkinId(uuid, data.skinId());
      } else if (data.value() != null && data.signature() != null) {
        skinRepository.setSkinData(uuid, data.value(), data.signature());
      }
      applySkin(uuid);
      notifySkinUpdated(uuid);
      return;
    }

    eventBus.publish(
        EventTypes.SKIN_REFRESH_REQUEST, Map.of("uuid", uuid.toString(), "name", name));
  }

  /**
   * 应用当前仓库中的皮肤。
   *
   * @param uuid 玩家 UUID
   */
  public void applySkin(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid");
    eventBus.publish(EventTypes.SKIN_APPLIED, Map.of("uuid", uuid.toString()));
  }

  /**
   * 通知皮肤已更新。
   *
   * @param uuid 玩家 UUID
   */
  public void notifySkinUpdated(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid");
    eventBus.publish(EventTypes.SKIN_UPDATED, Map.of("uuid", uuid.toString()));
  }
}
