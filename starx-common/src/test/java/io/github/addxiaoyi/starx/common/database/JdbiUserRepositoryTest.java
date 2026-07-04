package io.github.addxiaoyi.starx.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbiUserRepositoryTest {

  private DatabaseManager manager;
  private JdbiUserRepository repository;

  @BeforeEach
  void setUp() {
    String dbName = "test_repo_" + UUID.randomUUID().toString().replace("-", "");
    DatabaseConfig config =
        new DatabaseConfig(
            "h2",
            "",
            0,
            "test",
            "sa",
            "",
            "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1",
            5,
            5_000L);
    manager = new DatabaseManager(config);
    repository = new JdbiUserRepository(manager.getJdbi());
  }

  @AfterEach
  void tearDown() {
    manager.close();
  }

  @Test
  void saveAndFindByUuid() {
    UUID uuid = UUID.randomUUID();
    Instant now = Instant.now();
    StarxUser user =
        new StarxUser(
            uuid, "alice", "alice@example.com", "hash", "secret", true, now, now, "ext-1");

    repository.saveUser(user);

    Optional<UserDto> found = repository.findByUuid(uuid);
    assertThat(found).isPresent();
    assertThat(found.get().username()).isEqualTo("alice");
    assertThat(found.get().email()).isEqualTo("alice@example.com");
    assertThat(found.get().premium()).isTrue();
  }

  @Test
  void findByUsernameReturnsUser() {
    StarxUser user = sampleUser("bob", "bob@example.com");
    repository.saveUser(user);

    Optional<UserDto> found = repository.findByUsername("bob");
    assertThat(found).isPresent();
    assertThat(found.get().email()).isEqualTo("bob@example.com");
  }

  @Test
  void findByEmailReturnsUser() {
    StarxUser user = sampleUser("carol", "carol@example.com");
    repository.saveUser(user);

    Optional<UserDto> found = repository.findByEmail("carol@example.com");
    assertThat(found).isPresent();
    assertThat(found.get().username()).isEqualTo("carol");
  }

  @Test
  void saveDtoPreservesPasswordAndTotp() {
    UUID uuid = UUID.randomUUID();
    StarxUser full =
        new StarxUser(
            uuid,
            "dave",
            "dave@example.com",
            "pw-hash",
            "totp-secret",
            false,
            Instant.now(),
            null,
            null);
    repository.saveUser(full);

    UserDto update =
        UserDto.builder()
            .uuid(uuid)
            .username("dave-updated")
            .email("dave-new@example.com")
            .premium(false)
            .createdAt(Instant.now())
            .build();
    repository.save(update);

    Optional<StarxUser> stored = repository.findFullByUuid(uuid);
    assertThat(stored).isPresent();
    assertThat(stored.get().username()).isEqualTo("dave-updated");
    assertThat(stored.get().passwordHash()).isEqualTo("pw-hash");
    assertThat(stored.get().totpSecret()).isEqualTo("totp-secret");
  }

  @Test
  void deleteRemovesUser() {
    StarxUser user = sampleUser("eve", "eve@example.com");
    repository.saveUser(user);

    repository.delete(user.uuid());

    assertThat(repository.findByUuid(user.uuid())).isEmpty();
  }

  @Test
  void findAllReturnsSavedUsers() {
    repository.saveUser(sampleUser("one", "one@example.com"));
    repository.saveUser(sampleUser("two", "two@example.com"));

    assertThat(repository.findAll()).hasSize(2);
  }

  private StarxUser sampleUser(String username, String email) {
    return new StarxUser(
        UUID.randomUUID(), username, email, "hash", "secret", false, Instant.now(), null, null);
  }
}
