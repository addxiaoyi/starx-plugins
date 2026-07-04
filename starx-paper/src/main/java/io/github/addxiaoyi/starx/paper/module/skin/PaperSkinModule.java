package io.github.addxiaoyi.starx.paper.module.skin;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.repository.SkinRepository;
import io.github.addxiaoyi.starx.common.event.LocalEventBus;
import io.github.addxiaoyi.starx.common.skin.NoopSkinRepository;
import io.github.addxiaoyi.starx.common.skin.SkinService;
import io.github.addxiaoyi.starx.common.skin.SkinsRestorerSkinRepository;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

/** Paper 后端皮肤模块：检测 SkinsRestorer 并使用 {@link SkinService} 应用皮肤。 */
public final class PaperSkinModule implements PaperModule, Listener {

  private final StarxPaperPlugin plugin;
  private final EventBus eventBus;
  private final Supplier<SkinRepository> repositoryFactory;

  private SkinService skinService;
  private boolean enabled;

  public PaperSkinModule(StarxPaperPlugin plugin) {
    this(
        plugin,
        new LocalEventBus(),
        () ->
            Bukkit.getPluginManager().getPlugin("SkinsRestorer") != null
                ? new SkinsRestorerSkinRepository()
                : new NoopSkinRepository());
  }

  PaperSkinModule(
      StarxPaperPlugin plugin, EventBus eventBus, Supplier<SkinRepository> repositoryFactory) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.repositoryFactory = Objects.requireNonNull(repositoryFactory, "repositoryFactory");
  }

  @Override
  public String getName() {
    return "skin";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    if (Bukkit.getPluginManager().getPlugin("SkinsRestorer") == null) {
      plugin.getLogger().warning("SkinsRestorer not found, skin module will stay disabled.");
      return;
    }
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    SkinRepository repository = repositoryFactory.get();
    skinService = new SkinService(repository, eventBus);
    enabled = true;
    plugin.getLogger().info("Skin module enabled.");
  }

  /**
   * 刷新指定玩家的皮肤。
   *
   * @param uuid 玩家 UUID
   * @param name 玩家名称
   */
  public void refreshSkin(UUID uuid, String name) {
    if (!enabled || skinService == null) {
      return;
    }
    skinService.refreshSkin(uuid, name);
  }
}
