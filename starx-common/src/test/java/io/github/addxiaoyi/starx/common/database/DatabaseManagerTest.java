package io.github.addxiaoyi.starx.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class DatabaseManagerTest {

  @Test
  void h2InMemoryDatabaseMigratesAndProvidesJdbi() {
    DatabaseConfig config =
        new DatabaseConfig(
            "h2", "", 0, "test", "sa", "", "jdbc:h2:mem:test_manager;DB_CLOSE_DELAY=-1", 5, 5_000L);

    try (DatabaseManager manager = new DatabaseManager(config)) {
      Jdbi jdbi = manager.getJdbi();
      assertThat(jdbi).isNotNull();

      Integer result =
          jdbi.withHandle(handle -> handle.createQuery("SELECT 1").mapTo(Integer.class).one());
      assertThat(result).isEqualTo(1);

      assertThat(manager.getDataSource()).isNotNull();
    }
  }
}
