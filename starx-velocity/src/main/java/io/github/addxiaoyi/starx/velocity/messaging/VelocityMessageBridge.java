package io.github.addxiaoyi.starx.velocity.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/** Velocity 端 Plugin Messaging 桥，负责与 Paper 后端收发 starx:main 通道消息。 */
public final class VelocityMessageBridge implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final ProxyServer proxy;
  private final EventBus eventBus;
  private final ChannelIdentifier channel;
  private final Gson gson = new Gson();

  public VelocityMessageBridge(StarxVelocityPlugin plugin, ProxyServer proxy, EventBus eventBus) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.proxy = Objects.requireNonNull(proxy, "proxy");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.channel = MinecraftChannelIdentifier.create("starx", "main");
  }

  @Override
  public String name() {
    return "messaging";
  }

  @Override
  public void onEnable() {
    proxy.getChannelRegistrar().register(channel);
    proxy.getEventManager().register(plugin, new MessageListener());
  }

  /** 向指定玩家发送 PluginMessage（Paper 兼容格式：UTF 命令 + 长度 + JSON payload）。 */
  public void sendMessage(Player player, PluginMessage message) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(message, "message");
    byte[] payload = gson.toJson(message.payload()).getBytes(StandardCharsets.UTF_8);
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF(message.command());
    out.writeInt(payload.length);
    out.write(payload);
    player.sendPluginMessage(channel, out.toByteArray());
  }

  public ChannelIdentifier channel() {
    return channel;
  }

  private final class MessageListener {

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
      if (!event.getIdentifier().equals(channel)) {
        return;
      }
      ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
      String command = in.readUTF();
      int length = in.readInt();
      byte[] payloadBytes = new byte[length];
      in.readFully(payloadBytes);
      @SuppressWarnings("unchecked")
      Map<String, Object> payload =
          gson.fromJson(new String(payloadBytes, StandardCharsets.UTF_8), Map.class);
      PluginMessage message = new PluginMessage(command, payload);
      eventBus.publish(
          EventTypes.SYNC_PLAYER_STATE,
          Map.of(
              "command", message.command(),
              "payload", message.payload()));
    }
  }
}
