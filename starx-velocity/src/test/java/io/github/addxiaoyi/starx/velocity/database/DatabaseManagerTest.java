package io.github.addxiaoyi.starx.velocity.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatabaseManagerTest {

  private DatabaseManager databaseManager;

  private static DatabaseConfig h2Config() {
    return new DatabaseConfig(
        "h2", "", 0, "test", "sa", "", "jdbc:h2:mem:test_velocity;DB_CLOSE_DELAY=-1", 5, 5_000L);
  }

  @AfterEach
  void tearDown() {
    if (databaseManager != null) {
      databaseManager.close();
    }
  }

  @Test
  @DisplayName("should create DatabaseManager with H2 in-memory database")
  void shouldCreateWithH2() {
    DatabaseConfig config = h2Config();
    databaseManager = new DatabaseManager(config);

    assertThat(databaseManager).isNotNull();
    assertThat(databaseManager.isOpen()).isTrue();
  }

  @Test
  @DisplayName("should provide JdbcUserRepository")
  void shouldProvideJdbcUserRepository() {
    DatabaseConfig config = h2Config();
    databaseManager = new DatabaseManager(config);

    JdbcUserRepository repo = databaseManager.getUserRepository();

    assertThat(repo).isNotNull();
  }

  @Test
  @DisplayName("should save and find user by UUID")
  void shouldSaveAndFindByUuid() {
    DatabaseConfig config = h2Config();
    databaseManager = new DatabaseManager(config);
    JdbcUserRepository repo = databaseManager.getUserRepository();

    UUID uuid = UUID.randomUUID();
    UserDto user =
        UserDto.builder()
            .uuid(uuid)
            .username("testuser")
            .email("test@example.com")
            .premium(false)
            .createdAt(Instant.now())
            .build();

    repo.save(user);

    assertThat(repo.findByUuid(uuid)).isPresent();
    assertThat(repo.findByUuid(uuid).get().username()).isEqualTo("testuser");
  }

  @Test
  @DisplayName("should find user by username")
  void shouldFindByUsername() {
    DatabaseConfig config = h2Config();
    databaseManager = new DatabaseManager(config);
    JdbcUserRepository repo = databaseManager.getUserRepository();

    UUID uuid = UUID.randomUUID();
    UserDto user =
        UserDto.builder()
            .uuid(uuid)
            .username("alice")
            .email("alice@example.com")
            .premium(true)
            .createdAt(Instant.now())
            .build();

    repo.save(user);

    assertThat(repo.findByUsername("alice")).isPresent();
    assertThat(repo.findByUsername("alice").get().uuid()).isEqualTo(uuid);
  }

  @Test
  @DisplayName("should find user by email")
  void shouldFindByEmail() {
    DatabaseConfig config = h2Config();
    databaseManager = new DatabaseManager(config);
    JdbcUserRepository repo = databaseManager.getUserRepository();

    UUID uuid = UUID.randomUUID();
    UserDto user =
        UserDto.builder()
            .uuid(uuid)
            .username("bob")
            .email("bob@example.com")
            .premium(false)
            .createdAt(Instant.now())
            .build();

    repo.save(user);

    assertThat(repo.findByEmail("bob@example.com")).isPresent();
    assertThat(repo.findByEmail("bob@example.com").get().username()).isEqualTo("bob");
  }

  @Test
  @DisplayName("should check existence by username")
  void shouldCheckExistenceByUsername() {
    DatabaseConfig config = h2Config();
    databaseManager = new DatabaseManager(config);
    JdbcUserRepository repo = databaseManager.getUserRepository();

    UUID uuid = UUID.randomUUID();
    UserDto user =
        UserDto.builder()
            .uuid(uuid)
            .username("exists")
            .email("exists@example.com")
            .premium(false)
            .createdAt(Instant.now())
            .build();

    repo.save(user);

    assertThat(repo.existsByUsername("exists")).isTrue();
    assertThat(repo.existsByUsername("nonexistent")).isFalse();
  }

  @Test
  @DisplayName("should delete user")
  void shouldDeleteUser() {
    DatabaseConfig config = h2Config();
    databaseManager = new DatabaseManager(config);
    JdbcUserRepository repo = databaseManager.getUserRepository();

    UUID uuid = UUID.randomUUID();
    UserDto user =
        UserDto.builder()
            .uuid(uuid)
            .username("deletable")
            .email("del@example.com")
            .premium(false)
            .createdAt(Instant.now())
            .build();

    repo.save(user);
    assertThat(repo.findByUuid(uuid)).isPresent();

    repo.delete(uuid);

    assertThat(repo.findByUuid(uuid)).isEmpty();
  }

  @Test
  @DisplayName("should close database connection")
  void shouldCloseDatabase() {
    DatabaseConfig config = h2Config();
    databaseManager = new DatabaseManager(config);

    assertThat(databaseManager.isOpen()).isTrue();

    databaseManager.close();

    assertThat(databaseManager.isOpen()).isFalse();
  }

  @Test
  @DisplayName("should throw when using closed database")
  void shouldThrowWhenClosed() {
    DatabaseConfig config = h2Config();
    databaseManager = new DatabaseManager(config);
    JdbcUserRepository repo = databaseManager.getUserRepository();

    databaseManager.close();

    assertThrows(Exception.class, () -> repo.findByUuid(UUID.randomUUID()));
  }
}
