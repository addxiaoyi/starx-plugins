package io.github.addxiaoyi.starx.common.auth.uniauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.DatabaseManager;
import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UniAuthBridgeTest {

  private DatabaseManager databaseManager;
  private JdbiUserRepository userRepository;
  private UniAuthClient uniAuthClient;
  private UniAuthConfig uniAuthConfig;
  private UniAuthBridge uniAuthBridge;

  @BeforeEach
  void setUp() {
    String dbName = "test_uniauth_" + UUID.randomUUID().toString().replace("-", "");
    DatabaseConfig config =
        new DatabaseConfig(
            "h2",
            "",
            0,
            dbName,
            "sa",
            "",
            "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1",
            5,
            5_000L);
    databaseManager = new DatabaseManager(config);
    userRepository = new JdbiUserRepository(databaseManager.getJdbi());
    uniAuthClient = mock(UniAuthClient.class);
    uniAuthConfig =
        new UniAuthConfig(true, "https://api.example.com/", "test-app", "test-secret", 5000, true);
    uniAuthBridge = new UniAuthBridge(uniAuthConfig, uniAuthClient, userRepository);
  }

  @AfterEach
  void tearDown() {
    databaseManager.close();
  }

  @Test
  void authenticateLocallyWithMigratedUserSucceeds() throws Exception {
    UUID uuid = UUID.randomUUID();
    String password = "validPass123";
    String hashedPassword = io.github.addxiaoyi.starx.common.crypto.PasswordHasher.hash(password);

    StarxUser user =
        new StarxUser(
            uuid,
            "testuser",
            "test@example.com",
            hashedPassword,
            null,
            false,
            Instant.now(),
            null,
            null,
            List.of(),
            null,
            "starvc",
            "completed",
            Instant.now());
    userRepository.saveUser(user);

    UniAuthBridge.BridgeResult result =
        uniAuthBridge.authenticate(uuid, "testuser", password).get();

    assertThat(result.success()).isTrue();
    assertThat(result.user()).isNotNull();
    assertThat(result.user().username()).isEqualTo("testuser");
  }

  @Test
  void authenticateLocallyWithWrongPasswordFails() throws Exception {
    UUID uuid = UUID.randomUUID();
    String hashedPassword =
        io.github.addxiaoyi.starx.common.crypto.PasswordHasher.hash("validPass123");

    StarxUser user =
        new StarxUser(
            uuid,
            "testuser",
            "test@example.com",
            hashedPassword,
            null,
            false,
            Instant.now(),
            null,
            null,
            List.of(),
            null,
            "starvc",
            "completed",
            Instant.now());
    userRepository.saveUser(user);

    UniAuthBridge.BridgeResult result =
        uniAuthBridge.authenticate(uuid, "testuser", "wrongPass").get();

    assertThat(result.success()).isFalse();
  }

  @Test
  void authenticateWithUniAuthAndMigratePendingUser() throws Exception {
    UUID uuid = UUID.randomUUID();
    String password = "validPass123";

    StarxUser user =
        new StarxUser(
            uuid,
            "testuser",
            "test@example.com",
            null,
            null,
            false,
            Instant.now(),
            null,
            null,
            List.of(),
            null,
            "starvc",
            "pending",
            null);
    userRepository.saveUser(user);

    when(uniAuthClient.login(anyString(), anyString()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new UniAuthClient.LoginResponse(true, "Success", "user-1", "test@example.com")));

    UniAuthBridge.BridgeResult result =
        uniAuthBridge.authenticate(uuid, "testuser", password).get();

    assertThat(result.success()).isTrue();
    assertThat(result.user()).isNotNull();

    StarxUser updatedUser = userRepository.findFullByUuid(uuid).get();
    assertThat(updatedUser.migrationState()).isEqualTo("completed");
    assertThat(updatedUser.passwordHash()).isNotNull();
    assertThat(updatedUser.passwordMigratedAt()).isNotNull();
  }

  @Test
  void authenticateWithUniAuthAndCreateNewUser() throws Exception {
    UUID uuid = UUID.randomUUID();
    String password = "validPass123";

    when(uniAuthClient.login(anyString(), anyString()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new UniAuthClient.LoginResponse(true, "Success", "user-1", "newuser@example.com")));

    UniAuthBridge.BridgeResult result = uniAuthBridge.authenticate(uuid, "newuser", password).get();

    assertThat(result.success()).isTrue();
    assertThat(result.user()).isNotNull();
    assertThat(result.user().username()).isEqualTo("newuser");
    assertThat(result.user().sourceSystem()).isEqualTo("starvc");
    assertThat(result.user().migrationState()).isEqualTo("completed");

    StarxUser storedUser = userRepository.findFullByUuid(uuid).get();
    assertThat(storedUser).isNotNull();
  }

  @Test
  void authenticateWithUniAuthFailsWithWrongPassword() throws Exception {
    UUID uuid = UUID.randomUUID();

    when(uniAuthClient.login(anyString(), anyString()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new UniAuthClient.LoginResponse(false, "Invalid credentials", null, null)));

    UniAuthBridge.BridgeResult result =
        uniAuthBridge.authenticate(uuid, "testuser", "wrongPass").get();

    assertThat(result.success()).isFalse();
  }
}
