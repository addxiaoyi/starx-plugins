package io.github.addxiaoyi.starx.paper.module.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.messaging.PaperMessageBridge;
import io.github.addxiaoyi.starx.paper.scheduler.SchedulerAdapter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock PaperMessageBridge messageBridge;
  @Mock Server server;
  @Mock PluginManager pluginManager;
  @Mock SchedulerAdapter scheduler;

  Logger logger = Logger.getLogger(PlanModuleTest.class.getName());
  PlanModule module;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
    lenient().when(plugin.getSchedulerAdapter()).thenReturn(scheduler);
    lenient().when(configLoader.isModuleEnabled("plan")).thenReturn(true);
    module = new PlanModule(plugin, configLoader, messageBridge);
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
  void shouldCollectServerStats() {
    module.onEnable();

    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getOnlinePlayers).thenReturn((Collection) Collections.emptyList());
      bukkit.when(Bukkit::getMaxPlayers).thenReturn(20);
      bukkit.when(Bukkit::getTPS).thenReturn(new double[] {19.5, 18.0, 17.0});
      bukkit.when(Bukkit::getWorlds).thenReturn(Collections.emptyList());

      module.collectStats();
    }

    assertThat(module.getLastStats()).isNotNull();
    assertThat(module.getLastStats())
        .containsKeys(
            "onlinePlayers",
            "maxPlayers",
            "tps",
            "usedMemory",
            "maxMemory",
            "loadedChunks",
            "entities",
            "timestamp");
    assertThat(module.getLastStats().get("onlinePlayers")).isEqualTo(0);
    assertThat(module.getLastStats().get("maxPlayers")).isEqualTo(20);
    assertThat(module.getLastStats().get("tps")).isEqualTo(19.5);
  }

  @Test
  void shouldSendStatsViaPluginMessage() {
    module.onEnable();
    Player onlinePlayer = mock(Player.class);

    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getOnlinePlayers).thenReturn((Collection) List.of(onlinePlayer));
      bukkit.when(Bukkit::getMaxPlayers).thenReturn(20);
      bukkit.when(Bukkit::getTPS).thenReturn(new double[] {20.0});
      bukkit.when(Bukkit::getWorlds).thenReturn(Collections.emptyList());

      module.collectAndSend();
    }

    ArgumentCaptor<PluginMessage> captor = ArgumentCaptor.forClass(PluginMessage.class);
    verify(messageBridge).send(any(Player.class), captor.capture());
    assertThat(captor.getValue().command()).isEqualTo(PluginMessageChannels.CMD_PLAN_STATS);
    assertThat(captor.getValue().payload()).containsKeys("onlinePlayers", "tps", "usedMemory");
  }

  @Test
  void shouldNotSendWhenDisabled() {
    lenient().when(configLoader.isModuleEnabled("plan")).thenReturn(false);
    module.onEnable();
    module.collectAndSend();
    verify(messageBridge, never()).send(any(), any());
  }

  @Test
  void shouldHandlePlanStatsCommand() {
    module.onEnable();
    Player onlinePlayer = mock(Player.class);
    PluginMessage request =
        new PluginMessage(PluginMessageChannels.CMD_PLAN_STATS, Collections.emptyMap());

    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getOnlinePlayers).thenReturn((Collection) List.of(onlinePlayer));
      bukkit.when(Bukkit::getMaxPlayers).thenReturn(20);
      bukkit.when(Bukkit::getTPS).thenReturn(new double[] {20.0});
      bukkit.when(Bukkit::getWorlds).thenReturn(Collections.emptyList());

      module.onPluginMessage(request);
    }

    verify(messageBridge).send(any(Player.class), any(PluginMessage.class));
  }

  @Test
  void shouldIgnoreOtherCommands() {
    module.onEnable();
    PluginMessage other = new PluginMessage("other_command", Collections.emptyMap());
    module.onPluginMessage(other);
    verify(messageBridge, never()).send(any(), any());
  }

  @Test
  void shouldIncludeChunkAndEntityCounts() {
    World world = mock(World.class);
    lenient().when(world.getLoadedChunks()).thenReturn(new org.bukkit.Chunk[150]);
    lenient()
        .when(world.getEntities())
        .thenReturn(
            List.of(mock(org.bukkit.entity.Entity.class), mock(org.bukkit.entity.Entity.class)));

    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getOnlinePlayers).thenReturn((Collection) Collections.emptyList());
      bukkit.when(Bukkit::getMaxPlayers).thenReturn(20);
      bukkit.when(Bukkit::getTPS).thenReturn(new double[] {20.0});
      bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));

      module.collectStats();
    }

    assertThat(module.getLastStats().get("loadedChunks")).isEqualTo(150);
    assertThat(module.getLastStats().get("entities")).isEqualTo(2);
  }
}
