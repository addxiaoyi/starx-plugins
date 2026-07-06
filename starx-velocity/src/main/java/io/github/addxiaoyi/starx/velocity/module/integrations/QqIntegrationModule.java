package io.github.addxiaoyi.starx.velocity.module.integrations;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.http.WebhookClient;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** QQ 机器人桥模块：将 Minecraft 聊天与 QQ 群双向桥接。 */
public final class QqIntegrationModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final WebhookClient webhookClient;
  private final Config config;

  public QqIntegrationModule(
      StarxVelocityPlugin plugin, WebhookClient webhookClient, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
    if (config.enabled()) {
      this.webhookClient = Objects.requireNonNull(webhookClient, "webhookClient");
    } else {
      this.webhookClient = webhookClient;
    }
  }

  @Override
  public String name() {
    return "integrations.qq";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new ChatListener());
  }

  void onPlayerChat(PlayerChatEvent event) {
    if (!config.enabled()) {
      return;
    }
    Objects.requireNonNull(webhookClient, "webhookClient");
    Player player = event.getPlayer();
    String formatted =
        config
            .forwardFormat()
            .replace("{player}", player.getUsername())
            .replace("{message}", event.getMessage());
    Map<String, Object> body =
        Map.of(
            "player", player.getUsername(),
            "message", event.getMessage(),
            "group_id", config.qqGroupId(),
            "formatted", formatted);
    webhookClient.post(config.webhookUrl(), body);
  }

  /** 将 QQ 消息广播到所有在线玩家。 */
  public void broadcastQqMessage(String qqSender, String message) {
    if (qqSender == null || qqSender.isBlank() || message == null || message.isBlank()) {
      return;
    }
    Component component =
        Component.text()
            .append(Component.text("[QQ] ", NamedTextColor.AQUA))
            .append(Component.text(qqSender, NamedTextColor.YELLOW))
            .append(Component.text(": ", NamedTextColor.WHITE))
            .append(Component.text(message, NamedTextColor.WHITE))
            .build();
    for (Player player : plugin.proxy().getAllPlayers()) {
      player.sendMessage(component);
    }
  }

  public interface Config {
    boolean enabled();

    String webhookUrl();

    String qqGroupId();

    String forwardFormat();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public String webhookUrl() {
          return "";
        }

        @Override
        public String qqGroupId() {
          return "";
        }

        @Override
        public String forwardFormat() {
          return "[QQ] {player}: {message}";
        }
      };
    }
  }

  private final class ChatListener {
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
      QqIntegrationModule.this.onPlayerChat(event);
    }
  }
}
