package io.github.addxiaoyi.starx.paper.module.skin;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

/** 皮肤模块骨架，检测 SkinsRestorer 是否存在并提供皮肤刷新占位。 */
public final class PaperSkinModule implements PaperModule, Listener {

  private final StarxPaperPlugin plugin;
  private boolean enabled;

  public PaperSkinModule(StarxPaperPlugin plugin) {
    this.plugin = plugin;
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
    enabled = true;
    plugin.getLogger().info("Skin module enabled.");
  }

  /**
   * 刷新指定玩家的皮肤（占位实现）。
   *
   * @param uuid 玩家 UUID
   */
  public void refreshSkin(UUID uuid) {
    if (!enabled) {
      return;
    }
    plugin.getLogger().info("Refreshing skin for " + uuid);
  }
}
