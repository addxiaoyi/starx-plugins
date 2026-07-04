package io.github.addxiaoyi.starx.velocity.http;

import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 基于 Javalin 的内部 HTTP API 服务器。 */
public final class HttpApiServer {

  public static final String API_KEY_HEADER = "X-API-Key";

  private final StarxConfig config;
  private final SkinBridgeModule skinBridge;
  private Javalin app;

  public HttpApiServer(StarxConfig config, SkinBridgeModule skinBridge) {
    this.config = Objects.requireNonNull(config, "config");
    this.skinBridge = skinBridge;
  }

  public void start() {
    app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner = false);
    app.before(this::authFilter);
    app.get("/v1/health", this::health);
    app.get("/v1/user/exists", this::userExists);
    app.post("/v1/skin/refresh", this::refreshSkin);
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
    String provided = ctx.header(API_KEY_HEADER);
    if (!apiKey.equals(provided)) {
      ctx.status(401).result("Unauthorized");
      ctx.skipRemainingHandlers();
    }
  }

  private void health(Context ctx) {
    ctx.status(200).result("OK");
  }

  private void userExists(Context ctx) {
    ctx.status(501).result("Not implemented");
  }

  private void refreshSkin(Context ctx) {
    UUID uuid;
    try {
      uuid = UUID.fromString(ctx.bodyAsClass(SkinRefreshRequest.class).uuid);
    } catch (Exception e) {
      ctx.status(400).result("Invalid UUID");
      return;
    }
    if (skinBridge != null) {
      skinBridge.refreshSkin(uuid);
    }
    ctx.status(200).json(Map.of("success", true));
  }

  /** 皮肤刷新请求体。 */
  public static final class SkinRefreshRequest {
    public String uuid;
  }
}
