package io.github.addxiaoyi.starx.common.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PremiumResolverTest {

  private final PremiumResolver resolver = new PremiumResolver();

  @Test
  void onlineModeRandomUuidIsPremium() {
    UUID onlineUuid = UUID.fromString("123e4567-e89b-42d3-a845-0462426f8c09");

    assertThat(resolver.isPremium(onlineUuid, true)).isTrue();
  }

  @Test
  void offlineModeIsNotPremium() {
    UUID offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:alice".getBytes());

    assertThat(resolver.isPremium(offlineUuid, false)).isFalse();
  }

  @Test
  void onlineModeWithOfflineUuidIsNotPremium() {
    UUID offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:alice".getBytes());

    assertThat(resolver.isPremium(offlineUuid, true)).isFalse();
  }
}
