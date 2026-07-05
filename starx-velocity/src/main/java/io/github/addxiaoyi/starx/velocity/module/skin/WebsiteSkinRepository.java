package io.github.addxiaoyi.starx.velocity.module.skin;

import com.google.gson.Gson;
import io.github.addxiaoyi.starx.api.dto.SkinDto;
import io.github.addxiaoyi.starx.api.repository.SkinRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 网站皮肤仓库实现，通过 HTTP 调用网站皮肤 API 获取 VLA 兼容的纹理 JSON。
 * 内置本地缓存避免重复 HTTP 请求。
 *
 * <p>API 格式：{@code GET {baseUrl}/{playerName}.json}
 */
public final class WebsiteSkinRepository implements SkinRepository {

  private static final int CACHE_TTL_SECONDS = 60;
  private static final int CACHE_MAX_SIZE = 500;

  private final String skinProfileBaseUrl;
  private final Logger logger;
  private final HttpClient httpClient;
  private final Gson gson;
  private final Map<String, CacheEntry> cache;

  public WebsiteSkinRepository(String skinProfileBaseUrl, Logger logger) {
    this.skinProfileBaseUrl =
        skinProfileBaseUrl.endsWith("/")
            ? skinProfileBaseUrl.substring(0, skinProfileBaseUrl.length() - 1)
            : skinProfileBaseUrl;
    this.logger = logger;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    this.gson = new Gson();
    this.cache = new ConcurrentHashMap<>(64);
  }

  @Override
  public Optional<SkinDto> findByPlayer(UUID uuid, String name) {
    CacheEntry cached = cache.get(name);
    if (cached != null && !cached.isExpired()) {
      return cached.skin;
    }

    String url = skinProfileBaseUrl + "/" + name + ".json";
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(5))
          .GET()
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        logger.fine("Website skin API returned status " + response.statusCode() + " for " + name);
        return Optional.empty();
      }
      SkinProfileResponse profile = gson.fromJson(response.body(), SkinProfileResponse.class);
      if (profile == null || profile.textures == null) {
        return Optional.empty();
      }
      String skinUrl = null;
      if (profile.textures.containsKey("SKIN")) {
        skinUrl = profile.textures.get("SKIN").url;
      }
      Optional<SkinDto> result = Optional.of(
          new SkinDto(uuid, name, profile.id, null, null, skinUrl));
      if (cache.size() < CACHE_MAX_SIZE) {
        cache.put(name, new CacheEntry(result, Instant.now().plusSeconds(CACHE_TTL_SECONDS)));
      }
      return result;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to fetch skin from website for " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public void setSkinId(UUID uuid, String skinId) {
    // Website manages skins, no-op
  }

  @Override
  public void setSkinData(UUID uuid, String value, String signature) {
    // Website manages skins, no-op
  }

  @Override
  public void clearSkin(UUID uuid) {
    // Website manages skins, no-op
  }

  private static final class SkinProfileResponse {
    String id;
    String name;
    Map<String, TextureInfo> textures;
  }

  private static final class TextureInfo {
    String url;
    Map<String, String> metadata;
  }

  private static final class CacheEntry {
    final Optional<SkinDto> skin;
    final Instant expiresAt;

    CacheEntry(Optional<SkinDto> skin, Instant expiresAt) {
      this.skin = skin;
      this.expiresAt = expiresAt;
    }

    boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }
}