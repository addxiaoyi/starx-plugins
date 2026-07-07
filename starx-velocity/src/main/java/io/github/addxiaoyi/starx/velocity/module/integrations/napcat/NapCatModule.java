package io.github.addxiaoyi.starx.velocity.module.integrations.napcat;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import io.github.addxiaoyi.starx.common.auth.BindingVerificationService;
import io.github.addxiaoyi.starx.common.database.JdbcBindingRepository;
import io.github.addxiaoyi.starx.common.model.PlayerBinding;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig.NapcatConfig;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import io.github.addxiaoyi.starx.velocity.module.integrations.napcat.NapCatWebSocketClient.MessageHandler;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NapCatModule implements VelocityModule, MessageHandler {

  private static final Logger log = LoggerFactory.getLogger(NapCatModule.class);

  private final StarxVelocityPlugin plugin;
  private final JdbcBindingRepository bindingRepo;
  private final BindingVerificationService bindingVerification;
  private final NapcatConfig config;
  private NapCatWebSocketClient wsClient;

  public NapCatModule(
      StarxVelocityPlugin plugin,
      JdbcBindingRepository bindingRepo,
      BindingVerificationService bindingVerification,
      NapcatConfig config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.bindingRepo = Objects.requireNonNull(bindingRepo, "bindingRepo");
    this.bindingVerification = Objects.requireNonNull(bindingVerification, "bindingVerification");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "starx.integrations.napcat";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) return;

    wsClient = new NapCatWebSocketClient(config.wsUrl(), config.httpUrl(), this);
    wsClient.start();

    plugin.proxy().getEventManager().register(plugin, new ChatListener());
    log.info("NapCat module enabled: WS={}, HTTP={}", config.wsUrl(), config.httpUrl());
  }

  @Override
  public void onDisable() {
    if (wsClient != null) {
      wsClient.stop();
      wsClient = null;
    }
  }

  @Override
  public void onPrivateMessage(long userId, String message, String nickname) {
    String code = extractCode(message);
    if (code == null) return;

    UUID playerUuid = bindingVerification.verifyCode(code);
    if (playerUuid == null) {
      sendPrivateMessage(userId, "Invalid or expired verification code.");
      return;
    }

    String qqId = String.valueOf(userId);
    Optional<PlayerBinding> existing = bindingRepo.findByPlayer(playerUuid);
    if (existing.isPresent() && existing.get().qqId() != null) {
      if (existing.get().qqId().equals(qqId)) {
        sendPrivateMessage(userId, "Your Minecraft account is already bound to this QQ.");
      } else {
        sendPrivateMessage(userId, "This Minecraft account is already bound to another QQ.");
      }
      return;
    }

    PlayerBinding binding = new PlayerBinding(playerUuid, qqId, null, System.currentTimeMillis());
    bindingRepo.save(binding);
    sendPrivateMessage(userId, "QQ binding successful! You can now play on the server.");
    log.info("QQ binding: player={} qq={}", playerUuid, qqId);
  }

  @Override
  public void onGroupMessage(long groupId, long userId, String message, String nickname) {
    if (config.qqGroupId() <= 0 || groupId != config.qqGroupId()) return;

    broadcastQqMessage(nickname, message);
  }

  void onPlayerChat(PlayerChatEvent event) {
    if (!config.enabled() || config.qqGroupId() <= 0) return;
    if (wsClient == null) return;

    Player player = event.getPlayer();
    String formatted =
        config
            .forwardFormat()
            .replace("{player}", player.getUsername())
            .replace("{message}", event.getMessage());

    wsClient.sendGroupMessage(config.qqGroupId(), formatted);
  }

  private void broadcastQqMessage(String qqSender, String message) {
    if (qqSender == null || qqSender.isBlank() || message == null || message.isBlank()) return;

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

  private void sendPrivateMessage(long userId, String text) {
    if (wsClient != null) {
      wsClient.sendPrivateMessage(userId, text);
    }
  }

  private static String extractCode(String message) {
    if (message == null || message.isBlank()) return null;
    String trimmed = message.trim();
    if (trimmed.length() == 6 && trimmed.chars().allMatch(Character::isDigit)) {
      return trimmed;
    }
    return null;
  }

  private final class ChatListener {
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
      NapCatModule.this.onPlayerChat(event);
    }
  }
}
