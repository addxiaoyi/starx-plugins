package io.github.addxiaoyi.starx.velocity.module.security;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * 反作弊数据收集模块。
 *
 * <p>通过 Plugin Messaging 接收 Paper 后端上报的检测事件，汇总违规数据并在超过阈值时通过 EventBus 发布安全告警。
 */
public final class AnticheatModule implements VelocityModule {

  private static final Gson GSON = new Gson();

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;
  private final ChannelIdentifier channel;

  private final Map<UUID, PlayerDetectionData> detectionData = new ConcurrentHashMap<>();

  public AnticheatModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
    this.channel = MinecraftChannelIdentifier.create("starx", "anticheat");
  }

  @Override
  public String name() {
    return "security.anticheat";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getChannelRegistrar().register(channel);
    plugin.proxy().getEventManager().register(plugin, new LoginListener());
    plugin.proxy().getEventManager().register(plugin, new ServerConnectedListener());
    plugin.proxy().getEventManager().register(plugin, new PluginMessageListener());
    registerCommand();
  }

  @Override
  public void onDisable() {
    detectionData.clear();
  }

  int getDetectionCount(UUID playerId) {
    PlayerDetectionData data = detectionData.get(playerId);
    return data != null ? data.totalViolations : 0;
  }

  boolean isPlayerTracked(UUID playerId) {
    return detectionData.containsKey(playerId);
  }

  void onLogin(LoginEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    String username = event.getPlayer().getUsername();
    detectionData.putIfAbsent(playerId, new PlayerDetectionData(playerId, username));
  }

  void onServerConnected(ServerConnectedEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    String username = event.getPlayer().getUsername();
    detectionData.putIfAbsent(playerId, new PlayerDetectionData(playerId, username));
  }

  void onPluginMessage(PluginMessageEvent event) {
    if (!event.getIdentifier().equals(channel)) {
      return;
    }

    ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
    String command = in.readUTF();
    int length = in.readInt();
    byte[] payloadBytes = new byte[length];
    in.readFully(payloadBytes);

    @SuppressWarnings("unchecked")
    Map<String, Object> payload =
        GSON.fromJson(new String(payloadBytes, StandardCharsets.UTF_8), Map.class);

    if (!"anticheat:detection".equals(command)) {
      return;
    }

    String playerUuidStr = (String) payload.get("player");
    String checkName = (String) payload.get("check");
    String category = (String) payload.get("category");
    double vl =
        payload.get("vl") instanceof Number ? ((Number) payload.get("vl")).doubleValue() : 0;
    String debug = (String) payload.getOrDefault("debug", "");

    if (playerUuidStr == null || checkName == null) {
      return;
    }

    if (!config.enabledChecks().contains(checkName)) {
      return;
    }

    UUID playerId;
    try {
      playerId = UUID.fromString(playerUuidStr);
    } catch (IllegalArgumentException e) {
      return;
    }

    PlayerDetectionData data =
        detectionData.computeIfAbsent(playerId, k -> new PlayerDetectionData(playerId, "Unknown"));
    data.addViolation(checkName, category, vl, debug);
    data.lastDetectionTime = System.currentTimeMillis();

    if (data.totalViolations >= config.alertThreshold()) {
      eventBus.publish(
          new StarxEvent(
              SecurityEvents.SECURITY_ALERT,
              Map.of(
                  "uuid", playerId,
                  "username", data.username,
                  "type", "anticheat",
                  "check", checkName,
                  "category", category,
                  "totalViolations", data.totalViolations,
                  "detail", debug)));
    }

    eventBus.publish(
        new StarxEvent(
            SecurityEvents.ANTICHEAT_DETECTION,
            Map.of(
                "uuid", playerId,
                "username", data.username,
                "check", checkName,
                "category", category,
                "vl", vl,
                "totalViolations", data.totalViolations,
                "debug", debug)));
  }

  private void registerCommand() {
    CommandManager cmdManager = plugin.proxy().getCommandManager();
    CommandMeta meta = cmdManager.metaBuilder("starx").aliases("sx").plugin(plugin).build();
    cmdManager.register(meta, new AnticheatCommand());
  }

  /**
   * 反作弊配置接口。
   *
   * <p>可通过 YAML 配置项 security.anticheat.* 覆盖默认值。
   */
  public interface Config {
    boolean enabled();

    /** 触发 SECURITY_ALERT 的最低违规次数，默认 5。 */
    int alertThreshold();

    /** 数据收集间隔（毫秒），默认 60000。 */
    long collectIntervalMs();

    /** 启用的检测类型列表。 */
    List<String> enabledChecks();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return true;
        }

        @Override
        public int alertThreshold() {
          return 5;
        }

        @Override
        public long collectIntervalMs() {
          return 60000;
        }

        @Override
        public List<String> enabledChecks() {
          return Arrays.asList(
              "Speed",
              "Fly",
              "KillAura",
              "Reach",
              "NoSlow",
              "Timer",
              "Jesus",
              "AntiKnockback",
              "FastPlace",
              "AutoClicker");
        }
      };
    }
  }

  /** 单个玩家的检测数据。 */
  static final class PlayerDetectionData {
    final UUID playerId;
    final String username;
    int totalViolations;
    long firstDetectionTime;
    long lastDetectionTime;
    final Map<String, Integer> checkViolations = new HashMap<>();
    final List<String> recentDetections = new ArrayList<>();

    PlayerDetectionData(UUID playerId, String username) {
      this.playerId = playerId;
      this.username = username;
      this.firstDetectionTime = System.currentTimeMillis();
    }

    void addViolation(String checkName, String category, double vl, String debug) {
      int roundedVl = (int) Math.max(1, Math.round(vl));
      totalViolations += roundedVl;
      checkViolations.merge(checkName, roundedVl, Integer::sum);
      if (recentDetections.size() >= 20) {
        recentDetections.remove(0);
      }
      recentDetections.add(checkName + "[" + category + "] VL=" + roundedVl);
    }
  }

  private final class LoginListener {
    @Subscribe
    public void onLogin(LoginEvent event) {
      AnticheatModule.this.onLogin(event);
    }
  }

  private final class ServerConnectedListener {
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
      AnticheatModule.this.onServerConnected(event);
    }
  }

  private final class PluginMessageListener {
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
      AnticheatModule.this.onPluginMessage(event);
    }
  }

  private final class AnticheatCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
      CommandSource source = invocation.source();
      String[] args = invocation.arguments();

      if (args.length == 0 || !"anticheat".equalsIgnoreCase(args[0])) {
        return;
      }

      if (args.length == 1) {
        showSummary(source);
        return;
      }

      String subCommand = args[1].toLowerCase();
      switch (subCommand) {
        case "stats":
          showSummary(source);
          break;
        case "player":
          if (args.length >= 3) {
            showPlayer(source, args[2]);
          } else {
            source.sendMessage(
                Component.text("用法: /starx anticheat player <玩家名>", NamedTextColor.RED));
          }
          break;
        case "clear":
          if (args.length >= 3) {
            clearPlayer(source, args[2]);
          } else {
            detectionData.clear();
            source.sendMessage(Component.text("已清除所有反作弊检测数据", NamedTextColor.GREEN));
          }
          break;
        default:
          source.sendMessage(Component.text("未知子命令: " + subCommand, NamedTextColor.RED));
          break;
      }
    }

    private void showSummary(CommandSource source) {
      int totalPlayers = detectionData.size();
      int totalViolations = detectionData.values().stream().mapToInt(d -> d.totalViolations).sum();
      int alertCount =
          (int)
              detectionData.values().stream()
                  .filter(d -> d.totalViolations >= config.alertThreshold())
                  .count();

      source.sendMessage(Component.text("=== 反作弊检测统计 ===", NamedTextColor.GOLD));
      source.sendMessage(Component.text("追踪玩家数: " + totalPlayers, NamedTextColor.YELLOW));
      source.sendMessage(Component.text("总违规次数: " + totalViolations, NamedTextColor.YELLOW));
      source.sendMessage(Component.text("触发告警玩家数: " + alertCount, NamedTextColor.YELLOW));
      source.sendMessage(Component.text("告警阈值: " + config.alertThreshold(), NamedTextColor.GRAY));
      source.sendMessage(
          Component.text(
              "启用检测: " + String.join(", ", config.enabledChecks()), NamedTextColor.GRAY));
    }

    private void showPlayer(CommandSource source, String playerName) {
      PlayerDetectionData found = null;
      for (PlayerDetectionData data : detectionData.values()) {
        if (data.username.equalsIgnoreCase(playerName)) {
          found = data;
          break;
        }
      }

      if (found == null) {
        source.sendMessage(Component.text("未找到玩家 " + playerName + " 的检测数据", NamedTextColor.RED));
        return;
      }

      source.sendMessage(
          Component.text("=== " + found.username + " 检测详情 ===", NamedTextColor.GOLD));
      source.sendMessage(Component.text("总违规次数: " + found.totalViolations, NamedTextColor.YELLOW));
      source.sendMessage(Component.text("检测类型明细:", NamedTextColor.YELLOW));
      for (Map.Entry<String, Integer> entry : found.checkViolations.entrySet()) {
        source.sendMessage(
            Component.text("  " + entry.getKey() + ": " + entry.getValue(), NamedTextColor.GRAY));
      }
      source.sendMessage(Component.text("最近检测:", NamedTextColor.YELLOW));
      for (String detection : found.recentDetections) {
        source.sendMessage(Component.text("  " + detection, NamedTextColor.GRAY));
      }
    }

    private void clearPlayer(CommandSource source, String playerName) {
      PlayerDetectionData found = null;
      for (Map.Entry<UUID, PlayerDetectionData> entry : detectionData.entrySet()) {
        if (entry.getValue().username.equalsIgnoreCase(playerName)) {
          found = entry.getValue();
          detectionData.remove(entry.getKey());
          break;
        }
      }

      if (found == null) {
        source.sendMessage(Component.text("未找到玩家 " + playerName + " 的检测数据", NamedTextColor.RED));
      } else {
        source.sendMessage(Component.text("已清除玩家 " + playerName + " 的检测数据", NamedTextColor.GREEN));
      }
    }
  }
}
