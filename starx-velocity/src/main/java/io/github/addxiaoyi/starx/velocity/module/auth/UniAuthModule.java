package io.github.addxiaoyi.starx.velocity.module.auth;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** UniAuth 统一认证后端模块，对接 UniAuth API 提供集中式认证服务。 */
public final class UniAuthModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;
  private final HttpClient httpClient;

  public UniAuthModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
    this.httpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofMillis(config.timeout())).build();
  }

  @Override
  public String name() {
    return "auth.uniauth";
  }

  @Override
  public void onEnable() {
    plugin.logger().log(Level.INFO, "UniAuthModule 已启用，API: {0}", config.apiUrl());
    // TODO: 验证 API 连通性
  }

  @Override
  public void onDisable() {
    // 无资源需要释放
  }

  public String getApiUrl() {
    return config.apiUrl();
  }

  public String getAppId() {
    return config.appId();
  }

  /**
   * 通过 UniAuth 登录用户。
   *
   * @param username 用户名
   * @param password 密码
   * @return 认证结果
   */
  public CompletableFuture<UniAuthResult> login(String username, String password) {
    // TODO: 实现完整的 UniAuth 登录流程
    CompletableFuture<UniAuthResult> future = new CompletableFuture<>();
    httpClient
        .sendAsync(
            HttpRequest.newBuilder()
                .uri(URI.create(config.apiUrl() + "login"))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "{ \"username\": \""
                            + username
                            + "\", \"password\": \""
                            + password
                            + "\", \"appId\": \""
                            + config.appId()
                            + "\" }"))
                .build(),
            HttpResponse.BodyHandlers.ofString())
        .thenAccept(
            response -> {
              if (response.statusCode() == 200) {
                future.complete(UniAuthResult.SUCCESS);
              } else {
                future.complete(UniAuthResult.UNKNOWN_ERROR);
              }
            })
        .exceptionally(
            ex -> {
              plugin.logger().log(Level.WARNING, "UniAuth 登录失败: " + ex.getMessage());
              future.complete(UniAuthResult.UNKNOWN_ERROR);
              return null;
            });
    return future;
  }

  /**
   * 通过 UniAuth 注册用户（无邮箱验证）。
   *
   * @param username 用户名
   * @param password 密码
   * @return 注册结果
   */
  public CompletableFuture<UniAuthResult> registerWithoutEmail(String username, String password) {
    // TODO: 实现完整的 UniAuth 注册流程
    CompletableFuture<UniAuthResult> future = new CompletableFuture<>();
    future.complete(UniAuthResult.UNKNOWN_ERROR);
    return future;
  }

  /**
   * 查询用户在 UniAuth 后端的状态。
   *
   * @param username 用户名
   * @return 用户状态
   */
  public CompletableFuture<UserStatus> fetchStatus(String username) {
    // TODO: 实现用户状态查询
    CompletableFuture<UserStatus> future = new CompletableFuture<>();
    future.complete(UserStatus.UNKNOWN);
    return future;
  }

  /** UniAuth 认证结果枚举。 */
  public enum UniAuthResult {
    SUCCESS,
    INVALID_PASSWORD,
    NOT_REGISTERED,
    EMAIL_NOT_VERIFIED,
    ALREADY_REGISTERED,
    UNKNOWN_ERROR
  }

  /** 用户状态枚举。 */
  public enum UserStatus {
    REGISTERED,
    IMPORTED,
    NOT_EXIST,
    UNKNOWN
  }

  /** UniAuth 模块配置。 */
  public interface Config {
    boolean enabled();

    String apiUrl();

    String appId();

    String appSecret();

    int timeout();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public String apiUrl() {
          return "https://api.example.com/uniauth/";
        }

        @Override
        public String appId() {
          return "";
        }

        @Override
        public String appSecret() {
          return "";
        }

        @Override
        public int timeout() {
          return 5000;
        }
      };
    }
  }
}
