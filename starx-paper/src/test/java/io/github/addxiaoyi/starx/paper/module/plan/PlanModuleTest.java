package io.github.addxiaoyi.starx.paper.module.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.scheduler.SchedulerAdapter;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
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
    module = new PlanModule(plugin, configLoader);
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
      bukkit.when(Bukkit::getOnlinePlayers)
          .thenReturn((Collection) Collections.emptyList());
      bukkit.when(Bukkit::getMaxPlayers).thenReturn(20);

      module.collectStats();
    }

    assertThat(module.getLastStats()).isNotNull();
    assertThat(module.getLastStats()).containsKey("onlinePlayers");
    assertThat(module.getLastStats()).containsKey("maxPlayers");
  }
}