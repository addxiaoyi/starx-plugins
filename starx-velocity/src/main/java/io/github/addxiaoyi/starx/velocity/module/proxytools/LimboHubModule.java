package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Limbo 大厅模块：管理 Limbo 虚拟服务器，提供 /hub 和 /lobby 命令返回大厅。
 *
 * <p>核心功能：将玩家发送到配置的大厅服务器，显示在线玩家统计。
 */
public final class LimboHubModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;

  public LimboHubModule(StarxVelocityPlugin plugin, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "limbo";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) {
      return;
    }
    ProxyServer proxy = plugin.proxy();
    proxy.getCommandManager().register(
        proxy.getCommandManager().metaBuilder("hub").build(), new HubCommand());
    proxy.getCommandManager().register(
        proxy.getCommandManager().metaBuilder("lobby").build(), new LobbyCommand());
  }

  @Override
  public void onDisable() {}

  /** 将玩家发送到大厅服务器。 */
  public void sendToHub(Player player) {
    String hubServerName = config.hubServerName();
    Optional<RegisteredServer> hubServer = plugin.proxy().getServer(hubServerName);
    if (hubServer.isPresent()) {
      player.createConnectionRequest(hubServer.get()).connect();
    } else {
      player.sendMessage(Component.text("Hub server not available.", NamedTextColor.RED));
    }
  }

  private final class HubCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (invocation.source() instanceof Player player) {
        sendToHub(player);
      }
    }
  }

  private final class LobbyCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (invocation.source() instanceof Player player) {
        sendToHub(player);
      }
    }
  }

  /** 模块配置。 */
  public interface Config {
    boolean enabled();

    String hubServerName();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return true;
        }

        @Override
        public String hubServerName() {
          return "lobby";
        }
      };
    }
  }
}
