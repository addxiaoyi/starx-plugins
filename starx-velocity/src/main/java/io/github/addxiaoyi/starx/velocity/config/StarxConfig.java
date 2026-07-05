package io.github.addxiaoyi.starx.velocity.config;

import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthConfig;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import java.util.Map;
import java.util.Objects;

/** starx-velocity 运行时配置模型。 */
public final class StarxConfig {

  private final String apiKey;
  private final HttpConfig http;
  private final WebhookConfig webhook;
  private final DatabaseConfig database;
  private final UniAuthConfig uniauth;
  private final Map<String, ModuleConfig> modules;

  public StarxConfig(
      String apiKey,
      HttpConfig http,
      WebhookConfig webhook,
      DatabaseConfig database,
      UniAuthConfig uniauth,
      Map<String, ModuleConfig> modules) {
    this.apiKey = apiKey;
    this.http = Objects.requireNonNull(http, "http");
    this.webhook = Objects.requireNonNull(webhook, "webhook");
    this.database = database == null ? DatabaseConfig.defaults() : database;
    this.uniauth = uniauth == null ? UniAuthConfig.defaults() : uniauth;
    this.modules = modules == null ? Map.of() : Map.copyOf(modules);
  }

  public String apiKey() {
    return apiKey;
  }

  public HttpConfig http() {
    return http;
  }

  public WebhookConfig webhook() {
    return webhook;
  }

  public DatabaseConfig database() {
    return database;
  }

  public UniAuthConfig uniauth() {
    return uniauth;
  }

  public Map<String, ModuleConfig> modules() {
    return modules;
  }

  public boolean isModuleEnabled(String name) {
    ModuleConfig module = modules.get(name);
    return module != null && module.enabled();
  }

  public static final class HttpConfig {
    private final String bind;
    private final int port;

    public HttpConfig(String bind, int port) {
      this.bind = bind == null || bind.isBlank() ? "127.0.0.1" : bind;
      this.port = port <= 0 || port > 65535 ? 8788 : port;
    }

    public String bind() {
      return bind;
    }

    public int port() {
      return port;
    }
  }

  public static final class WebhookConfig {
    private final String url;
    private final String secret;

    public WebhookConfig(String url, String secret) {
      this.url = url;
      this.secret = secret;
    }

    public String url() {
      return url;
    }

    public String secret() {
      return secret;
    }

    public boolean isConfigured() {
      return url != null && !url.isBlank();
    }
  }

  public static final class ModuleConfig {
    private final boolean enabled;

    public ModuleConfig(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean enabled() {
      return enabled;
    }
  }
}
