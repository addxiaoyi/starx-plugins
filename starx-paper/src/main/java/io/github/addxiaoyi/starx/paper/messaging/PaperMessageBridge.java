package io.github.addxiaoyi.starx.paper.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageHandler;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/** Plugin Messaging 接收/发送骨架，通道 {@code starx:main}。 */
public final class PaperMessageBridge implements PluginMessageListener {

  private static final Gson GSON = new Gson();

  private final StarxPaperPlugin plugin;
  private final PluginMessageHandler handler;

  public PaperMessageBridge(StarxPaperPlugin plugin, PluginMessageHandler handler) {
    this.plugin = plugin;
    this.handler = handler;
  }

  public void register() {
    plugin
        .getServer()
        .getMessenger()
        .registerIncomingPluginChannel(plugin, PluginMessageChannels.MAIN, this);
    plugin
        .getServer()
        .getMessenger()
        .registerOutgoingPluginChannel(plugin, PluginMessageChannels.MAIN);
  }

  public void unregister() {
    plugin
        .getServer()
        .getMessenger()
        .unregisterIncomingPluginChannel(plugin, PluginMessageChannels.MAIN, this);
    plugin
        .getServer()
        .getMessenger()
        .unregisterOutgoingPluginChannel(plugin, PluginMessageChannels.MAIN);
  }

  /**
   * 向指定玩家发送 Plugin Message。
   *
   * @param player 目标玩家
   * @param message 消息
   */
  public void send(Player player, PluginMessage message) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF(message.command());
    byte[] payload = GSON.toJson(message.payload()).getBytes(StandardCharsets.UTF_8);
    out.writeInt(payload.length);
    out.write(payload);
    player.sendPluginMessage(plugin, PluginMessageChannels.MAIN, out.toByteArray());
  }

  @Override
  public void onPluginMessageReceived(String channel, Player player, byte[] message) {
    if (!PluginMessageChannels.MAIN.equals(channel)) {
      return;
    }
    ByteArrayDataInput in = ByteStreams.newDataInput(message);
    String command = in.readUTF();
    int length = in.readInt();
    byte[] payloadBytes = new byte[length];
    in.readFully(payloadBytes);
    String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = GSON.fromJson(payloadJson, Map.class);
    handler.handle(new PluginMessage(command, payload));
  }
}
