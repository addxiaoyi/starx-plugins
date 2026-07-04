package io.github.addxiaoyi.starx.velocity.module.integrations;

import com.velocitypowered.api.scheduler.ScheduledTask;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.messaging.VelocityMessageBridge;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Plan 统计集成模块：定时收集代理端统计数据并对外提供查询端点。 */
public final class PlanIntegrationModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final VelocityMessageBridge messageBridge;
  private final Config config;
  private final AtomicBoolean collecting = new AtomicBoolean(false);
  private final List<Map<String, Object>> dataPoints =
      Collections.synchronizedList(new ArrayList<>());
  private ScheduledTask scheduledTask;

  public PlanIntegrationModule(
      StarxVelocityPlugin plugin,
      EventBus eventBus,
      VelocityMessageBridge messageBridge,
      Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.messageBridge = Objects.requireNonNull(messageBridge, "messageBridge");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "integrations.plan";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) {
      return;
    }
    collecting.set(true);
    scheduledTask =
        plugin
            .proxy()
            .getScheduler()
            .buildTask(plugin, this::collectDataPoint)
            .repeat(config.collectIntervalSec(), TimeUnit.SECONDS)
            .schedule();
  }

  @Override
  public void onDisable() {
    collecting.set(false);
    if (scheduledTask != null) {
      scheduledTask.cancel();
      scheduledTask = null;
    }
  }

  public boolean isCollecting() {
    return collecting.get();
  }

  /** 执行一次数据收集。 */
  public void collectDataPoint() {
    int onlinePlayers = plugin.proxy().getPlayerCount();
    Map<String, Object> point = new LinkedHashMap<>();
    point.put("timestamp", Instant.now().toString());
    point.put("online_players", onlinePlayers);
    point.put("server_count", plugin.proxy().getAllServers().size());
    dataPoints.add(point);
    if (dataPoints.size() > config.maxDataPoints()) {
      int excess = dataPoints.size() - config.maxDataPoints();
      if (excess > 0) {
        dataPoints.subList(0, excess).clear();
      }
    }
    // TODO: 通过 Plugin Messaging 从 Paper 后端收集 TPS、内存使用等数据
    // TODO: 使用 eventBus.publish 将数据发送到统计前端
  }

  /** 获取所有收集的数据点（不可变副本）。 */
  public List<Map<String, Object>> getDataPoints() {
    synchronized (dataPoints) {
      return List.copyOf(dataPoints);
    }
  }

  /** 获取当前快照，供 HTTP 端点使用。 */
  public Map<String, Object> getSnapshot() {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("online_players", plugin.proxy().getPlayerCount());
    snapshot.put("data_points", getDataPoints());
    snapshot.put("collect_interval_sec", config.collectIntervalSec());
    // TODO: 添加 TPS、内存使用等更丰富的数据
    return snapshot;
  }

  public interface Config {
    boolean enabled();

    int collectIntervalSec();

    int maxDataPoints();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public int collectIntervalSec() {
          return 60;
        }

        @Override
        public int maxDataPoints() {
          return 10080;
        }
      };
    }
  }
}
