package io.github.addxiaoyi.starx.common.skin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NoopSkinRepositoryTest {

  private final NoopSkinRepository repository = new NoopSkinRepository();
  private final UUID uuid = UUID.randomUUID();

  @Test
  void findByPlayerReturnsEmpty() {
    assertThat(repository.findByPlayer(uuid, "TestPlayer")).isEmpty();
  }

  @Test
  void setSkinIdDoesNothing() {
    assertThatNoException().isThrownBy(() -> repository.setSkinId(uuid, "steve"));
  }

  @Test
  void setSkinDataDoesNothing() {
    assertThatNoException().isThrownBy(() -> repository.setSkinData(uuid, "value", "signature"));
  }

  @Test
  void clearSkinDoesNothing() {
    assertThatNoException().isThrownBy(() -> repository.clearSkin(uuid));
  }
}
