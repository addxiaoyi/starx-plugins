package io.github.addxiaoyi.starx.velocity.module.skin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.repository.SkinRepository;
import io.github.addxiaoyi.starx.common.skin.NoopSkinRepository;
import io.github.addxiaoyi.starx.common.skin.SkinService;
import io.github.addxiaoyi.starx.common.skin.SkinsRestorerSkinRepository;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;

/** 皮肤桥模块：检测 SkinsRestorer 是否存在并提供皮肤刷新能力。 */
public final class SkinBridgeModule implements VelocityModule {

  private final ProxyServer proxy;
  private final EventBus eventBus;
  private final Supplier<SkinRepository> repositoryFactory;

  private SkinService skinService;
  private volatile boolean skinsRestorerAvailable;

  public SkinBridgeModule(ProxyServer proxy, EventBus eventBus) {
    this(
        proxy,
        eventBus,
        () ->
            proxy.getPluginManager().getPlugin("skinsrestorer").isPresent()
                ? new SkinsRestorerSkinRepository()
                : new NoopSkinRepository());
  }

  SkinBridgeModule(
      ProxyServer proxy, EventBus eventBus, Supplier<SkinRepository> repositoryFactory) {
    this.proxy = Objects.requireNonNull(proxy, "proxy");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.repositoryFactory = Objects.requireNonNull(repositoryFactory, "repositoryFactory");
  }

  @Override
  public String name() {
    return "skin-bridge";
  }

  @Override
  public void onEnable() {
    skinsRestorerAvailable = proxy.getPluginManager().getPlugin("skinsrestorer").isPresent();
    SkinRepository repository = repositoryFactory.get();
    skinService = new SkinService(repository, eventBus);
    registerSkinCommand();
  }

  public boolean isSkinsRestorerAvailable() {
    return skinsRestorerAvailable;
  }

  /** 刷新指定玩家的皮肤。 */
  public void refreshSkin(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid");
    if (skinService == null) {
      return;
    }
    Optional<Player> player = proxy.getPlayer(uuid);
    String name = player.map(Player::getUsername).orElseGet(uuid::toString);
    skinService.refreshSkin(uuid, name);
  }

  private void registerSkinCommand() {
    proxy.getCommandManager().register("skin", new SkinCommand(), "skins");
  }

  private final class SkinCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      CommandSource source = invocation.source();
      source.sendMessage(Component.text("Skin command placeholder. Usage: /skin"));
      if (source instanceof Player player) {
        skinService.refreshSkin(player.getUniqueId(), player.getUsername());
      }
    }
  }
}
