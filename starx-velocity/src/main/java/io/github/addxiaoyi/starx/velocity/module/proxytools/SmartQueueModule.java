package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.addxiaoyi.starx.common.smart.AdaptiveRateLimiter;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.smart.SmartQueueService;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * 智能排队模块 — VIP 优先级 + 活跃度加权 + 动态放行速率。
 *
 * <p>与原有 QueueModule（FIFO）不同，SmartQueueModule 使用优先级队列：
 *
 * <ul>
 *   <li>VIP 玩家（拥有 starx.vip 权限）获得 +500 优先级分，排在普通玩家前面
 *   <li>在线时长越长的玩家获得 +0~100 活跃度分（每在线 1 分钟 +1 分，上限 100）
 *   <li>放行速率根据服务器负载动态调整：低负载 5 人/周期，正常 3 人，中等 2 人，高负载 1 人，临界 0 人
 * </ul>
 *
 * <p>与 QueueModule 互斥：同时只能启用一个排队模块。
 */
public final class SmartQueueModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;
  private final SmartQueueService queueService;
  private final AdaptiveRateLimiter rateLimiter;

  private static final int DEFAULT_CHECK_INTERVAL_MS = 3000;
  private static final int VIP_BASE_SCORE = 500;
  private static final int NORMAL_BASE_SCORE = 100;

  public SmartQueueModule(
      StarxVelocityPlugin plugin, Config config, SmartQueueService queueService) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
    this.queueService = Objects.requireNonNull(queueService, "queueService");
    this.rateLimiter = new AdaptiveRateLimiter(5, 10);
  }

  @Override
  public String name() {
    return "starx.proxytools.smart-queue";
  }

  @Override
  public void onEnable() {
    ProxyServer proxy = plugin.proxy();
    proxy.getEventManager().register(plugin, new SmartQueueListener());
    proxy
        .getScheduler()
        .buildTask(plugin, this::sampleAndProcess)
        .repeat(Duration.ofMillis(config.checkIntervalMillis()))
        .schedule();
    plugin.logger().info("SmartQueue: VIP priority + dynamic release enabled");
  }

  public SmartQueueService queueService() {
    return queueService;
  }

  void onLogin(Player player) {
    queueService.recordJoin(player);
  }

  void onDisconnect(DisconnectEvent event) {
    Player player = event.getPlayer();
    queueService.recordQuit(player);
    player
        .getCurrentServer()
        .ifPresent(
            connection ->
                plugin.proxy().getScheduler().buildTask(plugin, () -> processQueues()).schedule());
  }

  void onKicked(KickedFromServerEvent event) {
    Optional<Component> reason = event.getServerKickReason();
    if (reason.isEmpty() || !isFullReason(reason.get())) {
      return;
    }
    Player player = event.getPlayer();
    int baseScore = isVip(player) ? VIP_BASE_SCORE : NORMAL_BASE_SCORE;
    queueService.enqueue(event.getServer(), player, baseScore);
    event.setResult(KickedFromServerEvent.Notify.create(Component.text(config.queueMessage())));
  }

  void onServerConnected(ServerConnectedEvent event) {
    queueService.removeFromQueue(event.getServer(), event.getPlayer());
  }

  private void sampleAndProcess() {
    sampleMetrics();
    processQueues();
  }

  private void sampleMetrics() {
    Runtime rt = Runtime.getRuntime();
    long used = rt.totalMemory() - rt.freeMemory();
    int memPercent = (int) (used * 100 / rt.maxMemory());
    int playerCount = plugin.proxy().getPlayerCount();
    int estimatedTps = Math.max(5, 20 - (playerCount / 20));
    rateLimiter.updateTps(estimatedTps);
    rateLimiter.updateMemoryPercent(memPercent);
  }

  void processQueues() {
    ProxyServer proxy = plugin.proxy();
    int maxRelease = releaseRate();
    if (maxRelease <= 0) {
      return;
    }
    queueService.processQueues(
        (player, serverName) -> {
          RegisteredServer server = proxy.getServer(serverName).orElse(null);
          if (server == null) {
            return false;
          }
          try {
            return player
                .createConnectionRequest(server)
                .connect()
                .thenApply(result -> result.isSuccessful())
                .exceptionally(ex -> false)
                .join();
          } catch (Exception e) {
            return false;
          }
        },
        maxRelease);
  }

  /** 根据负载等级计算动态放行速率 */
  private int releaseRate() {
    switch (rateLimiter.evaluateLoad()) {
      case LOW:
        return 5;
      case NORMAL:
        return 3;
      case MODERATE:
        return 2;
      case HIGH:
        return 1;
      case CRITICAL:
        return 0;
      default:
        return 3;
    }
  }

  private boolean isVip(Player player) {
    return player.hasPermission("starx.vip");
  }

  private boolean isFullReason(Component reason) {
    String text =
        PlainTextComponentSerializer.plainText().serialize(reason).toLowerCase(Locale.ROOT);
    return config.fullPatterns().stream().anyMatch(text::contains);
  }

  // 测试用
  AdaptiveRateLimiter.LoadLevel getCurrentLoadLevel() {
    return rateLimiter.evaluateLoad();
  }

  int getReleaseRate() {
    return releaseRate();
  }

  public interface Config {
    Set<String> fullPatterns();

    String queueMessage();

    long checkIntervalMillis();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public Set<String> fullPatterns() {
          return Set.of("full", "已满", "is full");
        }

        @Override
        public String queueMessage() {
          return "Server is full, you are queued. VIP players get priority.";
        }

        @Override
        public long checkIntervalMillis() {
          return DEFAULT_CHECK_INTERVAL_MS;
        }
      };
    }
  }

  private final class SmartQueueListener {
    @Subscribe
    public void onKicked(KickedFromServerEvent event) {
      SmartQueueModule.this.onKicked(event);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
      SmartQueueModule.this.onServerConnected(event);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
      SmartQueueModule.this.onDisconnect(event);
    }
  }
}
