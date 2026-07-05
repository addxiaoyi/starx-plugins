package io.github.addxiaoyi.starx.paper.module.chat;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** 聊天模块：接收 Velocity 跨服聊天消息并在本服务器广播。 */
public final class ChatModule implements PaperModule, Listener {

  private static final String CHAT_BROADCAST = "chat_broadcast";

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private boolean enabled;

  public ChatModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  @Override
  public String getName() {
    return "chat";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    enabled = configLoader.isModuleEnabled("chat");
    plugin.getLogger().info("Chat module enabled state: " + enabled);
  }

  @Override
  public void onPluginMessage(PluginMessage message) {
    if (!enabled || !CHAT_BROADCAST.equals(message.command())) {
      return;
    }
    String sender = String.valueOf(message.payload().getOrDefault("sender", ""));
    String text = String.valueOf(message.payload().getOrDefault("message", ""));
    String format = configLoader.getChatFormat();
    Bukkit.broadcast(Component.text(format.replace("{player}", sender).replace("{message}", text)));
  }

  @EventHandler
  public void onChat(AsyncChatEvent event) {
    if (!enabled) {
      return;
    }
    String format = configLoader.getChatFormat();
    event.renderer(
        (source, sourceDisplayName, message, viewer) ->
            Component.text(
                format
                    .replace("{player}", source.getName())
                    .replace(
                        "{message}", PlainTextComponentSerializer.plainText().serialize(message))));
  }
}
