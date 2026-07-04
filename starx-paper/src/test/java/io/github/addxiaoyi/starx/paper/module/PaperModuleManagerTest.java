package io.github.addxiaoyi.starx.paper.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaperModuleManagerTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock Server server;
  @Mock PluginManager pluginManager;
  Logger logger = Logger.getLogger(PaperModuleManagerTest.class.getName());

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
  }

  @Test
  void loadsMaintenanceAndChat_whenEnabled() {
    lenient().when(configLoader.isModuleEnabled("maintenance")).thenReturn(true);
    lenient().when(configLoader.isModuleEnabled("chat")).thenReturn(true);

    PaperModuleManager manager = new PaperModuleManager(plugin, configLoader);
    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
      when(pluginManager.getPlugin("SkinsRestorer")).thenReturn(null);
      manager.loadModules();
    }

    assertThat(manager.getModules())
        .hasSize(2)
        .extracting(PaperModule::getName)
        .containsExactlyInAnyOrder("maintenance", "chat");
  }

  @Test
  void loadsSkinModule_whenSkinsRestorerPresentAndEnabled() {
    lenient().when(configLoader.isModuleEnabled("maintenance")).thenReturn(false);
    lenient().when(configLoader.isModuleEnabled("chat")).thenReturn(false);
    lenient().when(configLoader.isModuleEnabled("skin")).thenReturn(true);

    PaperModuleManager manager = new PaperModuleManager(plugin, configLoader);
    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
      when(pluginManager.getPlugin("SkinsRestorer")).thenReturn(mock(Plugin.class));
      manager.loadModules();
    }

    assertThat(manager.getModules()).extracting(PaperModule::getName).contains("skin");
  }

  @Test
  void doesNotLoadSkinModule_whenSkinsRestorerMissing() {
    lenient().when(configLoader.isModuleEnabled("maintenance")).thenReturn(false);
    lenient().when(configLoader.isModuleEnabled("chat")).thenReturn(false);

    PaperModuleManager manager = new PaperModuleManager(plugin, configLoader);
    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
      when(pluginManager.getPlugin("SkinsRestorer")).thenReturn(null);
      manager.loadModules();
    }

    assertThat(manager.getModules()).extracting(PaperModule::getName).doesNotContain("skin");
  }
}