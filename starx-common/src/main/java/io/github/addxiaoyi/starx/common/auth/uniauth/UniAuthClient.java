package io.github.addxiaoyi.starx.common.auth.uniauth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/** UniAuth 服务 HTTP 客户端，用于与 StarVC UniAuth 后端通信。 */
public final class UniAuthClient {

  private static final Logger logger = Logger.getLogger(UniAuthClient.class.getName());
  private static final Gson gson = new Gson();

  private final UniAuthConfig config;
  private final HttpClient httpClient;

  public UniAuthClient(UniAuthConfig config) {
    this.config = Objects.requireNonNull(config, "config");
    this.httpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofMillis(config.timeoutMs())).build();
  }

  public record LoginRequest(String username, String password, String appId) {}

  public record LoginResponse(boolean success, String message, String userId, String email) {}

  public record StatusResponse(boolean exists, boolean imported, String status) {}

  /**
   * 验证用户密码。
   *
   * @param username 用户名
   * @param password 密码
   * @return 异步认证结果
   */
  public CompletableFuture<LoginResponse> login(String username, String password) {
    CompletableFuture<LoginResponse> future = new CompletableFuture<>();

    try {
      LoginRequest request = new LoginRequest(username, password, config.appId());

      HttpRequest httpRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(normalizeUrl(config.apiUrl()) + "login"))
              .timeout(Duration.ofMillis(config.timeoutMs()))
              .header("Content-Type", "application/json")
              .header("X-App-Id", config.appId())
              .header("X-App-Secret", config.appSecret())
              .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
              .build();

      httpClient
          .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
          .thenAccept(
              response -> {
                try {
                  if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    boolean success = json.has("success") && json.get("success").getAsBoolean();
                    String message = json.has("message") ? json.get("message").getAsString() : "";
                    String userId = json.has("userId") ? json.get("userId").getAsString() : null;
                    String email = json.has("email") ? json.get("email").getAsString() : null;
                    future.complete(new LoginResponse(success, message, userId, email));
                  } else {
                    logger.log(
                        Level.WARNING,
                        "UniAuth login failed with status {0}: {1}",
                        new Object[] {response.statusCode(), response.body()});
                    future.complete(
                        new LoginResponse(
                            false,
                            "Authentication failed with status " + response.statusCode(),
                            null,
                            null));
                  }
                } catch (Exception e) {
                  logger.log(
                      Level.WARNING, "Failed to parse UniAuth response: {0}", response.body());
                  future.complete(new LoginResponse(false, "Invalid response format", null, null));
                }
              })
          .exceptionally(
              ex -> {
                logger.log(Level.WARNING, "UniAuth request failed", ex);
                future.complete(new LoginResponse(false, ex.getMessage(), null, null));
                return null;
              });
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to create UniAuth request", e);
      future.complete(new LoginResponse(false, e.getMessage(), null, null));
    }

    return future;
  }

  /**
   * 查询用户状态。
   *
   * @param username 用户名
   * @return 异步状态查询结果
   */
  public CompletableFuture<StatusResponse> fetchStatus(String username) {
    CompletableFuture<StatusResponse> future = new CompletableFuture<>();

    try {
      HttpRequest httpRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(normalizeUrl(config.apiUrl()) + "status/" + username))
              .timeout(Duration.ofMillis(config.timeoutMs()))
              .header("X-App-Id", config.appId())
              .header("X-App-Secret", config.appSecret())
              .GET()
              .build();

      httpClient
          .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
          .thenAccept(
              response -> {
                try {
                  if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    boolean exists = json.has("exists") && json.get("exists").getAsBoolean();
                    boolean imported = json.has("imported") && json.get("imported").getAsBoolean();
                    String status =
                        json.has("status") ? json.get("status").getAsString() : "UNKNOWN";
                    future.complete(new StatusResponse(exists, imported, status));
                  } else if (response.statusCode() == 404) {
                    future.complete(new StatusResponse(false, false, "NOT_EXIST"));
                  } else {
                    future.complete(new StatusResponse(false, false, "ERROR"));
                  }
                } catch (Exception e) {
                  logger.log(
                      Level.WARNING,
                      "Failed to parse UniAuth status response: {0}",
                      response.body());
                  future.complete(new StatusResponse(false, false, "ERROR"));
                }
              })
          .exceptionally(
              ex -> {
                logger.log(Level.WARNING, "UniAuth status request failed", ex);
                future.complete(new StatusResponse(false, false, "ERROR"));
                return null;
              });
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to create UniAuth status request", e);
      future.complete(new StatusResponse(false, false, "ERROR"));
    }

    return future;
  }

  private static String normalizeUrl(String url) {
    if (url == null) return "";
    return url.endsWith("/") ? url : url + "/";
  }
}
