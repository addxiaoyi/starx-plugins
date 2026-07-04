package io.github.addxiaoyi.starx.paper.module.qq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.util.logging.Logger;
import org.bukkit.advancement.Advancement;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QqModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock Server server;
  @Mock PluginManager pluginManager;
  @Mock Player player;

  Logger logger = Logger.getLogger(QqModuleTest.class.getName());
  QqModule module;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
    lenient().when(configLoader.isModuleEnabled("qq")).thenReturn(true);
    lenient().when(player.getName()).thenReturn("testPlayer");
    module = new QqModule(plugin, configLoader);
  }

  @Test
  void shouldRegisterListenerOnEnable() {
    module.onEnable();
    verify(pluginManager).registerEvents(module, plugin);
  }

  @Test
  void shouldQueueNotifyOnDeath() {
    module.onEnable();

    PlayerDeathEvent event = mock(PlayerDeathEvent.class);
    lenient().when(event.getEntity()).thenReturn(player);
    lenient().when(event.getDeathMessage()).thenReturn("testPlayer died");
    module.onDeath(event);

    assertThat(module.getPendingNotifications()).hasSize(1);
  }

  @Test
  void shouldQueueNotifyOnAdvancement() {
    module.onEnable();

    Advancement advancement = mock(Advancement.class);
    lenient().when(advancement.getKey()).thenReturn(org.bukkit.NamespacedKey.minecraft("test"));
    PlayerAdvancementDoneEvent event = mock(PlayerAdvancementDoneEvent.class);
    lenient().when(event.getPlayer()).thenReturn(player);
    lenient().when(event.getAdvancement()).thenReturn(advancement);
    module.onAdvancement(event);

    assertThat(module.getPendingNotifications()).hasSize(1);
  }
}