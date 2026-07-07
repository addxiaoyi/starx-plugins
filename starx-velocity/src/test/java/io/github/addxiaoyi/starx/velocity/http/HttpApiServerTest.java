package io.github.addxiaoyi.starx.velocity.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.github.addxiaoyi.starx.common.auth.BindingVerificationService;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.crypto.HmacSigner;
import io.github.addxiaoyi.starx.common.database.JdbcAnnouncementRepository;
import io.github.addxiaoyi.starx.common.database.JdbcBindingRepository;
import io.github.addxiaoyi.starx.common.database.JdbcPunishmentRepository;
import io.github.addxiaoyi.starx.common.database.JdbcReportRepository;
import io.github.addxiaoyi.starx.common.database.JdbcStaffNoteRepository;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import io.github.addxiaoyi.starx.common.database.JdbcVoteRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class HttpApiServerTest {

  private static final int TEST_PORT = 18788;
  private static final String API_KEY = "secret";

  @Mock SkinBridgeModule skinBridge;
  @Mock ProxyServer proxy;
  @Mock AuthService authService;
  @Mock JdbcUserRepository jdbiUserRepository;

  private JdbcPunishmentRepository punishmentRepo;
  private JdbcStaffNoteRepository staffNoteRepo;
  private JdbcReportRepository reportRepo;
  private JdbcAnnouncementRepository announcementRepo;
  private JdbcBindingRepository bindingRepo;
  private JdbcVoteRepository voteRepo;
  private BindingVerificationService bindingVerification;

  private AutoCloseable mocks;
  private HttpApiServer server;
  private HttpClient client;
  private EventBus eventBus;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    punishmentRepo = Mockito.mock(JdbcPunishmentRepository.class, Mockito.RETURNS_DEFAULTS);
    staffNoteRepo = Mockito.mock(JdbcStaffNoteRepository.class, Mockito.RETURNS_DEFAULTS);
    reportRepo = Mockito.mock(JdbcReportRepository.class, Mockito.RETURNS_DEFAULTS);
    announcementRepo = Mockito.mock(JdbcAnnouncementRepository.class, Mockito.RETURNS_DEFAULTS);
    bindingRepo = Mockito.mock(JdbcBindingRepository.class, Mockito.RETURNS_DEFAULTS);
    voteRepo = Mockito.mock(JdbcVoteRepository.class, Mockito.RETURNS_DEFAULTS);
    bindingVerification = new BindingVerificationService();
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    eventBus = new VelocityEventBus();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  void shouldReturn503WhenApiKeyNotConfigured() throws Exception {
    server =
        new HttpApiServer(
            configWithApiKey(null),
            eventBus,
            proxy,
            jdbiUserRepository,
            authService,
            skinBridge,
            punishmentRepo,
            staffNoteRepo,
            reportRepo,
            announcementRepo,
            bindingRepo,
            bindingVerification,
            voteRepo);
    server.start();

    HttpResponse<String> response = get("/v1/health", null);

    assertThat(response.statusCode()).isEqualTo(503);
    assertThat(response.body()).contains("API key not configured");
  }

  @Test
  void shouldReturn401WhenApiKeyIsWrong() throws Exception {
    server =
        new HttpApiServer(
            configWithApiKey(API_KEY),
            eventBus,
            proxy,
            jdbiUserRepository,
            authService,
            skinBridge,
            punishmentRepo,
            staffNoteRepo,
            reportRepo,
            announcementRepo,
            bindingRepo,
            bindingVerification,
            voteRepo);
    server.start();

    HttpResponse<String> response = get("/v1/health", "wrong");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void shouldReturn200ForHealthWithValidKey() throws Exception {
    server =
        new HttpApiServer(
            configWithApiKey(API_KEY),
            eventBus,
            proxy,
            jdbiUserRepository,
            authService,
            skinBridge,
            punishmentRepo,
            staffNoteRepo,
            reportRepo,
            announcementRepo,
            bindingRepo,
            bindingVerification,
            voteRepo);
    server.start();

    HttpResponse<String> response = get("/v1/health", API_KEY);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("OK");
  }

  @Test
  void shouldAuthenticateWithValidHmacSignature() throws Exception {
    server =
        new HttpApiServer(
            configWithApiKey(API_KEY),
            eventBus,
            proxy,
            jdbiUserRepository,
            authService,
            skinBridge,
            punishmentRepo,
            staffNoteRepo,
            reportRepo,
            announcementRepo,
            bindingRepo,
            bindingVerification,
            voteRepo);
    server.start();

    String timestamp = String.valueOf(System.currentTimeMillis());
    String signature = HmacSigner.sign(API_KEY, "");

    HttpResponse<String> response = getHmac("/v1/health", timestamp, signature);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("OK");
  }

  @Test
  void shouldReturn401ForInvalidHmacSignature() throws Exception {
    server =
        new HttpApiServer(
            configWithApiKey(API_KEY),
            eventBus,
            proxy,
            jdbiUserRepository,
            authService,
            skinBridge,
            punishmentRepo,
            staffNoteRepo,
            reportRepo,
            announcementRepo,
            bindingRepo,
            bindingVerification,
            voteRepo);
    server.start();

    HttpResponse<String> response = getHmac("/v1/health", "123", "invalid-signature");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void shouldReturnUserExists() throws Exception {
    when(jdbiUserRepository.existsByUsername("test")).thenReturn(true);

    server =
        new HttpApiServer(
            configWithApiKey(API_KEY),
            eventBus,
            proxy,
            jdbiUserRepository,
            authService,
            skinBridge,
            punishmentRepo,
            staffNoteRepo,
            reportRepo,
            announcementRepo,
            bindingRepo,
            bindingVerification,
            voteRepo);
    server.start();

    HttpResponse<String> response = get("/v1/user/exists?name=test", API_KEY);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"exists\":true");
  }

  @Test
  void shouldRefreshSkinWhenValidRequest() throws Exception {
    UUID uuid = UUID.randomUUID();
    StarxUser user =
        new StarxUser(
            uuid,
            "alice",
            null,
            null,
            null,
            false,
            Instant.now(),
            null,
            null,
            List.of(),
            null,
            null,
            "completed",
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    when(jdbiUserRepository.findFullByUsername("alice")).thenReturn(Optional.of(user));

    server =
        new HttpApiServer(
            configWithApiKey(API_KEY),
            eventBus,
            proxy,
            jdbiUserRepository,
            authService,
            skinBridge,
            punishmentRepo,
            staffNoteRepo,
            reportRepo,
            announcementRepo,
            bindingRepo,
            bindingVerification,
            voteRepo);
    server.start();

    HttpResponse<String> response =
        post("/v1/admin/skin-refresh", API_KEY, "{\"username\":\"alice\"}");

    assertThat(response.statusCode()).isEqualTo(200);
  }

  private StarxConfig configWithApiKey(String apiKey) {
    return new StarxConfig(
        apiKey,
        new StarxConfig.HttpConfig("127.0.0.1", TEST_PORT),
        new StarxConfig.WebhookConfig("", ""),
        DatabaseConfig.defaults(),
        io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthConfig.defaults(),
        StarxConfig.NapcatConfig.defaults(),
        java.util.Map.of());
  }

  private HttpResponse<String> get(String path, String apiKey)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + TEST_PORT + path));
    if (apiKey != null) {
      builder.header(HttpApiServer.API_KEY_HEADER, apiKey);
    }
    return client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> getHmac(String path, String timestamp, String signature)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + TEST_PORT + path))
            .header(HttpApiServer.TIMESTAMP_HEADER, timestamp)
            .header(HttpApiServer.SIGNATURE_HEADER, signature)
            .GET()
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, String apiKey, String body)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + TEST_PORT + path))
            .header("Content-Type", "application/json")
            .header(HttpApiServer.API_KEY_HEADER, apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
