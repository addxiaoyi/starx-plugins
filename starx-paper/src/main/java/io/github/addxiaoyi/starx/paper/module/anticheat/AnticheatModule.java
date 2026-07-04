package io.github.addxiaoyi.starx.paper.module.anticheat;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/** 反作弊检测模块：速度检测、挖掘检测、交互检测，结果通过 Plugin Messaging 上报 Velocity。 */
public final class AnticheatModule implements PaperModule, Listener {

  private static final int VL_THRESHOLD = 10;
  private static final double MAX_SPEED_SURVIVAL = 0.6;
  private static final double MAX_SPEED_CREATIVE = 0.8;

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private final Map<UUID, Integer> violations = new HashMap<>();
  private boolean enabled;
  private List<String> enabledChecks;

  public AnticheatModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  @Override
  public String getName() {
    return "anticheat";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    enabled = configLoader.isModuleEnabled("anticheat");
    enabledChecks = configLoader.getEnabledChecks();
    plugin.getLogger().info("Anticheat module enabled state: " + enabled);
  }

  @Override
  public void onDisable() {
    violations.clear();
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    if (!enabled || !enabledChecks.contains("speed")) {
      return;
    }
    if (event.getPlayer().hasPermission("starx.anticheat.bypass")) {
      return;
    }
    if (event.getFrom().getX() == event.getTo().getX()
        && event.getFrom().getY() == event.getTo().getY()
        && event.getFrom().getZ() == event.getTo().getZ()) {
      return;
    }
    double distance = event.getFrom().distance(event.getTo());
    double maxSpeed =
        event.getPlayer().getGameMode() == GameMode.CREATIVE
            ? MAX_SPEED_CREATIVE
            : MAX_SPEED_SURVIVAL;
    if (distance > maxSpeed) {
      addViolation(event.getPlayer().getUniqueId(), event.getPlayer().getName(), "speed", distance);
    }
  }

  @EventHandler
  public void onBreak(BlockBreakEvent event) {
    if (!enabled || !enabledChecks.contains("break")) {
      return;
    }
    if (event.getPlayer().hasPermission("starx.anticheat.bypass")) {
      return;
    }
    // TODO: 添加更复杂的挖掘检测逻辑（如 reach、nuker 检测）
    // 目前仅做框架占位，记录破坏事件
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (!enabled || !enabledChecks.contains("interact")) {
      return;
    }
    if (event.getPlayer().hasPermission("starx.anticheat.bypass")) {
      return;
    }
    // TODO: 添加交互检测逻辑（如 fastplace、autoclicker 检测）
  }

  private void addViolation(UUID uuid, String playerName, String checkType, double value) {
    int vl = violations.getOrDefault(uuid, 0) + 1;
    violations.put(uuid, vl);
    if (vl >= VL_THRESHOLD) {
      plugin
          .getLogger()
          .warning(
              "Anticheat alert: "
                  + playerName
                  + " triggered "
                  + checkType
                  + " (VL="
                  + vl
                  + ", value="
                  + value
                  + ")");
      // TODO: 通过 Plugin Messaging 向 Velocity 上报检测结果
    }
  }
}