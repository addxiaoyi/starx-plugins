package io.github.addxiaoyi.starx.velocity.module.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BotFilterModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  BotFilterModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config = BotFilterModule.Config.defaultConfig();
  }

  @Test
  void shouldReturnCorrectName() {
    BotFilterModule module = new BotFilterModule(plugin, eventBus, config);
    assertThat(module.name()).isEqualTo("security.bot");
  }

  @Test
  void shouldRegisterEventListenersOnEnable() {
    BotFilterModule module = new BotFilterModule(plugin, eventBus, config);
    module.onEnable();

    verify(eventManager, times(2)).register(eq(plugin), any());
  }

  @Test
  void shouldAllowNormalPingRate() {
    BotFilterModule module = new BotFilterModule(plugin, eventBus, config);
    module.onEnable();

    ProxyPingEvent event = pingEvent("127.0.0.1");
    for (int i = 0; i < 3; i++) {
      module.onProxyPing(event);
    }

    assertThat(module.getPingCount("127.0.0.1")).isGreaterThan(0);
  }

  @Test
  void shouldDetectPingFlood() {
    BotFilterModule.Config strictConfig =
        new BotFilterModule.Config() {
          @Override
          public int maxPingsPerSecond() {
            return 5;
          }

          @Override
          public int maxConnectionsPerSecond() {
            return 10;
          }

          @Override
          public boolean checkClientBrand() {
            return true;
          }

          @Override
          public boolean checkClientSettings() {
            return true;
          }

          @Override
          public long cachePurgeMillis() {
            return 60000;
          }
        };

    BotFilterModule module = new BotFilterModule(plugin, eventBus, strictConfig);
    module.onEnable();

    ProxyPingEvent event = pingEvent("10.0.0.1");
    for (int i = 0; i < 10; i++) {
      module.onProxyPing(event);
    }

    verify(eventBus, atLeastOnce()).publish(any(StarxEvent.class));
  }

  @Test
  void shouldNotTriggerAlertOnNormalTraffic() {
    BotFilterModule module = new BotFilterModule(plugin, eventBus, config);
    module.onEnable();

    ProxyPingEvent event = pingEvent("192.168.1.1");
    module.onProxyPing(event);

    verify(eventBus, never()).publish(any(StarxEvent.class));
  }

  @Test
  void shouldTrackConnectionsViaLoginEvent() {
    BotFilterModule module = new BotFilterModule(plugin, eventBus, config);
    module.onEnable();

    LoginEvent loginEvent = loginEvent("192.168.1.100");
    module.onLogin(loginEvent);

    assertThat(module.getConnectionCount("192.168.1.100")).isEqualTo(1);
  }

  @Test
  void shouldPurgeExpiredCacheEntries() {
    BotFilterModule.Config shortCacheConfig =
        new BotFilterModule.Config() {
          @Override
          public int maxPingsPerSecond() {
            return 20;
          }

          @Override
          public int maxConnectionsPerSecond() {
            return 20;
          }

          @Override
          public boolean checkClientBrand() {
            return true;
          }

          @Override
          public boolean checkClientSettings() {
            return true;
          }

          @Override
          public long cachePurgeMillis() {
            return 1;
          }
        };

    BotFilterModule module = new BotFilterModule(plugin, eventBus, shortCacheConfig);
    module.onEnable();

    ProxyPingEvent event = pingEvent("172.16.0.1");
    module.onProxyPing(event);

    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    module.purgeExpired();

    assertThat(module.getPingCount("172.16.0.1")).isEqualTo(0);
  }

  @Test
  void shouldCallOnDisable() {
    BotFilterModule module = new BotFilterModule(plugin, eventBus, config);
    module.onEnable();
    module.onDisable();

    assertThat(module.getPingCount("any")).isEqualTo(0);
    assertThat(module.getConnectionCount("any")).isEqualTo(0);
  }

  private ProxyPingEvent pingEvent(String ip) {
    ProxyPingEvent event = mock(ProxyPingEvent.class);
    InboundConnection connection = mock(InboundConnection.class);
    when(connection.getRemoteAddress()).thenReturn(new InetSocketAddress(ip, 25565));
    when(event.getConnection()).thenReturn(connection);
    return event;
  }

  private LoginEvent loginEvent(String ip) {
    LoginEvent event = mock(LoginEvent.class);
    com.velocitypowered.api.proxy.Player player = mock(com.velocitypowered.api.proxy.Player.class);
    lenient().when(player.getRemoteAddress()).thenReturn(new InetSocketAddress(ip, 25565));
    lenient().when(player.getUsername()).thenReturn("testPlayer");
    when(event.getPlayer()).thenReturn(player);
    return event;
  }
}
