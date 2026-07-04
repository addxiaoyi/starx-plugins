package io.github.addxiaoyi.starx.paper.module.mapmod;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** 地图模组同步模块：管理玩家地图数据，通过 Plugin Messaging 同步到 Velocity。 */
public final class MapModModule implements PaperModule, Listener {

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private final Map<String, String> trackedPlayers = new ConcurrentHashMap<>();
  private boolean enabled;

  public MapModModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  @Override
  public String getName() {
    return "mapmod";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    enabled = configLoader.isModuleEnabled("mapmod");
    plugin.getLogger().info("MapMod module enabled state: " + enabled);
  }

  @Override
  public void onDisable() {
    trackedPlayers.clear();
  }

  /** 获取已追踪的玩家列表（测试用）。 */
  public Map<String, String> getTrackedPlayers() {
    return Map.copyOf(trackedPlayers);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!enabled) {
      return;
    }
    String playerName = event.getPlayer().getName();
    String worldName = event.getPlayer().getWorld().getName();
    trackedPlayers.put(playerName, worldName);
    // TODO: 通过 Plugin Messaging 同步地图数据到 Velocity
    plugin.getLogger().info("MapMod: tracking " + playerName + " in world " + worldName);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    if (!enabled) {
      return;
    }
    trackedPlayers.remove(event.getPlayer().getName());
  }
}
