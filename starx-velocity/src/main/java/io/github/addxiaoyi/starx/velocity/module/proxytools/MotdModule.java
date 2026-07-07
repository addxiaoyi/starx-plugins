package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Objects;
import net.kyori.adventure.text.Component;

/** 自定义 MOTD 模块：根据维护状态切换普通/维护 MOTD。 */
public final class MotdModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;
  private volatile boolean maintenanceActive;

  public MotdModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "starx.motd";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new PingListener());
    eventBus.subscribe(MaintenanceModule.MAINTENANCE_CHANGED, this::onMaintenanceChanged);
  }

  public boolean isMaintenanceActive() {
    return maintenanceActive;
  }

  void onMaintenanceChanged(StarxEvent event) {
    Boolean enabled = event.get("enabled");
    this.maintenanceActive = enabled != null && enabled;
  }

  void onProxyPing(ProxyPingEvent event) {
    Component motd = maintenanceActive ? config.maintenanceMotd() : config.normalMotd();
    event.setPing(event.getPing().asBuilder().description(motd).build());
  }

  public interface Config {
    Component normalMotd();

    Component maintenanceMotd();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public Component normalMotd() {
          return Component.text("Welcome to StarX!");
        }

        @Override
        public Component maintenanceMotd() {
          return Component.text("StarX is under maintenance.");
        }
      };
    }
  }

  private final class PingListener {
    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
      MotdModule.this.onProxyPing(event);
    }
  }
}
