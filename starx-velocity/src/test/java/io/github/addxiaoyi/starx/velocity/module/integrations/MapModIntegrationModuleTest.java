package io.github.addxiaoyi.starx.velocity.module.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapModIntegrationModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventManager eventManager;
  @Mock ChannelRegistrar channelRegistrar;

  MapModIntegrationModule.Config enabledConfig;
  MapModIntegrationModule.Config disabledConfig;
  MapModIntegrationModule.Config noSyncOnJoinConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getChannelRegistrar()).thenReturn(channelRegistrar);
    enabledConfig =
        new MapModIntegrationModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public boolean syncOnJoin() {
            return true;
          }

          @Override
          public String mapDataChannel() {
            return "mapmod:data";
          }
        };
    disabledConfig =
        new MapModIntegrationModule.Config() {
          @Override
          public boolean enabled() {
            return false;
          }

          @Override
          public boolean syncOnJoin() {
            return true;
          }

          @Override
          public String mapDataChannel() {
            return "mapmod:data";
          }
        };
    noSyncOnJoinConfig =
        new MapModIntegrationModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public boolean syncOnJoin() {
            return false;
          }

          @Override
          public String mapDataChannel() {
            return "mapmod:data";
          }
        };
  }

  @Test
  void shouldHaveCorrectModuleName() {
    MapModIntegrationModule module = new MapModIntegrationModule(plugin, enabledConfig);
    assertThat(module.name()).isEqualTo("integrations.mapmod");
  }

  @Test
  void shouldRegisterChannelAndListenerOnEnable() {
    MapModIntegrationModule module = new MapModIntegrationModule(plugin, enabledConfig);
    module.onEnable();

    verify(channelRegistrar).register(any(ChannelIdentifier.class));
    verify(eventManager).register(eq(plugin), any());
  }

  @Test
  void shouldUnregisterChannelOnDisable() {
    MapModIntegrationModule module = new MapModIntegrationModule(plugin, enabledConfig);
    module.onEnable();
    module.onDisable();

    verify(channelRegistrar).unregister(any(ChannelIdentifier.class));
  }

  @Test
  void shouldNotRegisterChannelWhenDisabled() {
    MapModIntegrationModule module = new MapModIntegrationModule(plugin, disabledConfig);
    module.onEnable();

    verify(channelRegistrar, never()).register(any(ChannelIdentifier.class));
    verify(eventManager, never()).register(eq(plugin), any());
  }

  @Test
  void shouldSyncPlayerOnServerConnectedWhenSyncOnJoinEnabled() {
    MapModIntegrationModule module = new MapModIntegrationModule(plugin, enabledConfig);
    module.onEnable();
    Player player = mock(Player.class);
    when(player.getUsername()).thenReturn("Alice");

    ServerConnectedEvent event = mock(ServerConnectedEvent.class);
    when(event.getPlayer()).thenReturn(player);

    module.onServerConnected(event);

    verify(player).sendPluginMessage(any(ChannelIdentifier.class), any(byte[].class));
  }

  @Test
  void shouldNotSyncPlayerOnServerConnectedWhenSyncOnJoinDisabled() {
    MapModIntegrationModule module = new MapModIntegrationModule(plugin, noSyncOnJoinConfig);
    module.onEnable();

    ServerConnectedEvent event = mock(ServerConnectedEvent.class);

    module.onServerConnected(event);

    verify(proxy, never()).getAllPlayers();
  }

  @Test
  void shouldNotSyncWhenDisabled() {
    MapModIntegrationModule module = new MapModIntegrationModule(plugin, disabledConfig);
    ServerConnectedEvent event = mock(ServerConnectedEvent.class);

    module.onServerConnected(event);

    verify(proxy, never()).getAllPlayers();
  }
}