package io.github.addxiaoyi.starx.velocity.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.github.addxiaoyi.starx.common.auth.BindingVerificationService;
import io.github.addxiaoyi.starx.common.crypto.HmacSigner;
import io.github.addxiaoyi.starx.common.database.JdbcAnnouncementRepository;
import io.github.addxiaoyi.starx.common.database.JdbcBindingRepository;
import io.github.addxiaoyi.starx.common.database.JdbcPunishmentRepository;
import io.github.addxiaoyi.starx.common.database.JdbcReportRepository;
import io.github.addxiaoyi.starx.common.database.JdbcStaffNoteRepository;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import io.github.addxiaoyi.starx.common.database.JdbcVoteRepository;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.http.admin.*;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpApiServer implements RouteRegistrar {

  public static final String API_KEY_HEADER = "X-API-Key";
  public static final String SIGNATURE_HEADER = "X-StarX-Signature";
  public static final String TIMESTAMP_HEADER = "X-StarX-Timestamp";

  private static final Logger log = LoggerFactory.getLogger(HttpApiServer.class);

  private final StarxConfig config;
  private final EventBus eventBus;
  private final ProxyServer proxy;
  private final JdbcUserRepository userRepository;
  private final AuthService authService;
  private final SkinBridgeModule skinBridge;
  private final JdbcPunishmentRepository punishmentRepo;
  private final JdbcStaffNoteRepository staffNoteRepo;
  private final JdbcReportRepository reportRepo;
  private final JdbcAnnouncementRepository announcementRepo;
  private final JdbcBindingRepository bindingRepo;
  private final BindingVerificationService bindingVerification;
  private final JdbcVoteRepository voteRepo;
  private final Map<String, Map<String, RouteHandler>> routes = new HashMap<>();
  private HttpServer server;

  public HttpApiServer(
      StarxConfig config,
      EventBus eventBus,
      ProxyServer proxy,
      JdbcUserRepository userRepository,
      AuthService authService,
      SkinBridgeModule skinBridge,
      JdbcPunishmentRepository punishmentRepo,
      JdbcStaffNoteRepository staffNoteRepo,
      JdbcReportRepository reportRepo,
      JdbcAnnouncementRepository announcementRepo,
      JdbcBindingRepository bindingRepo,
      BindingVerificationService bindingVerification,
      JdbcVoteRepository voteRepo) {
    this.config = Objects.requireNonNull(config, "config");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.proxy = Objects.requireNonNull(proxy, "proxy");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    this.authService = Objects.requireNonNull(authService, "authService");
    this.skinBridge = skinBridge;
    this.punishmentRepo = Objects.requireNonNull(punishmentRepo, "punishmentRepo");
    this.staffNoteRepo = Objects.requireNonNull(staffNoteRepo, "staffNoteRepo");
    this.reportRepo = Objects.requireNonNull(reportRepo, "reportRepo");
    this.announcementRepo = Objects.requireNonNull(announcementRepo, "announcementRepo");
    this.bindingRepo = Objects.requireNonNull(bindingRepo, "bindingRepo");
    this.bindingVerification = Objects.requireNonNull(bindingVerification, "bindingVerification");
    this.voteRepo = Objects.requireNonNull(voteRepo, "voteRepo");
  }

  public void start() throws IOException {
    server =
        HttpServer.create(
            new InetSocketAddress(config.http().bind(), config.http().port()), 0);
    server.setExecutor(Executors.newCachedThreadPool());

    get("/v1/health", this::health);
    new UserQueryHandler(userRepository).register(this);
    new SkinRefreshHandler(skinBridge, userRepository).register(this);
    new PasswordResetHandler(authService).register(this);
    new BindEmailHandler(authService).register(this);
    new BanHandler(userRepository, eventBus).register(this);
    new KickHandler(proxy, eventBus).register(this);
    new DeleteUserHandler(authService).register(this);
    new LinkExternalUserHandler(userRepository, eventBus).register(this);
    new PunishmentHandler(punishmentRepo).register(this);
    new StaffNoteHandler(staffNoteRepo).register(this);
    new ReportHandler(reportRepo).register(this);
    new AnnouncementHandler(announcementRepo).register(this);
    new BindingHandler(bindingRepo, userRepository, bindingVerification).register(this);
    new VoteHandler(voteRepo).register(this);

    server.start();
    log.info("HTTP API server started on {}:{}", config.http().bind(), config.http().port());
  }

  public void stop() {
    if (server != null) {
      server.stop(0);
      log.info("HTTP API server stopped");
    }
  }

  @Override
  public void get(String path, RouteHandler handler) {
    register(path, "GET", handler);
  }

  @Override
  public void post(String path, RouteHandler handler) {
    register(path, "POST", handler);
  }

  private void register(String path, String method, RouteHandler handler) {
    Map<String, RouteHandler> methods = routes.computeIfAbsent(path, k -> new HashMap<>());
    methods.put(method, handler);
    if (methods.size() == 1) {
      server.createContext(path, this::dispatch);
    }
  }

  private void dispatch(HttpExchange exchange) {
    String path = exchange.getRequestURI().getPath();
    String method = exchange.getRequestMethod().toUpperCase();
    Map<String, RouteHandler> methods = routes.get(path);
    RouteHandler handler = methods != null ? methods.get(method) : null;
    if (handler == null) {
      try { exchange.sendResponseHeaders(405, -1); } catch (IOException ignored) {}
      return;
    }
    JsonHttpExchange ctx = new JsonHttpExchange(exchange);
    try {
      if (!authFilter(ctx)) return;
      handler.handle(ctx);
    } catch (Exception e) {
      log.error("Error handling {} {}", method, path, e);
      try { ctx.status(500).sendError(500, "Internal Server Error"); } catch (IOException ignored) {}
    }
  }

  private boolean authFilter(JsonHttpExchange ctx) throws IOException {
    String apiKey = config.apiKey();
    if (apiKey == null || apiKey.isBlank()) {
      ctx.status(503).result("API key not configured");
      return false;
    }

    String signature = ctx.header(SIGNATURE_HEADER);
    String timestamp = ctx.header(TIMESTAMP_HEADER);
    if (signature != null && !signature.isBlank() && timestamp != null && !timestamp.isBlank()) {
      if (HmacSigner.verify(apiKey, ctx.bodyString(), signature)) {
        return true;
      }
    }

    String provided = ctx.header(API_KEY_HEADER);
    if (apiKey.equals(provided)) {
      return true;
    }

    ctx.status(401).result("Unauthorized");
    return false;
  }

  private void health(JsonHttpExchange ctx) throws IOException {
    ctx.status(200).result("OK");
  }

}
