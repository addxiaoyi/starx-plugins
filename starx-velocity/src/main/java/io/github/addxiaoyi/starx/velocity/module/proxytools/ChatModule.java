package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.messaging.VelocityMessageBridge;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** 跨服聊天模块：将玩家聊天消息转发到其他子服。 */
public final class ChatModule implements VelocityModule {

  public static final String CHAT_COMMAND = "chat_broadcast";

  private final StarxVelocityPlugin plugin;
  private final VelocityMessageBridge bridge;
  private final Config config;

  public ChatModule(StarxVelocityPlugin plugin, VelocityMessageBridge bridge, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.bridge = Objects.requireNonNull(bridge, "bridge");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "chat";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new ChatListener());
  }

  void onPlayerChat(PlayerChatEvent event) {
    if (!config.enabled()) {
      return;
    }
    Player sender = event.getPlayer();
    PluginMessage message =
        new PluginMessage(
            CHAT_COMMAND, Map.of("sender", sender.getUsername(), "message", event.getMessage()));

    Optional<ServerConnection> senderConnection = sender.getCurrentServer();
    for (Player target : plugin.proxy().getAllPlayers()) {
      if (target.equals(sender)) {
        continue;
      }
      Optional<ServerConnection> targetConnection = target.getCurrentServer();
      if (senderConnection.isPresent()
          && targetConnection.isPresent()
          && senderConnection.get().getServer().equals(targetConnection.get().getServer())) {
        continue;
      }
      bridge.sendMessage(target, message);
    }
  }

  public interface Config {
    boolean enabled();

    static Config defaultConfig() {
      return () -> true;
    }
  }

  private final class ChatListener {
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
      ChatModule.this.onPlayerChat(event);
    }
  }
}
