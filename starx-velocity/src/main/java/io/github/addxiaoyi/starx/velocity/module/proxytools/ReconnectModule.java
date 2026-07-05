package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 重连模块：玩家断开后自动重连到上次所在服务器。
 *
 * <p>监听 DisconnectEvent 记录玩家最后所在服务器，下次登录时自动发送到该服务器。
 */
public final class ReconnectModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;
  private final Map<UUID, String> lastServerMap = new ConcurrentHashMap<>();

  public ReconnectModule(StarxVelocityPlugin plugin, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "reconnect";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new ReconnectListener());
  }

  @Override
  public void onDisable() {
    lastServerMap.clear();
  }

  /** 获取玩家最后所在服务器名称。 */
  public Optional<String> getLastServer(UUID playerUuid) {
    return Optional.ofNullable(lastServerMap.get(playerUuid));
  }

  void onDisconnect(DisconnectEvent event) {
    if (!config.enabled()) {
      return;
    }
    Player player = event.getPlayer();
    Optional<ServerConnection> currentServer = player.getCurrentServer();
    currentServer.ifPresent(
        connection ->
            lastServerMap.put(
                player.getUniqueId(), connection.getServer().getServerInfo().getName()));
  }

  void onLogin(LoginEvent event) {
    if (!config.enabled()) {
      return;
    }
    Player player = event.getPlayer();
    String lastServerName = lastServerMap.get(player.getUniqueId());
    if (lastServerName == null) {
      return;
    }
    ProxyServer proxy = plugin.proxy();
    Optional<RegisteredServer> targetServer = proxy.getServer(lastServerName);
    targetServer.ifPresent(
        server ->
            proxy
                .getScheduler()
                .buildTask(plugin, () -> player.createConnectionRequest(server).connect())
                .schedule());
  }

  /** 模块配置。 */
  public interface Config {
    boolean enabled();

    static Config defaultConfig() {
      return () -> true;
    }
  }

  private final class ReconnectListener {
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
      ReconnectModule.this.onDisconnect(event);
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
      ReconnectModule.this.onLogin(event);
    }
  }
}
