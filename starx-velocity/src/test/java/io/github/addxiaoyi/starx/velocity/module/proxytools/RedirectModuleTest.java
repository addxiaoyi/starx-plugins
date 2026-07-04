package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedirectModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventManager eventManager;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
  }

  @Test
  void shouldRegisterKickListenerOnEnable() {
    RedirectModule module = new RedirectModule(plugin, RedirectModule.Config.defaultConfig());
    module.onEnable();
    verify(eventManager).register(eq(plugin), any());
  }

  @Test
  void shouldRedirectToTargetServerWhenKicked() {
    RegisteredServer game = mock(RegisteredServer.class);
    RegisteredServer lobby = mock(RegisteredServer.class);
    when(proxy.getServer("lobby")).thenReturn(Optional.of(lobby));

    KickedFromServerEvent event =
        kickEvent(game, KickedFromServerEvent.DisconnectPlayer.create(Component.text("kicked")));

    RedirectModule module = new RedirectModule(plugin, () -> "lobby");
    module.onKicked(event);

    ArgumentCaptor<KickedFromServerEvent.ServerKickResult> resultCaptor =
        ArgumentCaptor.forClass(KickedFromServerEvent.ServerKickResult.class);
    verify(event).setResult(resultCaptor.capture());
    assertThat(resultCaptor.getValue()).isInstanceOf(KickedFromServerEvent.RedirectPlayer.class);
    assertThat(((KickedFromServerEvent.RedirectPlayer) resultCaptor.getValue()).getServer())
        .isEqualTo(lobby);
  }

  @Test
  void shouldNotRedirectWhenTargetIsCurrentServer() {
    RegisteredServer lobby = mock(RegisteredServer.class);
    when(proxy.getServer("lobby")).thenReturn(Optional.of(lobby));

    KickedFromServerEvent event =
        kickEvent(lobby, KickedFromServerEvent.DisconnectPlayer.create(Component.text("kicked")));

    RedirectModule module = new RedirectModule(plugin, () -> "lobby");
    module.onKicked(event);

    verify(event, never()).setResult(any());
  }

  @Test
  void shouldNotRedirectWhenTargetUnavailable() {
    RegisteredServer game = mock(RegisteredServer.class);
    when(proxy.getServer("lobby")).thenReturn(Optional.empty());

    KickedFromServerEvent event =
        kickEvent(game, KickedFromServerEvent.DisconnectPlayer.create(Component.text("kicked")));

    RedirectModule module = new RedirectModule(plugin, () -> "lobby");
    module.onKicked(event);

    verify(event, never()).setResult(any());
  }

  private KickedFromServerEvent kickEvent(
      RegisteredServer server, KickedFromServerEvent.ServerKickResult result) {
    KickedFromServerEvent event = mock(KickedFromServerEvent.class);
    lenient().when(event.getServer()).thenReturn(server);
    lenient().when(event.getResult()).thenReturn(result);
    return event;
  }
}
