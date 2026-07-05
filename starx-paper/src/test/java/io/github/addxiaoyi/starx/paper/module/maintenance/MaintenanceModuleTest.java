package io.github.addxiaoyi.starx.paper.module.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaintenanceModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock Server server;
  @Mock PluginManager pluginManager;
  @Mock Player player;

  Logger logger = Logger.getLogger(MaintenanceModuleTest.class.getName());
  MaintenanceModule module;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
    module = new MaintenanceModule(plugin, configLoader);
  }

  @Test
  void shouldRegisterListenerOnEnable() {
    module.onEnable();
    verify(pluginManager).registerEvents(module, plugin);
  }

  @Test
  void shouldSyncEnabledStateFromVelocity() {
    module.onPluginMessage(
        new PluginMessage(PluginMessageChannels.CMD_CONFIG_SYNC, Map.of("maintenance", true)));

    assertThat(module.isEnabled()).isTrue();
  }

  @Test
  void shouldAllowBypassPlayerDuringMaintenance() {
    module.onPluginMessage(
        new PluginMessage(PluginMessageChannels.CMD_CONFIG_SYNC, Map.of("maintenance", true)));

    PlayerLoginEvent event = loginEventFor(player, true);
    module.onLogin(event);

    verify(event, never())
        .disallow(eq(PlayerLoginEvent.Result.KICK_WHITELIST), any(Component.class));
  }

  @Test
  void shouldDenyNonBypassPlayerDuringMaintenance() {
    module.onPluginMessage(
        new PluginMessage(PluginMessageChannels.CMD_CONFIG_SYNC, Map.of("maintenance", true)));

    PlayerLoginEvent event = loginEventFor(player, false);
    module.onLogin(event);

    verify(event).disallow(eq(PlayerLoginEvent.Result.KICK_WHITELIST), any(Component.class));
  }

  @Test
  void shouldAllowAllPlayersWhenMaintenanceDisabled() {
    module.onPluginMessage(
        new PluginMessage(PluginMessageChannels.CMD_CONFIG_SYNC, Map.of("maintenance", false)));

    PlayerLoginEvent event = loginEventFor(player, false);
    module.onLogin(event);

    verify(event, never())
        .disallow(eq(PlayerLoginEvent.Result.KICK_WHITELIST), any(Component.class));
  }

  private PlayerLoginEvent loginEventFor(Player player, boolean hasBypass) {
    PlayerLoginEvent event = mock(PlayerLoginEvent.class);
    lenient().when(event.getPlayer()).thenReturn(player);
    lenient().when(player.hasPermission("starx.maintenance.bypass")).thenReturn(hasBypass);
    return event;
  }
}
