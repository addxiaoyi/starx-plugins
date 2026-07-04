package io.github.addxiaoyi.starx.paper.module.mapmod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapModModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock Server server;
  @Mock PluginManager pluginManager;
  @Mock Player player;

  Logger logger = Logger.getLogger(MapModModuleTest.class.getName());
  MapModModule module;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
    lenient().when(configLoader.isModuleEnabled("mapmod")).thenReturn(true);
    lenient().when(player.getName()).thenReturn("testPlayer");
    module = new MapModModule(plugin, configLoader);
  }

  @Test
  void shouldRegisterListenerOnEnable() {
    module.onEnable();
    verify(pluginManager).registerEvents(module, plugin);
  }

  @Test
  void shouldTrackPlayerOnJoin() {
    module.onEnable();

    World world = mock(World.class);
    lenient().when(world.getName()).thenReturn("world");
    lenient().when(player.getWorld()).thenReturn(world);

    PlayerJoinEvent event = new PlayerJoinEvent(player, "joined");
    module.onJoin(event);

    assertThat(module.getTrackedPlayers()).containsKey(player.getName());
  }

  @Test
  void shouldBeDisabledWhenConfigDisabled() {
    when(configLoader.isModuleEnabled("mapmod")).thenReturn(false);
    module.onEnable();
    assertThat(module.isEnabled()).isFalse();
  }
}