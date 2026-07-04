package io.github.addxiaoyi.starx.paper.scheduler;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.Plugin;

/** 封装 Folia 的 GlobalRegionScheduler 与 Bukkit 调度器，根据运行时环境自动回退。 */
public final class SchedulerAdapter {

  private final Plugin plugin;
  private final boolean folia;
  private final Object globalRegionScheduler;
  private final Object asyncScheduler;

  public SchedulerAdapter(Plugin plugin) {
    this(plugin, detectFolia());
  }

  SchedulerAdapter(Plugin plugin, boolean folia) {
    this.plugin = plugin;
    this.folia = folia;
    if (folia) {
      this.globalRegionScheduler = plugin.getServer().getGlobalRegionScheduler();
      this.asyncScheduler = plugin.getServer().getAsyncScheduler();
    } else {
      this.globalRegionScheduler = null;
      this.asyncScheduler = null;
    }
  }

  private static boolean detectFolia() {
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public void runGlobal(Runnable task) {
    if (folia) {
      ((GlobalRegionScheduler) globalRegionScheduler).execute(plugin, task);
    } else {
      plugin.getServer().getScheduler().runTask(plugin, task);
    }
  }

  public void runGlobalDelayed(Runnable task, long delayTicks) {
    if (folia) {
      ((GlobalRegionScheduler) globalRegionScheduler)
          .runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
    } else {
      plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }
  }

  public void runAsync(Runnable task) {
    if (folia) {
      ((io.papermc.paper.threadedregions.scheduler.AsyncScheduler) asyncScheduler)
          .runNow(plugin, scheduledTask -> task.run());
    } else {
      plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }
  }

  public void runAsyncDelayed(Runnable task, long delay, TimeUnit unit) {
    if (folia) {
      ((io.papermc.paper.threadedregions.scheduler.AsyncScheduler) asyncScheduler)
          .runDelayed(plugin, scheduledTask -> task.run(), delay, unit);
    } else {
      plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
    }
  }
}
