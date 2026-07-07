package io.github.addxiaoyi.starx.velocity.module.integrations;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 地图模组同步模块：在代理端协调 Xaero/JourneyMap 等地图模组的数据同步。 */
public final class MapModIntegrationModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;
  private final Set<String> syncedPlayers = ConcurrentHashMap.newKeySet();
  private ChannelIdentifier channel;

  public MapModIntegrationModule(StarxVelocityPlugin plugin, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "starx.integrations.mapmod";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) {
      return;
    }
    channel = MinecraftChannelIdentifier.create("mapmod", "data");
    plugin.proxy().getChannelRegistrar().register(channel);
    plugin.proxy().getEventManager().register(plugin, new ServerConnectListener());
    // TODO: 注册多个地图模组通道（xaerominimap:main, xaeroworldmap:main, worldinfo:world_id）
    // TODO: 支持多种地图模组格式
  }

  @Override
  public void onDisable() {
    if (channel != null) {
      plugin.proxy().getChannelRegistrar().unregister(channel);
    }
    syncedPlayers.clear();
  }

  void onServerConnected(ServerConnectedEvent event) {
    if (!config.enabled() || !config.syncOnJoin()) {
      return;
    }
    Player player = event.getPlayer();
    String playerName = player.getUsername();
    if (!syncedPlayers.add(playerName)) {
      return;
    }
    // TODO: 发送完整的地图数据同步包（level_id, minimap_data, worldmap_data）
    player.sendPluginMessage(channel, new byte[0]);
  }

  public interface Config {
    boolean enabled();

    boolean syncOnJoin();

    String mapDataChannel();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public boolean syncOnJoin() {
          return true;
        }

        @Override
        public String mapDataChannel() {
          return "mapmod:data";
        }
      };
    }
  }

  private final class ServerConnectListener {
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
      MapModIntegrationModule.this.onServerConnected(event);
    }
  }
}
