package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.messaging.VelocityMessageBridge;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaintenanceModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock VelocityMessageBridge bridge;
  @Mock EventManager eventManager;
  @Mock CommandManager commandManager;

  MaintenanceModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getCommandManager()).thenReturn(commandManager);
    config =
        new MaintenanceModule.Config() {
          @Override
          public Component kickMessage() {
            return Component.text("Maintenance kick");
          }

          @Override
          public String bypassPermission() {
            return "starx.maintenance.bypass";
          }

          @Override
          public Set<String> whitelist() {
            return Set.of("admin");
          }
        };
  }

  @Test
  void shouldRegisterListenerAndCommandOnEnable() {
    MaintenanceModule module = new MaintenanceModule(plugin, eventBus, bridge, config);
    module.onEnable();

    verify(eventManager).register(eq(plugin), any());
    verify(commandManager).register(eq("maintenance"), any(SimpleCommand.class));
  }

  @Test
  void shouldToggleOnAndPublishEvent() {
    MaintenanceModule module = new MaintenanceModule(plugin, eventBus, bridge, config);
    when(proxy.getAllPlayers()).thenReturn(List.of());

    module.setEnabled(true);

    assertThat(module.isEnabled()).isTrue();
    verify(eventBus)
        .publish(
            eq(MaintenanceModule.MAINTENANCE_CHANGED),
            argThat((Map<String, Object> payload) -> Boolean.TRUE.equals(payload.get("enabled"))));
  }

  @Test
  void shouldToggleOffAndSyncToBackends() {
    MaintenanceModule module = new MaintenanceModule(plugin, eventBus, bridge, config);
    Player player = mock(Player.class);
    when(proxy.getAllPlayers()).thenReturn(List.of(player));

    module.setEnabled(true);
    module.setEnabled(false);

    assertThat(module.isEnabled()).isFalse();
    verify(bridge)
        .sendMessage(
            eq(player),
            argThat(
                (PluginMessage msg) ->
                    msg.command().equals(PluginMessageChannels.CMD_CONFIG_SYNC)
                        && Boolean.FALSE.equals(msg.payload().get("maintenance"))));
  }

  @Test
  void shouldDenyLoginWhenEnabledAndNoBypass() {
    MaintenanceModule module = new MaintenanceModule(plugin, eventBus, bridge, config);
    module.setEnabled(true);
    Player player = playerWithPermission(false, "guest");
    LoginEvent event = loginEventFor(player);

    module.onLogin(event);

    ArgumentCaptor<ResultedEvent.ComponentResult> resultCaptor =
        ArgumentCaptor.forClass(ResultedEvent.ComponentResult.class);
    verify(event).setResult(resultCaptor.capture());
    assertThat(resultCaptor.getValue().isAllowed()).isFalse();
  }

  @Test
  void shouldAllowWhitelistedPlayerDuringMaintenance() {
    MaintenanceModule module = new MaintenanceModule(plugin, eventBus, bridge, config);
    module.setEnabled(true);
    Player player = playerWithPermission(false, "admin");
    LoginEvent event = loginEventFor(player);

    module.onLogin(event);

    verify(event, never()).setResult(any());
  }

  @Test
  void shouldAllowPlayerWithBypassPermissionDuringMaintenance() {
    MaintenanceModule module = new MaintenanceModule(plugin, eventBus, bridge, config);
    module.setEnabled(true);
    Player player = playerWithPermission(true, "guest");
    LoginEvent event = loginEventFor(player);

    module.onLogin(event);

    verify(event, never()).setResult(any());
  }

  @Test
  void shouldAllowLoginWhenMaintenanceDisabled() {
    MaintenanceModule module = new MaintenanceModule(plugin, eventBus, bridge, config);
    Player player = playerWithPermission(false, "guest");
    LoginEvent event = loginEventFor(player);

    module.onLogin(event);

    verify(event, never()).setResult(any());
  }

  private Player playerWithPermission(boolean hasBypass, String username) {
    Player player = mock(Player.class);
    lenient().when(player.hasPermission(config.bypassPermission())).thenReturn(hasBypass);
    lenient().when(player.getUsername()).thenReturn(username);
    return player;
  }

  private LoginEvent loginEventFor(Player player) {
    LoginEvent event = mock(LoginEvent.class);
    lenient().when(event.getPlayer()).thenReturn(player);
    return event;
  }
}
