package io.github.addxiaoyi.starx.common.smart;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdaptiveRateLimiterTest {

  @Test
  void shouldDefaultToNormalLoad() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    assertThat(limiter.evaluateLoad()).isEqualTo(AdaptiveRateLimiter.LoadLevel.NORMAL);
  }

  @Test
  void shouldDetectLowLoad() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    limiter.updateTps(20);
    limiter.updateMemoryPercent(30);
    assertThat(limiter.evaluateLoad()).isEqualTo(AdaptiveRateLimiter.LoadLevel.LOW);
  }

  @Test
  void shouldDetectNormalLoad() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    limiter.updateTps(19);
    limiter.updateMemoryPercent(55);
    assertThat(limiter.evaluateLoad()).isEqualTo(AdaptiveRateLimiter.LoadLevel.NORMAL);
  }

  @Test
  void shouldDetectModerateLoad() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    limiter.updateTps(17);
    limiter.updateMemoryPercent(50);
    assertThat(limiter.evaluateLoad()).isEqualTo(AdaptiveRateLimiter.LoadLevel.MODERATE);
  }

  @Test
  void shouldDetectHighLoadByTps() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    limiter.updateTps(14);
    limiter.updateMemoryPercent(50);
    assertThat(limiter.evaluateLoad()).isEqualTo(AdaptiveRateLimiter.LoadLevel.HIGH);
  }

  @Test
  void shouldDetectHighLoadByMemory() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    limiter.updateTps(18);
    limiter.updateMemoryPercent(80);
    assertThat(limiter.evaluateLoad()).isEqualTo(AdaptiveRateLimiter.LoadLevel.HIGH);
  }

  @Test
  void shouldDetectCriticalLoad() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    limiter.updateTps(5);
    limiter.updateMemoryPercent(50);
    assertThat(limiter.evaluateLoad()).isEqualTo(AdaptiveRateLimiter.LoadLevel.CRITICAL);
  }

  @Test
  void shouldDetectCriticalLoadByMemory() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    limiter.updateTps(15);
    limiter.updateMemoryPercent(95);
    assertThat(limiter.evaluateLoad()).isEqualTo(AdaptiveRateLimiter.LoadLevel.CRITICAL);
  }

  @Test
  void shouldApplyCorrectMultiplier() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);

    limiter.updateTps(20);
    limiter.updateMemoryPercent(30);
    assertThat(limiter.maxConnectionsPerSecond()).isEqualTo(20);
    assertThat(limiter.maxPingsPerSecond()).isEqualTo(40);

    limiter.updateTps(10);
    limiter.updateMemoryPercent(50);
    assertThat(limiter.maxConnectionsPerSecond()).isEqualTo(2);
    assertThat(limiter.maxPingsPerSecond()).isEqualTo(5);
  }

  @Test
  void shouldCapAtMinimumOne() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(1, 1);
    limiter.updateTps(5);
    limiter.updateMemoryPercent(50);
    assertThat(limiter.maxConnectionsPerSecond()).isEqualTo(0);
    assertThat(limiter.maxPingsPerSecond()).isEqualTo(0);
  }

  @Test
  void shouldBeStaleAfter30Seconds() throws InterruptedException {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    assertThat(limiter.isStale()).isFalse();

    limiter.updateTps(15);
    assertThat(limiter.isStale()).isFalse();
  }

  @Test
  void shouldUpdateTpsCorrectly() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    limiter.updateTps(18);
    assertThat(limiter.getCurrentTps()).isEqualTo(18);
    limiter.updateTps(5);
    assertThat(limiter.getCurrentTps()).isEqualTo(5);
  }

  @Test
  void shouldUpdateMemoryPercentCorrectly() {
    AdaptiveRateLimiter limiter = new AdaptiveRateLimiter(10, 20);
    limiter.updateMemoryPercent(75);
    assertThat(limiter.getCurrentMemoryPercent()).isEqualTo(75);
    limiter.updateMemoryPercent(0);
    assertThat(limiter.getCurrentMemoryPercent()).isEqualTo(0);
    limiter.updateMemoryPercent(100);
    assertThat(limiter.getCurrentMemoryPercent()).isEqualTo(100);
  }
}
