package io.github.addxiaoyi.starx.paper.module.chat;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** 聊天模块骨架，监听 AsyncPlayerChatEvent 并格式化（占位）。 */
public final class ChatModule implements PaperModule, Listener {

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

  @EventHandler
  public void onChat(AsyncPlayerChatEvent event) {
    if (!enabled) {
      return;
    }
    String format = configLoader.getChatFormat();
    event.setFormat(
        format
            .replace("{player}", event.getPlayer().getName())
            .replace("{message}", event.getMessage()));
  }
}
