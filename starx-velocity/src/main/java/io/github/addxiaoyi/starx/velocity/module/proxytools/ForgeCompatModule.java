package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Objects;

/**
 * Forge 兼容模块：处理 Forge 客户端的握手和 Mod 列表同步。
 *
 * <p>参考 Ambassador 和 Proxy-Compatible-Forge 插件逻辑。检测 Forge 客户端连接，
 * 确保 Mod 列表在代理端正确传递，保证 Forge 玩家能正常通过代理连接到子服。
 *
 * <p>TODO: 实现完整的 Forge 握手管道注入（ChannelInitializer hook）
 * TODO: 实现 FML2 握手协议处理
 * TODO: 实现 Mod 列表缓存和同步
 */
public final class ForgeCompatModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;

  public ForgeCompatModule(StarxVelocityPlugin plugin, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "forge";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) {
      return;
    }
    plugin.proxy().getEventManager().register(plugin, new ForgeListener());
  }

  @Override
  public void onDisable() {}

  void onPostLogin(PostLoginEvent event) {
    Player player = event.getPlayer();
    // TODO: Detect Forge client via handshake data
    if (config.debug()) {
      plugin.logger().info("Player " + player.getUsername() + " logged in (Forge compat active)");
    }
  }

  void onServerConnected(ServerConnectedEvent event) {
    // TODO: Forward Forge mod list to the target server
    if (config.debug()) {
      plugin
          .logger()
          .info(
              "Player "
                  + event.getPlayer().getUsername()
                  + " connected to "
                  + event.getServer().getServerInfo().getName());
    }
  }

  /** 模块配置。 */
  public interface Config {
    boolean enabled();

    boolean debug();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return true;
        }

        @Override
        public boolean debug() {
          return false;
        }
      };
    }
  }

  private final class ForgeListener {
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
      ForgeCompatModule.this.onPostLogin(event);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
      ForgeCompatModule.this.onServerConnected(event);
    }
  }
}