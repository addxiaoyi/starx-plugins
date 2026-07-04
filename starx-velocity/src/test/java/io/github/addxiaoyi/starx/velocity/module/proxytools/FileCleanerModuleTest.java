package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileCleanerModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock Logger logger;

  @TempDir Path tempDir;

  FileCleanerModule.Config enabledConfig;
  FileCleanerModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.logger()).thenReturn(logger);
    enabledConfig = configWithFolders(List.of());
    disabledConfig = disabled();
  }

  private static FileCleanerModule.Config disabled() {
    return new FileCleanerModule.Config() {
      @Override
      public boolean enabled() {
        return false;
      }

      @Override
      public String schedule() {
        return "0 0 * * *";
      }

      @Override
      public List<FileCleanerModule.FolderConfig> folders() {
        return List.of();
      }

      @Override
      public List<FileCleanerModule.FileConfig> files() {
        return List.of();
      }
    };
  }

  private FileCleanerModule.Config configWithFolders(List<FileCleanerModule.FolderConfig> folders) {
    return new FileCleanerModule.Config() {
      @Override
      public boolean enabled() {
        return true;
      }

      @Override
      public String schedule() {
        return "0 0 * * *";
      }

      @Override
      public List<FileCleanerModule.FolderConfig> folders() {
        return folders;
      }

      @Override
      public List<FileCleanerModule.FileConfig> files() {
        return List.of();
      }
    };
  }

  @Test
  void shouldHaveCorrectModuleName() {
    FileCleanerModule module = new FileCleanerModule(plugin, enabledConfig);
    assertThat(module.name()).isEqualTo("proxytools.filecleaner");
  }

  @Test
  void shouldNotInitializeWhenDisabled() {
    FileCleanerModule module = new FileCleanerModule(plugin, disabledConfig);
    module.onEnable();
    assertThat(module.isInitialized()).isFalse();
  }

  @Test
  void shouldInitializeWhenEnabled() {
    FileCleanerModule module = new FileCleanerModule(plugin, enabledConfig);
    module.onEnable();
    assertThat(module.isInitialized()).isTrue();
  }

  @Test
  void shouldCleanUpOnDisable() {
    FileCleanerModule module = new FileCleanerModule(plugin, enabledConfig);
    module.onEnable();
    assertThat(module.isInitialized()).isTrue();
    module.onDisable();
    assertThat(module.isInitialized()).isFalse();
  }

  @Test
  void shouldDeleteOldFilesBasedOnAge() throws IOException {
    Path folder = tempDir.resolve("test-age");
    Files.createDirectory(folder);
    Path oldFile = folder.resolve("old.log");
    Files.writeString(oldFile, "old content");
    Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS)));
    Path newFile = folder.resolve("new.log");
    Files.writeString(newFile, "new content");

    FileCleanerModule.Config ageConfig = configWithFolders(List.of(
        folderConfig(folder.toString(), 5, -1, -1, List.of())));
    FileCleanerModule module = new FileCleanerModule(plugin, ageConfig, tempDir.toFile());
    module.onEnable();
    module.runCleanup();

    assertThat(oldFile.toFile().exists()).isFalse();
    assertThat(newFile.toFile().exists()).isTrue();
    assertThat(module.getFilesDeleted()).isGreaterThanOrEqualTo(1);
  }

  @Test
  void shouldExcludeFilesByRegex() throws IOException {
    Path folder = tempDir.resolve("test-exclude");
    Files.createDirectory(folder);
    Path keepFile = folder.resolve("keep-this.log");
    Files.writeString(keepFile, "keep");
    Files.setLastModifiedTime(keepFile, FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS)));
    Path deleteFile = folder.resolve("delete.log");
    Files.writeString(deleteFile, "delete");
    Files.setLastModifiedTime(deleteFile, FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS)));

    FileCleanerModule.Config excludeConfig = configWithFolders(List.of(
        folderConfig("test-exclude", 5, -1, -1, List.of("keep-.*"))));
    FileCleanerModule module = new FileCleanerModule(plugin, excludeConfig, tempDir.toFile());
    module.onEnable();
    module.runCleanup();

    assertThat(keepFile.toFile().exists()).isTrue();
    assertThat(deleteFile.toFile().exists()).isFalse();
  }

  private static FileCleanerModule.FolderConfig folderConfig(
      String location, int age, int count, long size, List<String> exclude) {
    return new FileCleanerModule.FolderConfig() {
      @Override
      public String location() {
        return location;
      }

      @Override
      public int age() {
        return age;
      }

      @Override
      public int count() {
        return count;
      }

      @Override
      public long size() {
        return size;
      }

      @Override
      public List<String> exclude() {
        return exclude;
      }
    };
  }
}