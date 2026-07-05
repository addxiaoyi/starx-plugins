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
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;

/** 皮肤桥模块：检测 SkinsRestorer 是否存在并提供皮肤刷新能力。 */
public final class SkinBridgeModule implements VelocityModule {

  private static final Logger LOGGER = Logger.getLogger(SkinBridgeModule.class.getName());

  private final ProxyServer proxy;
  private final EventBus eventBus;
  private final Supplier<SkinRepository> repositoryFactory;
  private final String skinProfileBaseUrl;

  private SkinService skinService;
  private volatile boolean skinsRestorerAvailable;

  public SkinBridgeModule(ProxyServer proxy, EventBus eventBus) {
    this.proxy = Objects.requireNonNull(proxy, "proxy");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.skinProfileBaseUrl = null;
    this.repositoryFactory = null;
  }

  public SkinBridgeModule(ProxyServer proxy, EventBus eventBus, String skinProfileBaseUrl) {
    this.proxy = Objects.requireNonNull(proxy, "proxy");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.skinProfileBaseUrl = skinProfileBaseUrl;
    this.repositoryFactory = null;
  }

  SkinBridgeModule(
      ProxyServer proxy, EventBus eventBus, Supplier<SkinRepository> repositoryFactory) {
    this.proxy = Objects.requireNonNull(proxy, "proxy");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.repositoryFactory = Objects.requireNonNull(repositoryFactory, "repositoryFactory");
    this.skinProfileBaseUrl = null;
  }

  @Override
  public String name() {
    return "skin-bridge";
  }

  @Override
  public void onEnable() {
    SkinRepository repository;
    if (skinProfileBaseUrl != null && !skinProfileBaseUrl.isBlank()) {
      repository =
          new WebsiteSkinRepository(
              skinProfileBaseUrl, Logger.getLogger(WebsiteSkinRepository.class.getName()));
      skinsRestorerAvailable = false;
    } else if (repositoryFactory != null) {
      repository = repositoryFactory.get();
      skinsRestorerAvailable = proxy.getPluginManager().getPlugin("skinsrestorer").isPresent();
    } else {
      skinsRestorerAvailable = proxy.getPluginManager().getPlugin("skinsrestorer").isPresent();
      repository =
          skinsRestorerAvailable ? new SkinsRestorerSkinRepository() : new NoopSkinRepository();
    }
    skinService = new SkinService(repository, eventBus);
    registerSkinCommand();
  }

  public boolean isSkinsRestorerAvailable() {
    return skinsRestorerAvailable;
  }

  public boolean isWebsiteSkinAvailable() {
    return skinProfileBaseUrl != null && !skinProfileBaseUrl.isBlank();
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

  /**
   * 从网站皮肤 API 获取纹理并刷新指定玩家的皮肤。
   *
   * @param uuid 玩家 UUID
   * @param playerName 玩家名称
   */
  public void refreshSkinFromWebsite(UUID uuid, String playerName) {
    Objects.requireNonNull(uuid, "uuid");
    Objects.requireNonNull(playerName, "playerName");
    if (skinProfileBaseUrl == null || skinProfileBaseUrl.isBlank()) {
      LOGGER.warning("Website skin base URL is not configured, cannot refresh from website.");
      return;
    }
    if (skinService == null) {
      return;
    }
    skinService.refreshSkin(uuid, playerName);
  }

  private void registerSkinCommand() {
    proxy
        .getCommandManager()
        .register(
            proxy.getCommandManager().metaBuilder("skin").aliases("skins").build(),
            new SkinCommand());
  }

  private final class SkinCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      CommandSource source = invocation.source();
      if (source instanceof Player player) {
        if (skinProfileBaseUrl != null && !skinProfileBaseUrl.isBlank()) {
          source.sendMessage(
              Component.text("Fetching skin from website for " + player.getUsername() + "..."));
          refreshSkinFromWebsite(player.getUniqueId(), player.getUsername());
          source.sendMessage(Component.text("Skin refresh requested from website."));
        } else {
          source.sendMessage(Component.text("Refreshing skin for " + player.getUsername() + "..."));
          skinService.refreshSkin(player.getUniqueId(), player.getUsername());
          source.sendMessage(Component.text("Skin refresh requested."));
        }
      }
    }
  }
}
