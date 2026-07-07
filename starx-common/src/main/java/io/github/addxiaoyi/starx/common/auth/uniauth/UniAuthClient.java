package io.github.addxiaoyi.starx.common.auth.uniauth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/** UniAuth 服务 HTTP 客户端，使用 RSA 加密协议与 UniAuth 后端通信。 */
public final class UniAuthClient {

  private static final Logger logger = Logger.getLogger(UniAuthClient.class.getName());
  private static final Gson gson = new Gson();

  private final UniAuthConfig config;
  private final HttpClient httpClient;
  private String publicKey;

  public UniAuthClient(UniAuthConfig config) {
    this.config = Objects.requireNonNull(config, "config");
    this.httpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofMillis(config.timeoutMs())).build();
  }

  public record LoginResponse(boolean success, String message, String userId, String email) {}

  public record StatusResponse(boolean exists, boolean imported, String status) {}

  private String getPublicKey() {
    if (publicKey == null) {
      refreshPublicKey();
    }
    return publicKey;
  }

  private void refreshPublicKey() {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(normalizeUrl(config.apiUrl()) + "publickey"))
              .timeout(Duration.ofMillis(config.timeoutMs()))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        this.publicKey = response.body().trim();
        logger.log(Level.INFO, "UniAuth public key fetched");
      } else {
        throw new RuntimeException("Failed to fetch public key: HTTP " + response.statusCode());
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch UniAuth public key", e);
    }
  }

  private JsonObject request(String endpoint, Map<String, String> data) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("data", data);
      payload.put("apikey", config.apiKey());
      payload.put("timestamp", System.currentTimeMillis());

      String encrypted = RSAUtil.encryptByPublicKey(gson.toJson(payload), getPublicKey());

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(normalizeUrl(config.apiUrl()) + endpoint))
              .timeout(Duration.ofMillis(config.timeoutMs()))
              .POST(HttpRequest.BodyPublishers.ofString(encrypted))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      String body = response.body();

      String checksum = response.headers().firstValue("X-Checksum").orElse("");
      String timestamp = response.headers().firstValue("X-Timestamp").orElse("");
      if (!checksum.isEmpty() && !timestamp.isEmpty()) {
        String hash = sha256(body) + "$" + timestamp;
        String decrypted = RSAUtil.decryptByPublicKey(checksum, getPublicKey());
        if (!hash.equals(decrypted)) {
          throw new RuntimeException("Response checksum mismatch");
        }
      }

      return JsonParser.parseString(body).getAsJsonObject();
    } catch (Exception e) {
      if (e.getMessage() != null && e.getMessage().contains("checksum")) {
        throw new RuntimeException(e);
      }
      try {
        refreshPublicKey();
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);
        payload.put("apikey", config.apiKey());
        payload.put("timestamp", System.currentTimeMillis());

        String encrypted = RSAUtil.encryptByPublicKey(gson.toJson(payload), getPublicKey());

        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(URI.create(normalizeUrl(config.apiUrl()) + endpoint))
                .timeout(Duration.ofMillis(config.timeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(encrypted))
                .build();

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        String checksum = response.headers().firstValue("X-Checksum").orElse("");
        String timestamp = response.headers().firstValue("X-Timestamp").orElse("");
        if (!checksum.isEmpty() && !timestamp.isEmpty()) {
          String hash = sha256(body) + "$" + timestamp;
          String decrypted = RSAUtil.decryptByPublicKey(checksum, getPublicKey());
          if (!hash.equals(decrypted)) {
            throw new RuntimeException("Response checksum mismatch");
          }
        }

        return JsonParser.parseString(body).getAsJsonObject();
      } catch (Exception ex) {
        throw new RuntimeException("UniAuth request failed: " + endpoint, ex);
      }
    }
  }

  public CompletableFuture<LoginResponse> login(String username, String password) {
    CompletableFuture<LoginResponse> future = new CompletableFuture<>();
    try {
      Map<String, String> data = new HashMap<>();
      data.put("username", username);
      data.put("password", password);

      JsonObject json = request("login", data);
      int code = json.has("code") ? json.get("code").getAsInt() : 500;

      switch (code) {
        case 200:
          future.complete(new LoginResponse(true, "登录成功", null, null));
          break;
        case 401:
          future.complete(new LoginResponse(false, "密码错误", null, null));
          break;
        case 402:
          future.complete(new LoginResponse(false, "用户未注册", null, null));
          break;
        case 403:
          future.complete(new LoginResponse(false, "邮箱未验证", null, null));
          break;
        default:
          future.complete(new LoginResponse(false, "认证失败: " + code, null, null));
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "UniAuth login failed", e);
      future.complete(new LoginResponse(false, e.getMessage(), null, null));
    }
    return future;
  }

  public CompletableFuture<StatusResponse> fetchStatus(String username) {
    CompletableFuture<StatusResponse> future = new CompletableFuture<>();
    try {
      Map<String, String> data = new HashMap<>();
      data.put("username", username);

      JsonObject json = request("playerInfo", data);
      int code = json.has("code") ? json.get("code").getAsInt() : 500;

      if (code == 200) {
        JsonObject profile =
            json.has("data") ? json.get("data").getAsJsonObject() : new JsonObject();
        boolean exists =
            profile.has("profile.exists")
                ? profile.get("profile.exists").getAsBoolean()
                : profile.has("profile")
                    ? profile.get("profile").getAsJsonObject().get("exists").getAsBoolean()
                    : false;
        boolean registered =
            profile.has("profile.registered")
                ? profile.get("profile.registered").getAsBoolean()
                : profile.has("profile")
                    ? profile.get("profile").getAsJsonObject().get("registered").getAsBoolean()
                    : false;
        future.complete(
            new StatusResponse(
                exists, registered, registered ? "REGISTERED" : exists ? "IMPORTED" : "NOT_EXIST"));
      } else {
        future.complete(new StatusResponse(false, false, "ERROR"));
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "UniAuth status request failed", e);
      future.complete(new StatusResponse(false, false, "ERROR"));
    }
    return future;
  }

  private static String normalizeUrl(String url) {
    if (url == null) return "";
    return url.endsWith("/") ? url : url + "/";
  }

  private static String sha256(String data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(data.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }
}
