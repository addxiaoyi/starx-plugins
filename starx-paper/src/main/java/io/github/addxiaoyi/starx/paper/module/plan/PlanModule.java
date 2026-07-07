package io.github.addxiaoyi.starx.paper.module.plan;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.messaging.PaperMessageBridge;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import io.github.addxiaoyi.starx.paper.scheduler.SchedulerAdapter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public final class PlanModule implements PaperModule, Listener {

  private static final long COLLECT_INTERVAL_SECONDS = 60;

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private final PaperMessageBridge messageBridge;
  private boolean enabled;
  private Map<String, Object> lastStats;

  public PlanModule(
      StarxPaperPlugin plugin, PaperConfigLoader configLoader, PaperMessageBridge messageBridge) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.configLoader = Objects.requireNonNull(configLoader, "configLoader");
    this.messageBridge = Objects.requireNonNull(messageBridge, "messageBridge");
  }

  @Override
  public String getName() {
    return "starx.plan";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    enabled = configLoader.isModuleEnabled("plan");
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    if (enabled) {
      scheduleStatsCollection();
    }
    plugin.getLogger().info("Plan module enabled state: " + enabled);
  }

  @Override
  public void onDisable() {
    enabled = false;
  }

  @Override
  public void onPluginMessage(PluginMessage message) {
    if (!PluginMessageChannels.CMD_PLAN_STATS.equals(message.command())) {
      return;
    }
    collectAndSend();
  }

  public Map<String, Object> getLastStats() {
    return lastStats;
  }

  public void collectStats() {
    int onlinePlayers = Bukkit.getOnlinePlayers().size();
    int maxPlayers = Bukkit.getMaxPlayers();
    long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long maxMemory = Runtime.getRuntime().maxMemory();
    double tps = 20.0;
    try {
      double[] tpsArr = Bukkit.getTPS();
      if (tpsArr != null && tpsArr.length > 0) {
        tps = tpsArr[0];
      }
    } catch (Exception ignored) {
    }
    int loadedChunks = 0;
    int entities = 0;
    for (World world : Bukkit.getWorlds()) {
      loadedChunks += world.getLoadedChunks().length;
      entities += world.getEntities().size();
    }
    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("timestamp", Instant.now().toString());
    stats.put("onlinePlayers", onlinePlayers);
    stats.put("maxPlayers", maxPlayers);
    stats.put("tps", Math.round(tps * 100.0) / 100.0);
    stats.put("usedMemory", usedMemory);
    stats.put("maxMemory", maxMemory);
    stats.put("loadedChunks", loadedChunks);
    stats.put("entities", entities);
    lastStats = stats;
  }

  public void collectAndSend() {
    if (!enabled) {
      return;
    }
    try {
      collectStats();
    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "Failed to collect server stats", e);
      return;
    }
    Player anyOnline = null;
    for (Player player : Bukkit.getOnlinePlayers()) {
      anyOnline = player;
      break;
    }
    if (anyOnline == null) {
      return;
    }
    messageBridge.send(
        anyOnline, new PluginMessage(PluginMessageChannels.CMD_PLAN_STATS, lastStats));
  }

  private void scheduleStatsCollection() {
    SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
    scheduler.runAsyncDelayed(
        () -> {
          if (!enabled) {
            return;
          }
          collectAndSend();
          scheduleStatsCollection();
        },
        COLLECT_INTERVAL_SECONDS,
        TimeUnit.SECONDS);
  }
}
