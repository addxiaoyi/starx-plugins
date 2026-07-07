package io.github.addxiaoyi.starx.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DatabaseManagerTest {

  @Test
  void h2InMemoryDatabaseMigratesAndProvidesDataSource() {
    DatabaseConfig config =
        new DatabaseConfig(
            "h2", "", 0, "test", "sa", "", "jdbc:h2:mem:test_manager;DB_CLOSE_DELAY=-1", 5, 5_000L);

    try (DatabaseManager manager = new DatabaseManager(config)) {
      assertThat(manager.getDataSource()).isNotNull();
    }
  }

  @Test
  void sqliteDatabaseMigratesAndProvidesDataSource() throws Exception {
    Path tempFile = Files.createTempFile("starx_test", ".db");
    tempFile.toFile().deleteOnExit();

    DatabaseConfig config =
        new DatabaseConfig("sqlite", "", 0, tempFile.toString(), "", "", "", 2, 5_000L);

    try (DatabaseManager manager = new DatabaseManager(config)) {
      assertThat(manager.getDataSource()).isNotNull();
    }
  }
}
