package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * 在线同步模块：保持玩家在代理端显示为在线状态，处理全局玩家列表。
 *
 * <p>参考 ProxyOnlineLinker 和 AlwaysOnline 插件逻辑。维护玩家在线状态映射， 提供 /list 命令显示全局玩家（包括不同子服的玩家）。
 */
public final class OnlineSyncModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;
  private final Map<UUID, String> onlinePlayers = new ConcurrentHashMap<>();

  public OnlineSyncModule(StarxVelocityPlugin plugin, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "online";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) {
      return;
    }
    ProxyServer proxy = plugin.proxy();
    proxy.getEventManager().register(plugin, new OnlineListener());
    proxy.getCommandManager().register(
        proxy.getCommandManager().metaBuilder("list").build(), new ListCommand());
  }

  @Override
  public void onDisable() {
    onlinePlayers.clear();
  }

  public int getOnlineCount() {
    return onlinePlayers.size();
  }

  void onPostLogin(PostLoginEvent event) {
    Player player = event.getPlayer();
    onlinePlayers.put(player.getUniqueId(), player.getUsername());
  }

  void onDisconnect(DisconnectEvent event) {
    onlinePlayers.remove(event.getPlayer().getUniqueId());
  }

  private final class ListCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      ProxyServer proxy = plugin.proxy();
      int total = proxy.getPlayerCount();
      invocation
          .source()
          .sendMessage(
              Component.text("==== Online Players (" + total + ") ====", NamedTextColor.GOLD)
                  .decoration(TextDecoration.BOLD, true));
      for (Player player : proxy.getAllPlayers()) {
        String serverName =
            player
                .getCurrentServer()
                .map(conn -> conn.getServer().getServerInfo().getName())
                .orElse("connecting");
        invocation
            .source()
            .sendMessage(
                Component.text("  " + player.getUsername() + " ", NamedTextColor.WHITE)
                    .append(Component.text("[" + serverName + "]", NamedTextColor.GRAY)));
      }
    }
  }

  /** 模块配置。 */
  public interface Config {
    boolean enabled();

    static Config defaultConfig() {
      return () -> true;
    }
  }

  private final class OnlineListener {
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
      OnlineSyncModule.this.onPostLogin(event);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
      OnlineSyncModule.this.onDisconnect(event);
    }
  }
}
