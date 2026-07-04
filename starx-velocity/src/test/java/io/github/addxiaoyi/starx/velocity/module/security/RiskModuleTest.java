package io.github.addxiaoyi.starx.velocity.module.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  RiskModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config = RiskModule.Config.defaultConfig();
  }

  @Test
  void shouldReturnCorrectName() {
    RiskModule module = new RiskModule(plugin, eventBus, config);
    assertThat(module.name()).isEqualTo("security.risk");
  }

  @Test
  void shouldRegisterEventListenersOnEnable() {
    RiskModule module = new RiskModule(plugin, eventBus, config);
    module.onEnable();

    verify(eventManager).register(any(), any());
  }

  @Test
  void shouldScoreRiskForUnknownIp() {
    RiskModule module = new RiskModule(plugin, eventBus, config);
    module.onEnable();

    int score = module.scoreIp("203.0.113.1");
    assertThat(score).isGreaterThanOrEqualTo(0);
  }

  @Test
  void shouldDetectHighRiskIp() {
    RiskModule.Config strictConfig =
        new RiskModule.Config() {
          @Override
          public int highRiskThreshold() {
            return 50;
          }

          @Override
          public boolean requireTotpForHighRisk() {
            return true;
          }

          @Override
          public boolean checkNewDevice() {
            return true;
          }

          @Override
          public boolean checkAsn() {
            return true;
          }
        };

    RiskModule module = new RiskModule(plugin, eventBus, strictConfig);
    module.onEnable();

    assertThat(module.isHighRisk(80)).isTrue();
  }

  @Test
  void shouldNotFlagLowRisk() {
    RiskModule module = new RiskModule(plugin, eventBus, config);
    module.onEnable();

    assertThat(module.isHighRisk(20)).isFalse();
  }

  @Test
  void shouldPublishHighRiskAlertOnLogin() {
    RiskModule.Config strictConfig =
        new RiskModule.Config() {
          @Override
          public int highRiskThreshold() {
            return 5;
          }

          @Override
          public boolean requireTotpForHighRisk() {
            return true;
          }

          @Override
          public boolean checkNewDevice() {
            return true;
          }

          @Override
          public boolean checkAsn() {
            return true;
          }
        };

    RiskModule module = new RiskModule(plugin, eventBus, strictConfig);
    module.onEnable();

    LoginEvent loginEvent = loginEvent("203.0.113.50");
    module.onLogin(loginEvent);

    verify(eventBus, atLeastOnce()).publish(any(StarxEvent.class));
  }

  @Test
  void shouldTrackNewDeviceLogin() {
    RiskModule module = new RiskModule(plugin, eventBus, config);
    module.onEnable();

    assertThat(module.isNewDevice(UUID.randomUUID(), "192.168.1.1")).isTrue();
  }

  @Test
  void shouldRecognizeKnownDevice() {
    RiskModule module = new RiskModule(plugin, eventBus, config);
    module.onEnable();

    UUID playerId = UUID.randomUUID();
    module.registerDevice(playerId, "10.0.0.1");

    assertThat(module.isNewDevice(playerId, "10.0.0.1")).isFalse();
  }

  @Test
  void shouldRequireTotpForHighRiskWhenConfigured() {
    RiskModule.Config strictConfig =
        new RiskModule.Config() {
          @Override
          public int highRiskThreshold() {
            return 50;
          }

          @Override
          public boolean requireTotpForHighRisk() {
            return true;
          }

          @Override
          public boolean checkNewDevice() {
            return true;
          }

          @Override
          public boolean checkAsn() {
            return true;
          }
        };

    RiskModule module = new RiskModule(plugin, eventBus, strictConfig);
    module.onEnable();

    assertThat(module.requiresTotp(80)).isTrue();
  }

  @Test
  void shouldNotRequireTotpWhenDisabled() {
    RiskModule module = new RiskModule(plugin, eventBus, config);
    module.onEnable();

    assertThat(module.requiresTotp(80)).isFalse();
  }

  private LoginEvent loginEvent(String ip) {
    LoginEvent event = mock(LoginEvent.class);
    Player player = mock(Player.class);
    lenient().when(player.getRemoteAddress()).thenReturn(new InetSocketAddress(ip, 25565));
    lenient().when(player.getUsername()).thenReturn("testPlayer");
    lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(event.getPlayer()).thenReturn(player);
    return event;
  }
}
