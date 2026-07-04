package io.github.addxiaoyi.starx.paper.module.networking;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import java.nio.charset.StandardCharsets;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * 自定义网络通道模块：注册自定义 Plugin Messaging 通道，处理皮肤同步、配置同步、状态同步。
 */
public final class NetworkingModule implements PaperModule, PluginMessageListener {

  private static final String NETWORKING_CHANNEL = "starx:networking";

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private boolean enabled;

  public NetworkingModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  @Override
  public String getName() {
    return "networking";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    plugin
        .getServer()
        .getMessenger()
        .registerOutgoingPluginChannel(plugin, NETWORKING_CHANNEL);
    plugin
        .getServer()
        .getMessenger()
        .registerIncomingPluginChannel(plugin, NETWORKING_CHANNEL, this);
    enabled = configLoader.isModuleEnabled("networking");
    plugin.getLogger().info("Networking module enabled state: " + enabled);
  }

  @Override
  public void onDisable() {
    plugin
        .getServer()
        .getMessenger()
        .unregisterIncomingPluginChannel(plugin, NETWORKING_CHANNEL, this);
    plugin
        .getServer()
        .getMessenger()
        .unregisterOutgoingPluginChannel(plugin, NETWORKING_CHANNEL);
  }

  @Override
  public void onPluginMessageReceived(String channel, Player player, byte[] message) {
    if (!NETWORKING_CHANNEL.equals(channel) || !enabled) {
      return;
    }
    String payload = new String(message, StandardCharsets.UTF_8);
    // TODO: 解析并处理自定义包：皮肤同步、配置同步、状态同步
    plugin.getLogger().info("Networking message received from " + player.getName());
  }

  /**
   * 向所有在线玩家发送自定义网络包。
   */
  public void sendToAll(byte[] data) {
    if (!enabled) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendPluginMessage(plugin, NETWORKING_CHANNEL, data);
    }
  }
}