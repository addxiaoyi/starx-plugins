package io.github.addxiaoyi.starx.paper.module.qq;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** QQ机器人事件桥模块：监听游戏事件，通过 Plugin Messaging 向 Velocity 发送 QQ 通知事件。 */
public final class QqModule implements PaperModule, Listener {

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private final List<Map<String, Object>> pendingNotifications = new ArrayList<>();
  private boolean enabled;

  public QqModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  @Override
  public String getName() {
    return "qq";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    enabled = configLoader.isModuleEnabled("qq");
    plugin.getLogger().info("QQ module enabled state: " + enabled);
  }

  @Override
  public void onDisable() {
    pendingNotifications.clear();
  }

  /** 获取待发送通知列表（测试用）。 */
  public List<Map<String, Object>> getPendingNotifications() {
    return List.copyOf(pendingNotifications);
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent event) {
    if (!enabled) {
      return;
    }
    String deathMessage = event.getDeathMessage();
    pendingNotifications.add(
        Map.of(
            "type",
            "death",
            "player",
            event.getEntity().getName(),
            "message",
            deathMessage != null ? deathMessage : event.getEntity().getName() + " died"));
    // TODO: 通过 Plugin Messaging 向 Velocity 发送 QQ 通知事件
  }

  @EventHandler
  public void onAdvancement(PlayerAdvancementDoneEvent event) {
    if (!enabled) {
      return;
    }
    pendingNotifications.add(
        Map.of(
            "type", "advancement",
            "player", event.getPlayer().getName(),
            "advancement", event.getAdvancement().getKey().toString()));
    // TODO: 通过 Plugin Messaging 向 Velocity 发送 QQ 通知事件
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!enabled) {
      return;
    }
    pendingNotifications.add(Map.of("type", "join", "player", event.getPlayer().getName()));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    if (!enabled) {
      return;
    }
    pendingNotifications.add(Map.of("type", "quit", "player", event.getPlayer().getName()));
  }
}
