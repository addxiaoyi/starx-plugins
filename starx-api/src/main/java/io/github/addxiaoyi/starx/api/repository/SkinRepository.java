package io.github.addxiaoyi.starx.api.repository;

import io.github.addxiaoyi.starx.api.dto.SkinDto;
import java.util.Optional;
import java.util.UUID;

/** 皮肤仓库契约。抽象 SkinsRestorer 与本地存储，便于测试与降级。 */
public interface SkinRepository {

  Optional<SkinDto> findByPlayer(UUID uuid, String name);

  void setSkinId(UUID uuid, String skinId);

  void setSkinData(UUID uuid, String value, String signature);

  void clearSkin(UUID uuid);
}
