package io.github.addxiaoyi.starx.paper.module.filecleaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileCleanerModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock Server server;
  @Mock PluginManager pluginManager;

  Logger logger = Logger.getLogger(FileCleanerModuleTest.class.getName());
  FileCleanerModule module;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    File dataFolder = new File(tempDir.toFile(), "plugins/starx");
    dataFolder.mkdirs();
    lenient().when(plugin.getDataFolder()).thenReturn(dataFolder);
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
    lenient().when(configLoader.isModuleEnabled("filecleaner")).thenReturn(true);
    module = new FileCleanerModule(plugin, configLoader);
  }

  @Test
  void shouldHaveCorrectModuleName() {
    assertThat(module.getName()).isEqualTo("filecleaner");
  }

  @Test
  void shouldRegisterListenerOnEnable() {
    module.onEnable();
    verify(pluginManager).registerEvents(module, plugin);
  }

  @Test
  void shouldDeleteOldLogFiles() throws IOException {
    Path logDir = tempDir.resolve("logs");
    Files.createDirectory(logDir);
    Path oldFile = logDir.resolve("2026-06-01-1.log.gz");
    Files.writeString(oldFile, "old");
    Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS)));
    Path recentFile = logDir.resolve("2026-07-03-1.log.gz");
    Files.writeString(recentFile, "recent");

    module.onEnable();
    module.runCleanup();

    assertThat(oldFile.toFile().exists()).isFalse();
    assertThat(recentFile.toFile().exists()).isTrue();
  }

  @Test
  void shouldNotDeleteWhenDisabled() throws IOException {
    lenient().when(configLoader.isModuleEnabled("filecleaner")).thenReturn(false);
    module = new FileCleanerModule(plugin, configLoader);

    Path logDir = tempDir.resolve("logs");
    Files.createDirectory(logDir);
    Path oldFile = logDir.resolve("old.log");
    Files.writeString(oldFile, "old");
    Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS)));

    module.onEnable();
    module.runCleanup();

    assertThat(oldFile.toFile().exists()).isTrue();
  }
}