package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Objects;
import java.util.Optional;

/** 玩家被 kick 时重定向到指定子服或 Lobby。 */
public final class RedirectModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;

  public RedirectModule(StarxVelocityPlugin plugin, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "redirect";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new KickListener());
  }

  void onKicked(KickedFromServerEvent event) {
    KickedFromServerEvent.ServerKickResult current = event.getResult();
    if (current == null) {
      return;
    }
    Optional<RegisteredServer> target = plugin.proxy().getServer(config.targetServer());
    if (target.isEmpty()) {
      return;
    }
    RegisteredServer targetServer = target.get();
    if (event.getServer().equals(targetServer)) {
      return;
    }
    event.setResult(KickedFromServerEvent.RedirectPlayer.create(targetServer));
  }

  public interface Config {
    String targetServer();

    static Config defaultConfig() {
      return () -> "lobby";
    }
  }

  private final class KickListener {
    @Subscribe
    public void onKicked(KickedFromServerEvent event) {
      RedirectModule.this.onKicked(event);
    }
  }
}
