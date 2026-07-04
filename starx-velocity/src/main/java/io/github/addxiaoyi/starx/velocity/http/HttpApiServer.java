package io.github.addxiaoyi.starx.velocity.http;

import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.github.addxiaoyi.starx.common.crypto.HmacSigner;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.http.admin.BanHandler;
import io.github.addxiaoyi.starx.velocity.http.admin.BindEmailHandler;
import io.github.addxiaoyi.starx.velocity.http.admin.DeleteUserHandler;
import io.github.addxiaoyi.starx.velocity.http.admin.KickHandler;
import io.github.addxiaoyi.starx.velocity.http.admin.LinkExternalUserHandler;
import io.github.addxiaoyi.starx.velocity.http.admin.PasswordResetHandler;
import io.github.addxiaoyi.starx.velocity.http.admin.SkinRefreshHandler;
import io.github.addxiaoyi.starx.velocity.http.admin.UserQueryHandler;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Objects;

/** 基于 Javalin 的内部 HTTP API 服务器。 */
public final class HttpApiServer {

  public static final String API_KEY_HEADER = "X-API-Key";
  public static final String SIGNATURE_HEADER = "X-StarX-Signature";
  public static final String TIMESTAMP_HEADER = "X-StarX-Timestamp";

  private final StarxConfig config;
  private final EventBus eventBus;
  private final ProxyServer proxy;
  private final UserRepository userRepository;
  private final SkinBridgeModule skinBridge;
  private Javalin app;

  public HttpApiServer(
      StarxConfig config,
      EventBus eventBus,
      ProxyServer proxy,
      UserRepository userRepository,
      SkinBridgeModule skinBridge) {
    this.config = Objects.requireNonNull(config, "config");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.proxy = Objects.requireNonNull(proxy, "proxy");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    this.skinBridge = skinBridge;
  }

  public void start() {
    app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner = false);
    app.before(this::authFilter);
    app.get("/v1/health", this::health);

    new UserQueryHandler(userRepository).register(app);
    new SkinRefreshHandler(userRepository, eventBus).register(app);
    new PasswordResetHandler(userRepository, eventBus).register(app);
    new BindEmailHandler(userRepository, eventBus).register(app);
    new BanHandler(userRepository, eventBus).register(app);
    new KickHandler(proxy, eventBus).register(app);
    new DeleteUserHandler(userRepository).register(app);
    new LinkExternalUserHandler(userRepository, eventBus).register(app);

    app.start(config.http().bind(), config.http().port());
  }

  public void stop() {
    if (app != null) {
      app.stop();
    }
  }

  private void authFilter(Context ctx) {
    String apiKey = config.apiKey();
    if (apiKey == null || apiKey.isBlank()) {
      ctx.status(503).result("API key not configured");
      ctx.skipRemainingHandlers();
      return;
    }

    String signature = ctx.header(SIGNATURE_HEADER);
    String timestamp = ctx.header(TIMESTAMP_HEADER);
    if (signature != null && !signature.isBlank() && timestamp != null && !timestamp.isBlank()) {
      if (HmacSigner.verify(apiKey, timestamp, ctx.body(), signature)) {
        return;
      }
    }

    String provided = ctx.header(API_KEY_HEADER);
    if (apiKey.equals(provided)) {
      return;
    }

    ctx.status(401).result("Unauthorized");
    ctx.skipRemainingHandlers();
  }

  private void health(Context ctx) {
    ctx.status(200).result("OK");
  }
}
