package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.messaging.VelocityMessageBridge;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;

/** 维护模式模块：切换维护状态、拒绝非白名单玩家登录并同步到 Paper 后端。 */
public final class MaintenanceModule implements VelocityModule {

  public static final String MAINTENANCE_CHANGED = "proxy:maintenance:changed";
  private static final String DEFAULT_BYPASS_PERMISSION = "starx.maintenance.bypass";

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final VelocityMessageBridge bridge;
  private final Config config;
  private final AtomicBoolean enabled = new AtomicBoolean(false);

  public MaintenanceModule(
      StarxVelocityPlugin plugin, EventBus eventBus, VelocityMessageBridge bridge, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.bridge = Objects.requireNonNull(bridge, "bridge");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "starx.maintenance";
  }

  @Override
  public void onEnable() {
    ProxyServer proxy = plugin.proxy();
    proxy.getEventManager().register(plugin, new LoginListener());
    proxy
        .getCommandManager()
        .register(
            proxy.getCommandManager().metaBuilder("maintenance").build(), new MaintenanceCommand());
  }

  public boolean isEnabled() {
    return enabled.get();
  }

  public void setEnabled(boolean enabled) {
    if (this.enabled.getAndSet(enabled) != enabled) {
      eventBus.publish(MAINTENANCE_CHANGED, Map.of("enabled", enabled));
      syncToBackends(enabled);
    }
  }

  private void syncToBackends(boolean enabled) {
    PluginMessage message =
        new PluginMessage(PluginMessageChannels.CMD_CONFIG_SYNC, Map.of("maintenance", enabled));
    for (Player player : plugin.proxy().getAllPlayers()) {
      bridge.sendMessage(player, message);
    }
  }

  void onLogin(LoginEvent event) {
    if (!isEnabled()) {
      return;
    }
    Player player = event.getPlayer();
    if (canBypass(player)) {
      return;
    }
    event.setResult(ResultedEvent.ComponentResult.denied(config.kickMessage()));
  }

  private boolean canBypass(Player player) {
    return player.hasPermission(config.bypassPermission())
        || config.whitelist().contains(player.getUsername());
  }

  public interface Config {
    Component kickMessage();

    String bypassPermission();

    Set<String> whitelist();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public Component kickMessage() {
          return Component.text("Server is currently under maintenance.");
        }

        @Override
        public String bypassPermission() {
          return DEFAULT_BYPASS_PERMISSION;
        }

        @Override
        public Set<String> whitelist() {
          return Set.of();
        }
      };
    }
  }

  private final class LoginListener {
    @Subscribe
    public void onLogin(LoginEvent event) {
      MaintenanceModule.this.onLogin(event);
    }
  }

  private final class MaintenanceCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      String[] args = invocation.arguments();
      if (args.length != 1) {
        invocation.source().sendMessage(Component.text("Usage: /maintenance <on|off>"));
        return;
      }
      switch (args[0].toLowerCase()) {
        case "on" -> setEnabled(true);
        case "off" -> setEnabled(false);
        default -> invocation.source().sendMessage(Component.text("Usage: /maintenance <on|off>"));
      }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
      return invocation.source().hasPermission(config.bypassPermission());
    }
  }
}
