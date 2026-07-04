package io.github.addxiaoyi.starx.velocity.module.skin;

import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.repository.SkinRepository;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 皮肤桥模块：检测 SkinsRestorer 是否存在并提供皮肤刷新能力。 */
public final class SkinBridgeModule implements VelocityModule {

  private final ProxyServer proxy;
  private final SkinRepository skinRepository;
  private final EventBus eventBus;
  private volatile boolean skinsRestorerAvailable;

  public SkinBridgeModule(ProxyServer proxy, SkinRepository skinRepository, EventBus eventBus) {
    this.proxy = Objects.requireNonNull(proxy, "proxy");
    this.skinRepository = skinRepository;
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
  }

  @Override
  public String name() {
    return "skin-bridge";
  }

  @Override
  public void onEnable() {
    skinsRestorerAvailable = proxy.getPluginManager().getPlugin("skinsrestorer").isPresent();
  }

  public boolean isSkinsRestorerAvailable() {
    return skinsRestorerAvailable;
  }

  /** 刷新指定玩家的皮肤（占位实现）。 */
  public void refreshSkin(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid");
    // TODO: 对接 SkinsRestorer API 或 skinRepository
    eventBus.publish(EventTypes.SKIN_REFRESH_REQUEST, Map.of("uuid", uuid.toString()));
  }
}
