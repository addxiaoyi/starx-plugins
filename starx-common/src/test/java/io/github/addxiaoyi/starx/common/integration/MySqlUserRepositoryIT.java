package io.github.addxiaoyi.starx.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.DatabaseManager;
import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@Testcontainers
class MySqlUserRepositoryIT {

  @Container
  static MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName("starx")
          .withUsername("starx")
          .withPassword("starx_pass");

  @Test
  void shouldPersistAndFindUserInMySql() {
    DatabaseConfig config =
        new DatabaseConfig(
            "mysql",
            mysql.getHost(),
            mysql.getFirstMappedPort(),
            "starx",
            "starx",
            "starx_pass",
            mysql.getJdbcUrl(),
            5,
            5_000L);
    try (DatabaseManager manager = new DatabaseManager(config)) {
      JdbiUserRepository repo = new JdbiUserRepository(manager.getJdbi());

      UUID uuid = UUID.randomUUID();
      StarxUser user =
          new StarxUser(
              uuid,
              "integration_test_user",
              "it@test.com",
              "$2a$10$dummyhash",
              null,
              false,
              Instant.now(),
              null,
              null,
              null,
              null,
              "local",
              "completed",
              null,
              null,
              null,
              null,
              0L,
              null,
              false);

      repo.create(user);

      var found = repo.findByUuid(uuid);
      assertThat(found).isPresent();
      assertThat(found.get().username()).isEqualTo("integration_test_user");
    }
  }
}
