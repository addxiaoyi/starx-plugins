package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MotdModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  MotdModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config =
        new MotdModule.Config() {
          @Override
          public Component normalMotd() {
            return Component.text("Normal MOTD");
          }

          @Override
          public Component maintenanceMotd() {
            return Component.text("Maintenance MOTD");
          }
        };
  }

  @Test
  void shouldRegisterPingListenerAndSubscribeToMaintenanceEvents() {
    MotdModule module = new MotdModule(plugin, eventBus, config);
    module.onEnable();

    verify(eventManager).register(eq(plugin), any());
    verify(eventBus).subscribe(eq(MaintenanceModule.MAINTENANCE_CHANGED), any(Consumer.class));
  }

  @Test
  void shouldUseNormalMotdByDefault() {
    MotdModule module = new MotdModule(plugin, eventBus, config);
    ProxyPingEvent event = pingEvent(Component.text("default"));

    module.onProxyPing(event);

    ArgumentCaptor<ServerPing> pingCaptor = ArgumentCaptor.forClass(ServerPing.class);
    verify(event).setPing(pingCaptor.capture());
    assertThat(pingCaptor.getValue().getDescriptionComponent()).isEqualTo(config.normalMotd());
  }

  @Test
  void shouldSwitchToMaintenanceMotdWhenEnabled() {
    MotdModule module = new MotdModule(plugin, eventBus, config);
    module.onMaintenanceChanged(
        new StarxEvent(MaintenanceModule.MAINTENANCE_CHANGED, Map.of("enabled", true)));

    ProxyPingEvent event = pingEvent(Component.text("default"));
    module.onProxyPing(event);

    ArgumentCaptor<ServerPing> pingCaptor = ArgumentCaptor.forClass(ServerPing.class);
    verify(event).setPing(pingCaptor.capture());
    assertThat(pingCaptor.getValue().getDescriptionComponent()).isEqualTo(config.maintenanceMotd());
    assertThat(module.isMaintenanceActive()).isTrue();
  }

  @Test
  void shouldSwitchBackToNormalMotdWhenDisabled() {
    MotdModule module = new MotdModule(plugin, eventBus, config);
    module.onMaintenanceChanged(
        new StarxEvent(MaintenanceModule.MAINTENANCE_CHANGED, Map.of("enabled", true)));
    module.onMaintenanceChanged(
        new StarxEvent(MaintenanceModule.MAINTENANCE_CHANGED, Map.of("enabled", false)));

    ProxyPingEvent event = pingEvent(Component.text("default"));
    module.onProxyPing(event);

    ArgumentCaptor<ServerPing> pingCaptor = ArgumentCaptor.forClass(ServerPing.class);
    verify(event).setPing(pingCaptor.capture());
    assertThat(pingCaptor.getValue().getDescriptionComponent()).isEqualTo(config.normalMotd());
    assertThat(module.isMaintenanceActive()).isFalse();
  }

  private ProxyPingEvent pingEvent(Component description) {
    ProxyPingEvent event = mock(ProxyPingEvent.class);
    ServerPing ping = ServerPing.builder().description(description).build();
    when(event.getPing()).thenReturn(ping);
    return event;
  }
}
