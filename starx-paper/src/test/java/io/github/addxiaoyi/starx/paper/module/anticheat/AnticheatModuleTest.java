package io.github.addxiaoyi.starx.paper.module.anticheat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnticheatModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock Server server;
  @Mock PluginManager pluginManager;
  @Mock Player player;

  Logger logger = Logger.getLogger(AnticheatModuleTest.class.getName());
  AnticheatModule module;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
    lenient().when(configLoader.isModuleEnabled("anticheat")).thenReturn(true);
    module = new AnticheatModule(plugin, configLoader);
  }

  @Test
  void shouldRegisterListenerOnEnable() {
    module.onEnable();
    verify(pluginManager).registerEvents(module, plugin);
  }

  @Test
  void shouldBeEnabledWhenConfigEnabled() {
    module.onEnable();
    assertThat(module.isEnabled()).isTrue();
  }

  @Test
  void shouldBeDisabledWhenConfigDisabled() {
    when(configLoader.isModuleEnabled("anticheat")).thenReturn(false);
    module.onEnable();
    assertThat(module.isEnabled()).isFalse();
  }

  @Test
  void shouldNotCheckSpeedWhenDisabled() {
    when(configLoader.isModuleEnabled("anticheat")).thenReturn(false);
    module.onEnable();

    PlayerMoveEvent event = mock(PlayerMoveEvent.class);
    lenient().when(event.getPlayer()).thenReturn(player);
    module.onMove(event);

    verify(event, never()).setCancelled(true);
  }

  @Test
  void shouldNotCheckBreakWhenDisabled() {
    when(configLoader.isModuleEnabled("anticheat")).thenReturn(false);
    module.onEnable();

    BlockBreakEvent event = mock(BlockBreakEvent.class);
    lenient().when(event.getPlayer()).thenReturn(player);
    module.onBreak(event);

    verify(event, never()).setCancelled(true);
  }
}
