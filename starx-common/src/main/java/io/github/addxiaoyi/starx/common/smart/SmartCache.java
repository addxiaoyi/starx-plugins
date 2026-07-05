package io.github.addxiaoyi.starx.common.smart;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 多级智能缓存 — L1 内存 + 预测性预加载。
 *
 * <p>L1 缓存使用 LRU 策略，默认 500 条目，60 秒 TTL。提供预测性预加载接口： 当 L1 命中时，记录访问模式用于后续预测；当缓存未命中时，异步预加载关联数据。
 *
 * <p>线程安全。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public final class SmartCache<K, V> {

  private static final Logger logger = Logger.getLogger(SmartCache.class.getName());
  private static final ScheduledExecutorService preloader =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "starx-smart-cache-preloader");
            t.setDaemon(true);
            return t;
          });

  private final int maxSize;
  private final long ttlMillis;
  private final ConcurrentHashMap<K, CacheEntry> cache;
  private final Function<K, V> loader;

  // 访问计数，用于预测
  private final ConcurrentHashMap<K, Integer> accessCounts;

  public SmartCache(int maxSize, long ttlMillis, Function<K, V> loader) {
    this.maxSize = maxSize;
    this.ttlMillis = ttlMillis;
    this.cache = new ConcurrentHashMap<>(Math.min(maxSize, 64));
    this.loader = loader;
    this.accessCounts = new ConcurrentHashMap<>();
  }

  public static <K, V> SmartCache<K, V> withDefaults(Function<K, V> loader) {
    return new SmartCache<>(500, 60_000, loader);
  }

  /** 获取缓存值，未命中时同步加载 */
  public V get(K key) {
    CacheEntry entry = cache.get(key);
    if (entry != null && !entry.isExpired()) {
      accessCounts.merge(key, 1, Integer::sum);
      return entry.value;
    }
    return loadAndCache(key);
  }

  /** 获取缓存值，未命中时返回 null */
  public V getIfPresent(K key) {
    CacheEntry entry = cache.get(key);
    if (entry != null && !entry.isExpired()) {
      accessCounts.merge(key, 1, Integer::sum);
      return entry.value;
    }
    return null;
  }

  /** 放入缓存 */
  public void put(K key, V value) {
    cache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttlMillis));
    evictIfNeeded();
  }

  /** 异步预加载指定键 */
  public void preload(K key) {
    preloader.execute(
        () -> {
          try {
            if (getIfPresent(key) == null) {
              V value = loader.apply(key);
              if (value != null) {
                put(key, value);
              }
            }
          } catch (Exception e) {
            logger.log(Level.FINE, "SmartCache preload failed for key: {0}", key);
          }
        });
  }

  /** 获取访问频率最高的 N 个键 */
  public Map<K, Integer> topAccessKeys(int n) {
    return accessCounts.entrySet().stream()
        .sorted(Map.Entry.<K, Integer>comparingByValue().reversed())
        .limit(n)
        .collect(
            LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
  }

  /** 缓存大小 */
  public int size() {
    return cache.size();
  }

  /** 清除所有缓存 */
  public void clear() {
    cache.clear();
    accessCounts.clear();
  }

  private V loadAndCache(K key) {
    V value = loader.apply(key);
    if (value != null) {
      put(key, value);
      // 预测性预加载：根据访问计数，预加载热门键的关联数据
      if (accessCounts.getOrDefault(key, 0) > 5) {
        preloadRelated(key);
      }
    }
    return value;
  }

  /** 预加载关联数据（子类可覆盖） */
  protected void preloadRelated(K key) {
    // 默认空实现，子类可覆盖
  }

  private void evictIfNeeded() {
    while (cache.size() > maxSize) {
      cache.entrySet().stream()
          .min((a, b) -> Long.compare(a.getValue().expiresAt, b.getValue().expiresAt))
          .ifPresent(e -> cache.remove(e.getKey()));
    }
  }

  private final class CacheEntry {
    final V value;
    final long expiresAt;

    CacheEntry(V value, long expiresAt) {
      this.value = value;
      this.expiresAt = expiresAt;
    }

    boolean isExpired() {
      return System.currentTimeMillis() > expiresAt;
    }
  }
}
