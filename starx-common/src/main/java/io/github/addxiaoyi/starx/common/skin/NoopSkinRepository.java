package io.github.addxiaoyi.starx.common.skin;

import io.github.addxiaoyi.starx.api.dto.SkinDto;
import io.github.addxiaoyi.starx.api.repository.SkinRepository;
import java.util.Optional;
import java.util.UUID;

/** SkinsRestorer 不存在时的空实现，保证业务代码无需关心依赖是否可用。 */
public final class NoopSkinRepository implements SkinRepository {

  @Override
  public Optional<SkinDto> findByPlayer(UUID uuid, String name) {
    return Optional.empty();
  }

  @Override
  public void setSkinId(UUID uuid, String skinId) {
    // no-op
  }

  @Override
  public void setSkinData(UUID uuid, String value, String signature) {
    // no-op
  }

  @Override
  public void clearSkin(UUID uuid) {
    // no-op
  }
}
