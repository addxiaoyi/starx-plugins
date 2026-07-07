package io.github.addxiaoyi.starx.common.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.DatabaseManager;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import io.github.addxiaoyi.starx.common.event.LocalEventBus;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

  private static final String VALID_PASS = "secret1";

  private DatabaseManager databaseManager;
  private JdbcUserRepository userRepository;
  private LocalEventBus eventBus;
  private SessionManager sessionManager;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    String dbName = "test_auth_" + UUID.randomUUID().toString().replace("-", "");
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
    userRepository = new JdbcUserRepository(databaseManager.getDataSource());
    eventBus = new LocalEventBus();
    sessionManager = new SessionManager(Duration.ofMinutes(10), () -> Instant.now());
    authService = new AuthService(userRepository, eventBus, sessionManager);
  }

  @AfterEach
  void tearDown() {
    databaseManager.close();
  }

  @Test
  void registerCreatesUserAndPasswordLoginSucceeds() throws Exception {
    UUID uuid = UUID.randomUUID();

    AuthResult registered = authService.register(uuid, "alice", VALID_PASS, "alice@example.com");

    assertThat(registered.success()).isTrue();
    AuthResult loggedIn =
        authService.login(
            uuid, "alice", VALID_PASS, null, InetAddress.getByName("127.0.0.1"), null);
    assertThat(loggedIn.success()).isTrue();
    assertThat(loggedIn.state()).isEqualTo(AuthSession.State.AUTHENTICATED);
  }

  @Test
  void loginWithWrongPasswordFails() throws Exception {
    UUID uuid = UUID.randomUUID();
    authService.register(uuid, "bob", VALID_PASS, null);

    AuthResult result =
        authService.login(uuid, "bob", "wrong1", null, InetAddress.getByName("127.0.0.1"), null);

    assertThat(result.success()).isFalse();
  }

  @Test
  void totpLoginRequiresVerification() throws Exception {
    UUID uuid = UUID.randomUUID();
    String secret = "JBSWY3DPEHPK3PXP";
    authService.register(uuid, "carol", VALID_PASS, null);
    authService.bindTotp(uuid, VALID_PASS, secret);

    AuthResult first =
        authService.login(
            uuid, "carol", VALID_PASS, null, InetAddress.getByName("127.0.0.1"), null);

    assertThat(first.success()).isTrue();
    assertThat(first.state()).isEqualTo(AuthSession.State.AUTHENTICATING);

    String code =
        io.github.addxiaoyi.starx.common.crypto.TotpGenerator.generate(secret, Instant.now());
    AuthResult second = authService.verifyTotp(uuid, code);

    assertThat(second.success()).isTrue();
    assertThat(second.state()).isEqualTo(AuthSession.State.AUTHENTICATED);
  }

  @Test
  void wrongTotpCodeFails() throws Exception {
    UUID uuid = UUID.randomUUID();
    String secret = "JBSWY3DPEHPK3PXP";
    authService.register(uuid, "dave", VALID_PASS, null);
    authService.bindTotp(uuid, VALID_PASS, secret);
    authService.login(uuid, "dave", VALID_PASS, null, InetAddress.getByName("127.0.0.1"), null);

    AuthResult result = authService.verifyTotp(uuid, "000000");

    assertThat(result.success()).isFalse();
  }

  @Test
  void changePasswordInvalidatesOldPassword() throws Exception {
    UUID uuid = UUID.randomUUID();
    authService.register(uuid, "eve", "old123", null);

    AuthResult changed = authService.changePassword(uuid, "old123", "new456");

    assertThat(changed.success()).isTrue();
    assertThat(
            authService
                .login(uuid, "eve", "old123", null, InetAddress.getByName("127.0.0.1"), null)
                .success())
        .isFalse();
    Thread.sleep(2000);
    assertThat(
            authService
                .login(uuid, "eve", "new456", null, InetAddress.getByName("127.0.0.1"), null)
                .success())
        .isTrue();
  }

  @Test
  void loginPublishesSuccessEvent() throws Exception {
    UUID uuid = UUID.randomUUID();
    authService.register(uuid, "frank", VALID_PASS, null);
    List<StarxEvent> events = captureEvents("player:login:success");

    authService.login(uuid, "frank", VALID_PASS, null, InetAddress.getByName("127.0.0.1"), null);

    assertThat(events).hasSize(1);
    assertThat(events.get(0).payload().get("uuid")).isEqualTo(uuid);
  }

  @Test
  void failedLoginPublishesFailedEvent() throws Exception {
    UUID uuid = UUID.randomUUID();
    authService.register(uuid, "grace", VALID_PASS, null);
    List<StarxEvent> events = captureEvents("player:login:failed");

    authService.login(uuid, "grace", "wrong1", null, InetAddress.getByName("127.0.0.1"), null);

    assertThat(events).hasSize(1);
  }

  @Test
  void registerRejectsWeakPassword() {
    UUID uuid = UUID.randomUUID();
    AuthResult result = authService.register(uuid, "test", "12345", null);
    assertThat(result.success()).isFalse();

    result = authService.register(uuid, "test", "abcdef", null);
    assertThat(result.success()).isFalse();

    result = authService.register(uuid, "test", "123456", null);
    assertThat(result.success()).isFalse();
  }

  private List<StarxEvent> captureEvents(String type) {
    List<StarxEvent> events = new ArrayList<>();
    eventBus.subscribe(type, events::add);
    return events;
  }
}
