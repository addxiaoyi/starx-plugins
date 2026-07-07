package io.github.addxiaoyi.starx.velocity.module.auth;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthClient;
import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthConfig;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** UniAuth 统一认证后端模块，对接 UniAuth API 提供集中式认证服务。 */
public final class UniAuthModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final UniAuthConfig config;
  private UniAuthClient client;

  public UniAuthModule(StarxVelocityPlugin plugin, EventBus eventBus, UniAuthConfig config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "starx.auth.uniauth";
  }

  @Override
  public void onEnable() {
    if (config.enabled()) {
      this.client = new UniAuthClient(config);
      plugin.logger().log(Level.INFO, "UniAuthModule 已启用，API: {0}", config.apiUrl());
    }
  }

  @Override
  public void onDisable() {
    this.client = null;
  }

  public UniAuthConfig getConfig() {
    return config;
  }

  public UniAuthClient getClient() {
    return client;
  }

  public CompletableFuture<UniAuthClient.LoginResponse> login(String username, String password) {
    if (client == null) {
      return CompletableFuture.completedFuture(
          new UniAuthClient.LoginResponse(false, "UniAuth 模块未启用", null, null));
    }
    return client.login(username, password);
  }

  public CompletableFuture<UniAuthClient.StatusResponse> fetchStatus(String username) {
    if (client == null) {
      return CompletableFuture.completedFuture(
          new UniAuthClient.StatusResponse(false, false, "disabled"));
    }
    return client.fetchStatus(username);
  }
}
