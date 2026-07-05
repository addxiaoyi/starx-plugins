package io.github.addxiaoyi.starx.common.smart;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SmartCacheTest {

  private static SmartCache<String, String> createCache(int maxSize, long ttlMs) {
    return new SmartCache<>(maxSize, ttlMs, k -> "loaded:" + k);
  }

  @Test
  void shouldStoreAndRetrieveValue() {
    SmartCache<String, String> cache = createCache(10, 60_000);
    cache.put("key1", "value1");
    assertThat(cache.get("key1")).isEqualTo("value1");
  }

  @Test
  void shouldReturnNullForMissingKey() {
    SmartCache<String, String> cache = createCache(10, 60_000);
    assertThat(cache.getIfPresent("missing")).isNull();
  }

  @Test
  void shouldNotReloadOnIfPresent() {
    SmartCache<String, String> cache = createCache(10, 60_000);
    assertThat(cache.getIfPresent("missing")).isNull();
    assertThat(cache.size()).isEqualTo(0);
  }

  @Test
  void shouldEvictOldestWhenFull() {
    SmartCache<String, String> cache = createCache(2, 60_000);
    cache.put("a", "1");
    cache.put("b", "2");
    cache.put("c", "3");
    assertThat(cache.getIfPresent("a")).isNull();
    assertThat(cache.getIfPresent("b")).isEqualTo("2");
    assertThat(cache.getIfPresent("c")).isEqualTo("3");
  }

  @Test
  void shouldTrackAccessCounts() {
    SmartCache<String, String> cache = createCache(10, 60_000);
    cache.put("a", "1");
    cache.put("b", "2");
    cache.get("a");
    cache.get("a");
    cache.get("b");
    Map<String, Integer> top = cache.topAccessKeys(2);
    assertThat(top).containsKeys("a", "b");
    assertThat(top.get("a")).isGreaterThan(top.get("b"));
  }

  @Test
  void shouldReturnTopAccessKeys() {
    SmartCache<String, String> cache = createCache(10, 60_000);
    cache.put("a", "1");
    cache.put("b", "2");
    cache.put("c", "3");
    cache.get("a");
    cache.get("a");
    cache.get("a");
    cache.get("b");
    cache.get("b");
    cache.get("c");
    Map<String, Integer> top = cache.topAccessKeys(2);
    assertThat(top).containsKeys("a", "b");
  }

  @Test
  void shouldClearAllEntries() {
    SmartCache<String, String> cache = createCache(10, 60_000);
    cache.put("a", "1");
    cache.put("b", "2");
    cache.clear();
    assertThat(cache.size()).isEqualTo(0);
    assertThat(cache.getIfPresent("a")).isNull();
  }

  @Test
  void shouldReportCorrectSize() {
    SmartCache<String, String> cache = createCache(10, 60_000);
    assertThat(cache.size()).isEqualTo(0);
    cache.put("a", "1");
    assertThat(cache.size()).isEqualTo(1);
    cache.put("b", "2");
    assertThat(cache.size()).isEqualTo(2);
  }

  @Test
  void shouldPreloadAsync() {
    SmartCache<String, String> cache = createCache(10, 60_000);
    cache.preload("key");
    assertThat(cache.getIfPresent("key")).isNull();
  }
}
