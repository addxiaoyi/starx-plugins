package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.messaging.VelocityMessageBridge;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock VelocityMessageBridge bridge;
  @Mock EventManager eventManager;

  ChatModule.Config enabledConfig;
  ChatModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    enabledConfig = () -> true;
    disabledConfig = () -> false;
  }

  @Test
  void shouldRegisterChatListenerOnEnable() {
    ChatModule module = new ChatModule(plugin, bridge, enabledConfig);
    module.onEnable();
    verify(eventManager).register(eq(plugin), any());
  }

  @Test
  void shouldBroadcastChatToPlayersOnOtherServers() {
    RegisteredServer lobby = serverNamed("lobby");
    RegisteredServer game = serverNamed("game");
    Player sender = playerOnServer("Alice", lobby);
    Player sameServer = playerOnServer("Bob", lobby);
    Player otherServer = playerOnServer("Charlie", game);

    when(proxy.getAllPlayers()).thenReturn(List.of(sender, sameServer, otherServer));

    PlayerChatEvent event = chatEvent(sender, "hello");
    ChatModule module = new ChatModule(plugin, bridge, enabledConfig);
    module.onPlayerChat(event);

    verify(bridge)
        .sendMessage(
            eq(otherServer),
            argThat(
                (PluginMessage msg) ->
                    msg.command().equals(ChatModule.CHAT_COMMAND)
                        && "Alice".equals(msg.payload().get("sender"))
                        && "hello".equals(msg.payload().get("message"))));
    verify(bridge, never()).sendMessage(eq(sender), any(PluginMessage.class));
    verify(bridge, never()).sendMessage(eq(sameServer), any(PluginMessage.class));
  }

  @Test
  void shouldDoNothingWhenDisabled() {
    Player sender = mock(Player.class);
    lenient().when(proxy.getAllPlayers()).thenReturn(List.of(sender));

    ChatModule module = new ChatModule(plugin, bridge, disabledConfig);
    module.onPlayerChat(chatEvent(sender, "hello"));

    verify(bridge, never()).sendMessage(any(), any(PluginMessage.class));
  }

  private PlayerChatEvent chatEvent(Player player, String message) {
    PlayerChatEvent event = mock(PlayerChatEvent.class);
    lenient().when(event.getPlayer()).thenReturn(player);
    lenient().when(event.getMessage()).thenReturn(message);
    return event;
  }

  private Player playerOnServer(String username, RegisteredServer server) {
    Player player = mock(Player.class);
    ServerConnection connection = mock(ServerConnection.class);
    lenient().when(connection.getServer()).thenReturn(server);
    lenient().when(player.getCurrentServer()).thenReturn(Optional.of(connection));
    lenient().when(player.getUsername()).thenReturn(username);
    return player;
  }

  private RegisteredServer serverNamed(String name) {
    RegisteredServer server = mock(RegisteredServer.class);
    ServerInfo info = mock(ServerInfo.class);
    lenient().when(info.getName()).thenReturn(name);
    lenient().when(server.getServerInfo()).thenReturn(info);
    return server;
  }
}
