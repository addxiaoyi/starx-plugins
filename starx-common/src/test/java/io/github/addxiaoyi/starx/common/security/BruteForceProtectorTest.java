package io.github.addxiaoyi.starx.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.common.security.BruteForceProtector.BruteForceStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BruteForceProtectorTest {

  private final BruteForceProtector protector = new BruteForceProtector();
  private final UUID uuid = UUID.randomUUID();

  @Test
  void allowsFirstAttempt() {
    assertThat(protector.check(uuid)).isEqualTo(BruteForceStatus.ALLOWED);
  }

  @Test
  void allowsAfterFewFailures() {
    for (int i = 0; i < 4; i++) {
      protector.recordFailure(uuid);
    }
    assertThat(protector.check(uuid)).isEqualTo(BruteForceStatus.DELAYED);
  }

  @Test
  void locksAfterFiveFailures() {
    for (int i = 0; i < 5; i++) {
      protector.recordFailure(uuid);
    }
    assertThat(protector.check(uuid)).isEqualTo(BruteForceStatus.LOCKED);
  }

  @Test
  void clearResetsCounter() {
    for (int i = 0; i < 3; i++) {
      protector.recordFailure(uuid);
    }
    protector.clear(uuid);
    assertThat(protector.check(uuid)).isEqualTo(BruteForceStatus.ALLOWED);
  }

  @Test
  void attemptCountIncreases() {
    protector.recordFailure(uuid);
    protector.recordFailure(uuid);
    assertThat(protector.getAttemptCount(uuid)).isEqualTo(2);
  }

  @Test
  void attemptCountIsZeroForNewUuid() {
    assertThat(protector.getAttemptCount(UUID.randomUUID())).isZero();
  }

  @Test
  void differentUuidsAreIndependent() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    protector.recordFailure(a);
    protector.recordFailure(a);
    protector.recordFailure(b);
    assertThat(protector.getAttemptCount(a)).isEqualTo(2);
    assertThat(protector.getAttemptCount(b)).isEqualTo(1);
  }

  @Test
  void delayedStatusHasPositiveWaitMs() {
    for (int i = 0; i < 3; i++) {
      protector.recordFailure(uuid);
    }
    BruteForceStatus status = protector.check(uuid);
    assertThat(status.waitMs()).isGreaterThan(0);
  }

  @Test
  void lockedStatusHasPositiveWaitMs() {
    for (int i = 0; i < 6; i++) {
      protector.recordFailure(uuid);
    }
    BruteForceStatus status = protector.check(uuid);
    assertThat(status).isEqualTo(BruteForceStatus.LOCKED);
    assertThat(status.waitMs()).isGreaterThan(0);
  }
}
