package io.github.addxiaoyi.starx.paper.module.plan;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import io.github.addxiaoyi.starx.paper.scheduler.SchedulerAdapter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** 统计上报模块：定时收集服务器 TPS、玩家活动、区块加载等数据，通过 Plugin Messaging 上报。 */
public final class PlanModule implements PaperModule, Listener {

  private static final long COLLECT_INTERVAL_SECONDS = 60;

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private boolean enabled;
  private Map<String, Object> lastStats;

  public PlanModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  @Override
  public String getName() {
    return "plan";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    enabled = configLoader.isModuleEnabled("plan");
    if (enabled) {
      scheduleStatsCollection();
    }
    plugin.getLogger().info("Plan module enabled state: " + enabled);
  }

  /** 获取最近一次统计数据（测试用）。 */
  public Map<String, Object> getLastStats() {
    return lastStats;
  }

  /** 收集服务器统计数据（测试用）。 */
  public void collectStats() {
    if (!enabled) {
      return;
    }
    lastStats =
        Map.of(
            "onlinePlayers", Bukkit.getOnlinePlayers().size(),
            "maxPlayers", Bukkit.getMaxPlayers(),
            "timestamp", System.currentTimeMillis());
    // TODO: 通过 Plugin Messaging 向 Velocity 上报统计数据
    // TODO: 添加 TPS、区块加载、实体数量等详细统计
  }

  private void scheduleStatsCollection() {
    SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
    scheduler.runAsyncDelayed(
        () -> {
          collectStats();
          scheduleStatsCollection();
        },
        COLLECT_INTERVAL_SECONDS,
        TimeUnit.SECONDS);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!enabled) {
      return;
    }
    // TODO: 记录玩家加入数据
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    if (!enabled) {
      return;
    }
    // TODO: 记录玩家离开数据
  }
}
