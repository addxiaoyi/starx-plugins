package io.github.addxiaoyi.starx.velocity.module.integrations;

import static org.assertj.core.api.Assertions.assertThat;
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
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.http.WebhookClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QqIntegrationModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock WebhookClient webhookClient;
  @Mock EventManager eventManager;

  QqIntegrationModule.Config enabledConfig;
  QqIntegrationModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    enabledConfig =
        new QqIntegrationModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public String webhookUrl() {
            return "https://hooks.example.com/qq";
          }

          @Override
          public String qqGroupId() {
            return "123456";
          }

          @Override
          public String forwardFormat() {
            return "[QQ] {player}: {message}";
          }
        };
    disabledConfig =
        new QqIntegrationModule.Config() {
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
            return "";
          }
        };
  }

  @Test
  void shouldHaveCorrectModuleName() {
    QqIntegrationModule module = new QqIntegrationModule(plugin, webhookClient, enabledConfig);
    assertThat(module.name()).isEqualTo("integrations.qq");
  }

  @Test
  void shouldRegisterChatListenerOnEnable() {
    QqIntegrationModule module = new QqIntegrationModule(plugin, webhookClient, enabledConfig);
    module.onEnable();
    verify(eventManager).register(eq(plugin), any());
  }

  @Test
  void shouldSendWebhookOnPlayerChat() {
    QqIntegrationModule module = new QqIntegrationModule(plugin, webhookClient, enabledConfig);
    Player player = mock(Player.class);
    when(player.getUsername()).thenReturn("Alice");

    PlayerChatEvent event = mock(PlayerChatEvent.class);
    when(event.getPlayer()).thenReturn(player);
    when(event.getMessage()).thenReturn("Hello from MC");

    module.onPlayerChat(event);

    verify(webhookClient)
        .post(
            eq("https://hooks.example.com/qq"),
            argThat(
                (Map<String, Object> body) ->
                    "Alice".equals(body.get("player"))
                        && "Hello from MC".equals(body.get("message"))
                        && "123456".equals(body.get("group_id"))));
  }

  @Test
  void shouldNotSendWebhookWhenDisabled() {
    QqIntegrationModule module = new QqIntegrationModule(plugin, webhookClient, disabledConfig);
    PlayerChatEvent event = mock(PlayerChatEvent.class);

    module.onPlayerChat(event);

    verify(webhookClient, never()).post(any(), any());
  }

  @Test
  void shouldBroadcastIncomingQqMessageToServer() {
    QqIntegrationModule module = new QqIntegrationModule(plugin, webhookClient, enabledConfig);
    Player player1 = mock(Player.class);
    Player player2 = mock(Player.class);
    when(proxy.getAllPlayers()).thenReturn(List.of(player1, player2));

    module.broadcastQqMessage("QQUser", "Hello from QQ");

    verify(player1)
        .sendMessage(
            argThat(
                (net.kyori.adventure.text.Component c) ->
                    c.toString().contains("QQUser") && c.toString().contains("Hello from QQ")));
    verify(player2)
        .sendMessage(
            argThat(
                (net.kyori.adventure.text.Component c) ->
                    c.toString().contains("QQUser") && c.toString().contains("Hello from QQ")));
  }

  @Test
  void shouldNotBroadcastEmptyMessage() {
    QqIntegrationModule module = new QqIntegrationModule(plugin, webhookClient, enabledConfig);

    module.broadcastQqMessage("", "");

    verify(proxy, never()).getAllPlayers();
  }

  @Test
  void shouldFormatMessageUsingForwardFormat() {
    QqIntegrationModule module = new QqIntegrationModule(plugin, webhookClient, enabledConfig);
    Player player = mock(Player.class);
    when(player.getUsername()).thenReturn("Alice");

    PlayerChatEvent event = mock(PlayerChatEvent.class);
    when(event.getPlayer()).thenReturn(player);
    when(event.getMessage()).thenReturn("Hello");

    module.onPlayerChat(event);

    verify(webhookClient)
        .post(
            eq("https://hooks.example.com/qq"),
            argThat(
                (Map<String, Object> body) -> "[QQ] Alice: Hello".equals(body.get("formatted"))));
  }
}
