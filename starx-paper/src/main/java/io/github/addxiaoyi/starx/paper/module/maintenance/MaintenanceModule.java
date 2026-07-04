package io.github.addxiaoyi.starx.paper.module.maintenance;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

/** 维护模式后端模块：同步 Velocity 维护状态并拒绝非白名单玩家登录。 */
public final class MaintenanceModule implements PaperModule, Listener {

  private static final String BYPASS_PERMISSION = "starx.maintenance.bypass";

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

  @Override
  public void onPluginMessage(PluginMessage message) {
    if (!PluginMessageChannels.CMD_CONFIG_SYNC.equals(message.command())) {
      return;
    }
    Object maintenance = message.payload().get("maintenance");
    if (maintenance != null) {
      enabled = Boolean.parseBoolean(maintenance.toString());
    }
  }

  @EventHandler
  public void onLogin(PlayerLoginEvent event) {
    if (!enabled) {
      return;
    }
    if (event.getPlayer().hasPermission(BYPASS_PERMISSION)) {
      return;
    }
    event.disallow(
        PlayerLoginEvent.Result.KICK_WHITELIST, "Server is currently under maintenance.");
  }
}
