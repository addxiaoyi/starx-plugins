package io.github.addxiaoyi.starx.velocity.module.skin;

import com.google.gson.Gson;
import io.github.addxiaoyi.starx.api.dto.SkinDto;
import io.github.addxiaoyi.starx.api.repository.SkinRepository;
import io.github.addxiaoyi.starx.common.smart.SmartCache;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 网站皮肤仓库实现，通过 HTTP 调用网站皮肤 API 获取 VLA 兼容的纹理 JSON。
 *
 * <p>使用 SmartCache 实现 LRU 缓存 + TTL + 访问计数。
 *
 * <p>API 格式：{@code GET {baseUrl}/{playerName}.json}
 */
public final class WebsiteSkinRepository implements SkinRepository {

  private static final int CACHE_TTL_MS = 60_000;
  private static final int CACHE_MAX_SIZE = 500;

  private final String skinProfileBaseUrl;
  private final Logger logger;
  private final HttpClient httpClient;
  private final Gson gson;
  private final SmartCache<String, Optional<SkinDto>> cache;

  public WebsiteSkinRepository(String skinProfileBaseUrl, Logger logger) {
    this.skinProfileBaseUrl =
        skinProfileBaseUrl.endsWith("/")
            ? skinProfileBaseUrl.substring(0, skinProfileBaseUrl.length() - 1)
            : skinProfileBaseUrl;
    this.logger = logger;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    this.gson = new Gson();
    this.cache = new SmartCache<>(CACHE_MAX_SIZE, CACHE_TTL_MS, k -> Optional.empty());
  }

  @Override
  public Optional<SkinDto> findByPlayer(UUID uuid, String name) {
    Optional<SkinDto> cached = cache.getIfPresent(name);
    if (cached != null) {
      return cached;
    }
    Optional<SkinDto> fetched = fetchSkin(uuid, name);
    cache.put(name, fetched);
    return fetched;
  }

  private Optional<SkinDto> fetchSkin(UUID uuid, String name) {
    String url = skinProfileBaseUrl + "/" + name + ".json";
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofSeconds(5))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
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
      return Optional.of(new SkinDto(uuid, name, profile.id, null, null, skinUrl));
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
}
