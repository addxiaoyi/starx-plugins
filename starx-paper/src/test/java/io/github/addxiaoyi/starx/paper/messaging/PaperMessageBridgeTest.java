package io.github.addxiaoyi.starx.paper.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageHandler;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaperMessageBridgeTest {

  @Mock StarxPaperPlugin plugin;
  @Mock Server server;
  @Mock Messenger messenger;
  @Mock Player player;
  @Mock PluginMessageHandler handler;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getMessenger()).thenReturn(messenger);
  }

  @Test
  void registerRegistersIncomingAndOutgoingChannels() {
    PaperMessageBridge bridge = new PaperMessageBridge(plugin, handler);
    bridge.register();

    verify(messenger).registerIncomingPluginChannel(plugin, PluginMessageChannels.MAIN, bridge);
    verify(messenger).registerOutgoingPluginChannel(plugin, PluginMessageChannels.MAIN);
  }

  @Test
  void unregisterUnregistersIncomingAndOutgoingChannels() {
    PaperMessageBridge bridge = new PaperMessageBridge(plugin, handler);
    bridge.unregister();

    verify(messenger).unregisterIncomingPluginChannel(plugin, PluginMessageChannels.MAIN, bridge);
    verify(messenger).unregisterOutgoingPluginChannel(plugin, PluginMessageChannels.MAIN);
  }

  @Test
  void sendWritesCommandAndPayload() {
    PaperMessageBridge bridge = new PaperMessageBridge(plugin, handler);
    PluginMessage message = new PluginMessage("test_cmd", Map.of("key", "value"));

    bridge.send(player, message);

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(player).sendPluginMessage(eq(plugin), eq(PluginMessageChannels.MAIN), captor.capture());
    assertThat(captor.getValue()).isNotEmpty();
  }

  @Test
  void receiveDeserializesMessageAndInvokesHandler() {
    PaperMessageBridge bridge = new PaperMessageBridge(plugin, handler);
    PluginMessage message = new PluginMessage("test_cmd", Map.of("key", "value"));
    byte[] data = serialize(message);

    bridge.onPluginMessageReceived(PluginMessageChannels.MAIN, player, data);

    ArgumentCaptor<PluginMessage> captor = ArgumentCaptor.forClass(PluginMessage.class);
    verify(handler).handle(captor.capture());
    assertThat(captor.getValue().command()).isEqualTo("test_cmd");
    assertThat(captor.getValue().payload()).containsEntry("key", "value");
  }

  @Test
  void receiveIgnoresMessagesFromOtherChannels() {
    PaperMessageBridge bridge = new PaperMessageBridge(plugin, handler);
    PluginMessage message = new PluginMessage("test_cmd", Map.of("key", "value"));
    byte[] data = serialize(message);

    bridge.onPluginMessageReceived("other:channel", player, data);

    verify(handler, never()).handle(any());
  }

  private byte[] serialize(PluginMessage message) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF(message.command());
    byte[] payload = new Gson().toJson(message.payload()).getBytes(StandardCharsets.UTF_8);
    out.writeInt(payload.length);
    out.write(payload);
    return out.toByteArray();
  }
}
