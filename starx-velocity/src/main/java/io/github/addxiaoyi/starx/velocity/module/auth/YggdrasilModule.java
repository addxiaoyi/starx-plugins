package io.github.addxiaoyi.starx.velocity.module.auth;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** Yggdrasil 外置认证模块，支持 Mojang / LittleSkin 等 Yggdrasil 兼容认证服务器。 */
public final class YggdrasilModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;
  private final HttpClient httpClient;

  public YggdrasilModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(config.timeout()))
            .build();
  }

  @Override
  public String name() {
    return "auth.yggdrasil";
  }

  @Override
  public void onEnable() {
    plugin
        .logger()
        .log(Level.INFO, "YggdrasilModule 已启用，加载 {0} 个认证服务器", config.servers().size());
  }

  @Override
  public void onDisable() {
    // 无资源需要释放
  }

  public Map<String, String> getServers() {
    return Collections.unmodifiableMap(config.servers());
  }

  /**
   * 解析指定认证服务器的完整 URL。
   *
   * @param serverName 认证服务器名称
   * @param endpoint 端点路径（如 session/minecraft/hasJoined）
   * @return 完整 URL，如果服务器不存在则返回 null
   */
  public String resolveServerUrl(String serverName, String endpoint) {
    String baseUrl = config.servers().get(serverName);
    if (baseUrl == null) {
      return null;
    }
    return baseUrl + endpoint;
  }

  /**
   * 检查用户是否在指定 Yggdrasil 服务器上存在。
   *
   * @param username 用户名
   * @param uuid 玩家 UUID
   * @param serverName 认证服务器名称
   * @return 用户存在的 future
   */
  public CompletableFuture<Boolean> checkUserExists(
      String username, UUID uuid, String serverName) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    String baseUrl = config.servers().get(serverName);
    if (baseUrl == null) {
      future.complete(false);
      return future;
    }
    String url =
        baseUrl + "session/minecraft/profile/" + uuid.toString().replace("-", "");
    httpClient
        .sendAsync(
            HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString())
        .thenAccept(
            response -> {
              if (response.statusCode() == 200) {
                String body = response.body();
                boolean nameMatches =
                    body.contains("\"name\"") && body.contains("\"" + username + "\"");
                future.complete(nameMatches);
              } else {
                future.complete(false);
              }
            })
        .exceptionally(
            ex -> {
              plugin
                  .logger()
                  .log(Level.WARNING, "检查用户 " + username + " 在 " + serverName + " 上失败: " + ex.getMessage());
              future.complete(false);
              return null;
            });
    return future;
  }

  /**
   * 在所有配置的 Yggdrasil 服务器上检查用户是否存在。
   *
   * @param username 用户名
   * @param uuid 玩家 UUID
   * @return 任意服务器存在则为 true
   */
  public CompletableFuture<Boolean> checkAllServers(String username, UUID uuid) {
    // TODO: 实现完整的并行检查 + 超时控制逻辑（参考 StarVC YggdrasilAuthenticator.checkAllExists）
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    result.complete(false);
    return result;
  }

  /**
   * 通过 hasJoined 端点认证玩家。
   *
   * @param username 用户名
   * @param serverId 服务器 ID
   * @param ip 玩家 IP（可选，取决于 verifyIp 配置）
   * @param serverName 认证服务器名称
   * @return 认证结果，成功则返回玩家信息 JSON，失败返回 null
   */
  public CompletableFuture<String> authenticate(
      String username, String serverId, String ip, String serverName) {
    CompletableFuture<String> future = new CompletableFuture<>();
    String baseUrl = config.servers().get(serverName);
    if (baseUrl == null) {
      future.complete(null);
      return future;
    }
    StringBuilder urlBuilder =
        new StringBuilder(baseUrl)
            .append("session/minecraft/hasJoined")
            .append("?username=")
            .append(username)
            .append("&serverId=")
            .append(serverId);
    if (config.verifyIp() && ip != null) {
      urlBuilder.append("&ip=").append(ip);
    }
    httpClient
        .sendAsync(
            HttpRequest.newBuilder().uri(URI.create(urlBuilder.toString())).GET().build(),
            HttpResponse.BodyHandlers.ofString())
        .thenAccept(
            response -> {
              if (response.statusCode() == 200) {
                future.complete(response.body());
              } else {
                future.complete(null);
              }
            })
        .exceptionally(
            ex -> {
              plugin
                  .logger()
                  .log(Level.WARNING, "Yggdrasil 认证 " + username + " 失败: " + ex.getMessage());
              future.complete(null);
              return null;
            });
    return future;
  }

  /** Yggdrasil 模块配置。 */
  public interface Config {
    boolean enabled();

    Map<String, String> servers();

    boolean verifyIp();

    int timeout();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return true;
        }

        @Override
        public Map<String, String> servers() {
          return Map.of("mojang", "https://sessionserver.mojang.com/");
        }

        @Override
        public boolean verifyIp() {
          return false;
        }

        @Override
        public int timeout() {
          return 5000;
        }
      };
    }
  }
}