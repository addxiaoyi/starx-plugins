package io.github.addxiaoyi.starx.paper.module.maintenance;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

/** 维护模式模块骨架，监听 Paper 登录事件并可取消。 */
public final class MaintenanceModule implements PaperModule, Listener {

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private boolean enabled;

  public MaintenanceModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  @Override
  public String getName() {
    return "maintenance";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    enabled = configLoader.isModuleEnabled("maintenance");
    plugin.getLogger().info("Maintenance module enabled state: " + enabled);
  }

  @EventHandler
  public void onLogin(PlayerLoginEvent event) {
    if (!enabled) {
      return;
    }
    if (event.getPlayer().hasPermission("starx.maintenance.bypass")) {
      return;
    }
    event.disallow(
        PlayerLoginEvent.Result.KICK_WHITELIST, "Server is currently under maintenance.");
  }
}
