package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * 增强代理命令模块：提供 /glist, /find, /send, /alert, /ping, /kickall 命令。
 *
 * <p>配置键：proxytools.enhanced。
 */
public final class EnhancedProxyModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;

  private final GListCommand glistCommand;
  private final FindCommand findCommand;
  private final SendCommand sendCommand;
  private final AlertCommand alertCommand;
  private final PingCommand pingCommand;
  private final KickAllCommand kickAllCommand;

  public EnhancedProxyModule(StarxVelocityPlugin plugin, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
    this.glistCommand = new GListCommand();
    this.findCommand = new FindCommand();
    this.sendCommand = new SendCommand();
    this.alertCommand = new AlertCommand();
    this.pingCommand = new PingCommand();
    this.kickAllCommand = new KickAllCommand();
  }

  @Override
  public String name() {
    return "enhanced";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) {
      return;
    }
    ProxyServer proxy = plugin.proxy();
    proxy
        .getCommandManager()
        .register(proxy.getCommandManager().metaBuilder("glist").build(), glistCommand);
    proxy
        .getCommandManager()
        .register(proxy.getCommandManager().metaBuilder("find").build(), findCommand);
    proxy
        .getCommandManager()
        .register(proxy.getCommandManager().metaBuilder("send").build(), sendCommand);
    proxy
        .getCommandManager()
        .register(proxy.getCommandManager().metaBuilder("alert").build(), alertCommand);
    proxy
        .getCommandManager()
        .register(proxy.getCommandManager().metaBuilder("ping").build(), pingCommand);
    proxy
        .getCommandManager()
        .register(proxy.getCommandManager().metaBuilder("kickall").build(), kickAllCommand);
  }

  @Override
  public void onDisable() {}

  SimpleCommand getGlistCommand() {
    return glistCommand;
  }

  SimpleCommand getFindCommand() {
    return findCommand;
  }

  SimpleCommand getSendCommand() {
    return sendCommand;
  }

  SimpleCommand getAlertCommand() {
    return alertCommand;
  }

  SimpleCommand getPingCommand() {
    return pingCommand;
  }

  SimpleCommand getKickAllCommand() {
    return kickAllCommand;
  }

  // ---- Command implementations ----

  /** /glist - 全局服务器列表 */
  private final class GListCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (!invocation.source().hasPermission("starx.commands.glist")) {
        invocation
            .source()
            .sendMessage(
                Component.text(
                    "You do not have permission to use this command.", NamedTextColor.RED));
        return;
      }

      ProxyServer proxy = plugin.proxy();
      Collection<RegisteredServer> servers = proxy.getAllServers();
      int totalPlayers = proxy.getPlayerCount();

      invocation
          .source()
          .sendMessage(
              Component.text("==== Global Server List (", NamedTextColor.GOLD)
                  .decoration(TextDecoration.BOLD, true)
                  .append(Component.text(totalPlayers, NamedTextColor.GREEN))
                  .append(Component.text(" players) ====", NamedTextColor.GOLD)));

      for (RegisteredServer server : servers) {
        String serverName = server.getServerInfo().getName();
        Collection<Player> players = server.getPlayersConnected();
        int playerCount = players.size();

        // TODO: Support summarized servers, hidden servers, vanish player filtering
        // TODO: Support progress bar display with configurable style
        // TODO: Support server displayname from config

        NamedTextColor color = playerCount > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY;
        String playerList =
            players.isEmpty()
                ? "No players"
                : players.stream().map(Player::getUsername).collect(Collectors.joining(", "));

        invocation
            .source()
            .sendMessage(
                Component.text("  " + serverName + ": ", NamedTextColor.WHITE)
                    .append(Component.text("(" + playerCount + ") ", color))
                    .append(Component.text(playerList, NamedTextColor.GRAY)));
      }
    }
  }

  /** /find <玩家> - 查找玩家所在服务器 */
  private final class FindCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (!invocation.source().hasPermission("starx.commands.find")) {
        invocation
            .source()
            .sendMessage(
                Component.text(
                    "You do not have permission to use this command.", NamedTextColor.RED));
        return;
      }

      String[] args = invocation.arguments();
      if (args.length == 0) {
        invocation
            .source()
            .sendMessage(Component.text("Usage: /find <player>", NamedTextColor.YELLOW));
        return;
      }

      String targetName = args[0];
      Optional<Player> targetOpt = plugin.proxy().getPlayer(targetName);

      if (targetOpt.isEmpty()) {
        invocation
            .source()
            .sendMessage(
                Component.text("Player not found: ", NamedTextColor.RED)
                    .append(Component.text(targetName, NamedTextColor.WHITE)));
        return;
      }

      Player target = targetOpt.get();
      // TODO: Support vanish detection and filtering
      Optional<com.velocitypowered.api.proxy.ServerConnection> serverConn =
          target.getCurrentServer();

      if (serverConn.isEmpty()) {
        invocation
            .source()
            .sendMessage(
                Component.text("Player ", NamedTextColor.YELLOW)
                    .append(Component.text(target.getUsername(), NamedTextColor.GREEN))
                    .append(
                        Component.text(" is not connected to any server.", NamedTextColor.YELLOW)));
        return;
      }

      String serverName = serverConn.get().getServerInfo().getName();
      invocation
          .source()
          .sendMessage(
              Component.text("Player ", NamedTextColor.GOLD)
                  .append(Component.text(target.getUsername(), NamedTextColor.GREEN))
                  .append(Component.text(" is on ", NamedTextColor.GOLD))
                  .append(Component.text(serverName, NamedTextColor.AQUA)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
      String lastArg =
          invocation.arguments().length > 0
              ? invocation.arguments()[invocation.arguments().length - 1]
              : "";
      return plugin.proxy().getAllPlayers().stream()
          .map(Player::getUsername)
          .filter(name -> name.toLowerCase().startsWith(lastArg.toLowerCase()))
          .sorted()
          .collect(Collectors.toList());
    }
  }

  /** /send <玩家> <服务器> - 发送玩家到指定服务器 */
  private final class SendCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (!invocation.source().hasPermission("starx.commands.send")) {
        invocation
            .source()
            .sendMessage(
                Component.text(
                    "You do not have permission to use this command.", NamedTextColor.RED));
        return;
      }

      String[] args = invocation.arguments();
      if (args.length < 2) {
        invocation
            .source()
            .sendMessage(Component.text("Usage: /send <player> <server>", NamedTextColor.YELLOW));
        return;
      }

      String playerName = args[0];
      String serverName = args[1];

      Optional<Player> targetOpt = plugin.proxy().getPlayer(playerName);
      if (targetOpt.isEmpty()) {
        invocation
            .source()
            .sendMessage(
                Component.text("Player not found: ", NamedTextColor.RED)
                    .append(Component.text(playerName, NamedTextColor.WHITE)));
        return;
      }

      Optional<RegisteredServer> serverOpt = plugin.proxy().getServer(serverName);
      if (serverOpt.isEmpty()) {
        invocation
            .source()
            .sendMessage(
                Component.text("Server not found: ", NamedTextColor.RED)
                    .append(Component.text(serverName, NamedTextColor.WHITE)));
        return;
      }

      Player target = targetOpt.get();
      RegisteredServer server = serverOpt.get();
      target.createConnectionRequest(server).fireAndForget();

      invocation
          .source()
          .sendMessage(
              Component.text("Sent ", NamedTextColor.GREEN)
                  .append(Component.text(target.getUsername(), NamedTextColor.AQUA))
                  .append(Component.text(" to ", NamedTextColor.GREEN))
                  .append(Component.text(server.getServerInfo().getName(), NamedTextColor.AQUA)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
      String[] args = invocation.arguments();
      if (args.length <= 1) {
        String lastArg = args.length == 0 ? "" : args[0];
        return plugin.proxy().getAllPlayers().stream()
            .map(Player::getUsername)
            .filter(name -> name.toLowerCase().startsWith(lastArg.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
      }
      String lastArg = args[args.length - 1];
      return plugin.proxy().getAllServers().stream()
          .map(s -> s.getServerInfo().getName())
          .filter(name -> name.toLowerCase().startsWith(lastArg.toLowerCase()))
          .sorted()
          .collect(Collectors.toList());
    }
  }

  /** /alert <消息> - 全服广播 */
  private final class AlertCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (!invocation.source().hasPermission("starx.commands.alert")) {
        invocation
            .source()
            .sendMessage(
                Component.text(
                    "You do not have permission to use this command.", NamedTextColor.RED));
        return;
      }

      String[] args = invocation.arguments();
      if (args.length == 0) {
        invocation
            .source()
            .sendMessage(Component.text("Usage: /alert <message>", NamedTextColor.YELLOW));
        return;
      }

      String message = String.join(" ", args);
      Component alertMessage =
          Component.text("[Alert] ", NamedTextColor.RED)
              .decoration(TextDecoration.BOLD, true)
              .append(Component.text(message, NamedTextColor.GOLD));

      for (RegisteredServer server : plugin.proxy().getAllServers()) {
        for (Player player : server.getPlayersConnected()) {
          player.sendMessage(alertMessage);
        }
      }
    }
  }

  /** /ping [玩家] - 查看延迟 */
  private final class PingCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (!invocation.source().hasPermission("starx.commands.ping")) {
        invocation
            .source()
            .sendMessage(
                Component.text(
                    "You do not have permission to use this command.", NamedTextColor.RED));
        return;
      }

      String[] args = invocation.arguments();

      if (args.length > 0) {
        String targetName = args[0];
        Optional<Player> targetOpt = plugin.proxy().getPlayer(targetName);

        if (targetOpt.isEmpty()) {
          invocation
              .source()
              .sendMessage(
                  Component.text("Player not found: ", NamedTextColor.RED)
                      .append(Component.text(targetName, NamedTextColor.WHITE)));
          return;
        }

        Player target = targetOpt.get();
        // TODO: Support vanish detection for target ping
        invocation
            .source()
            .sendMessage(
                Component.text(target.getUsername(), NamedTextColor.AQUA)
                    .append(Component.text("'s ping: ", NamedTextColor.GOLD))
                    .append(Component.text(target.getPing() + "ms", NamedTextColor.GREEN)));
        return;
      }

      if (!(invocation.source() instanceof Player)) {
        invocation
            .source()
            .sendMessage(
                Component.text(
                    "Only players can use this command without arguments.", NamedTextColor.RED));
        return;
      }

      Player self = (Player) invocation.source();
      invocation
          .source()
          .sendMessage(
              Component.text("Your ping: ", NamedTextColor.GOLD)
                  .append(Component.text(self.getPing() + "ms", NamedTextColor.GREEN)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
      String lastArg =
          invocation.arguments().length > 0
              ? invocation.arguments()[invocation.arguments().length - 1]
              : "";
      return plugin.proxy().getAllPlayers().stream()
          .map(Player::getUsername)
          .filter(name -> name.toLowerCase().startsWith(lastArg.toLowerCase()))
          .sorted()
          .collect(Collectors.toList());
    }
  }

  /** /kickall <服务器> [原因] - 批量踢出 */
  private final class KickAllCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (!invocation.source().hasPermission("starx.commands.kickall")) {
        invocation
            .source()
            .sendMessage(
                Component.text(
                    "You do not have permission to use this command.", NamedTextColor.RED));
        return;
      }

      String[] args = invocation.arguments();
      if (args.length == 0) {
        invocation
            .source()
            .sendMessage(
                Component.text("Usage: /kickall <server> [reason]", NamedTextColor.YELLOW));
        return;
      }

      String serverName = args[0];
      ProxyServer proxy = plugin.proxy();
      Optional<RegisteredServer> targetServerOpt = proxy.getServer(serverName);

      if (targetServerOpt.isEmpty()) {
        invocation
            .source()
            .sendMessage(
                Component.text("Server not found: ", NamedTextColor.RED)
                    .append(Component.text(serverName, NamedTextColor.WHITE)));
        return;
      }

      RegisteredServer targetServer = targetServerOpt.get();
      Collection<RegisteredServer> allServers = proxy.getAllServers();
      RegisteredServer fallback =
          allServers.stream()
              .filter(
                  s -> !s.getServerInfo().getName().equals(targetServer.getServerInfo().getName()))
              .findFirst()
              .orElse(targetServer);

      int kicked = 0;
      for (Player player : targetServer.getPlayersConnected()) {
        if (player.hasPermission("starx.kickall.bypass")) {
          continue;
        }
        player.createConnectionRequest(fallback).fireAndForget();
        kicked++;
      }

      // TODO: Support reason parameter for kick message
      invocation
          .source()
          .sendMessage(
              Component.text("Kicked ", NamedTextColor.GREEN)
                  .append(Component.text(kicked, NamedTextColor.AQUA))
                  .append(Component.text(" players from ", NamedTextColor.GREEN))
                  .append(
                      Component.text(targetServer.getServerInfo().getName(), NamedTextColor.AQUA)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
      String lastArg =
          invocation.arguments().length > 0
              ? invocation.arguments()[invocation.arguments().length - 1]
              : "";
      return plugin.proxy().getAllServers().stream()
          .map(s -> s.getServerInfo().getName())
          .filter(name -> name.toLowerCase().startsWith(lastArg.toLowerCase()))
          .sorted()
          .collect(Collectors.toList());
    }
  }

  // ---- Config ----

  /** 模块配置 */
  public interface Config {
    boolean enabled();

    /** 服务器显示配置 */
    ServerDisplayConfig servers();

    /** 进度条样式配置 */
    ProgressBarConfig progressBar();

    /** 隐身设置 */
    VanishConfig vanish();

    static Config simpleDefault() {
      return new Config() {
        @Override
        public boolean enabled() {
          return true;
        }

        @Override
        public ServerDisplayConfig servers() {
          return ServerDisplayConfig.defaultConfig();
        }

        @Override
        public ProgressBarConfig progressBar() {
          return ProgressBarConfig.defaultConfig();
        }

        @Override
        public VanishConfig vanish() {
          return VanishConfig.defaultConfig();
        }
      };
    }

    static Config disabled() {
      return new Config() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public ServerDisplayConfig servers() {
          return ServerDisplayConfig.defaultConfig();
        }

        @Override
        public ProgressBarConfig progressBar() {
          return ProgressBarConfig.defaultConfig();
        }

        @Override
        public VanishConfig vanish() {
          return VanishConfig.defaultConfig();
        }
      };
    }
  }

  /** 服务器显示配置 */
  public interface ServerDisplayConfig {
    /** 隐藏的服务器列表 */
    List<String> hiddenServers();

    /** 合并的服务器组（key=显示名, value=合并的服务器名列表） */
    Map<String, List<String>> summarizedServers();

    /** 服务器显示名称映射 */
    Map<String, String> displayNames();

    static ServerDisplayConfig defaultConfig() {
      return new ServerDisplayConfig() {
        @Override
        public List<String> hiddenServers() {
          return List.of();
        }

        @Override
        public Map<String, List<String>> summarizedServers() {
          return Map.of();
        }

        @Override
        public Map<String, String> displayNames() {
          return Map.of();
        }
      };
    }
  }

  /** 进度条样式配置 */
  public interface ProgressBarConfig {
    /** 进度条总长度 */
    int count();

    /** 已完成字符 */
    String complete();

    /** 未完成字符 */
    String notComplete();

    static ProgressBarConfig defaultConfig() {
      return new ProgressBarConfig() {
        @Override
        public int count() {
          return 45;
        }

        @Override
        public String complete() {
          return "|";
        }

        @Override
        public String notComplete() {
          return ".";
        }
      };
    }
  }

  /** 隐身设置配置 */
  public interface VanishConfig {
    /** 是否启用隐身过滤 */
    boolean enabled();

    /** 隐身玩家装饰 */
    String playerDecoration();

    /** 有隐身玩家的服务器装饰 */
    String serverDecoration();

    static VanishConfig defaultConfig() {
      return new VanishConfig() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public String playerDecoration() {
          return "&o$player";
        }

        @Override
        public String serverDecoration() {
          return "&o$server";
        }
      };
    }
  }
}
