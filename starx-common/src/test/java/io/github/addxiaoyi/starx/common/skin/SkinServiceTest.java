package io.github.addxiaoyi.starx.common.skin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.addxiaoyi.starx.api.dto.SkinDto;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.repository.SkinRepository;
import io.github.addxiaoyi.starx.common.event.LocalEventBus;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SkinServiceTest {

  private final UUID uuid = UUID.randomUUID();
  private final String name = "TestPlayer";

  @Test
  void applySkinPublishesSkinAppliedEvent() {
    LocalEventBus bus = new LocalEventBus();
    SkinService service = new SkinService(new NoopSkinRepository(), bus);
    AtomicReference<UUID> received = new AtomicReference<>();

    bus.subscribe(
        EventTypes.SKIN_APPLIED,
        event -> received.set(UUID.fromString((String) event.payload().get("uuid"))));

    service.applySkin(uuid);

    assertThat(received.get()).isEqualTo(uuid);
  }

  @Test
  void notifySkinUpdatedPublishesSkinUpdatedEvent() {
    LocalEventBus bus = new LocalEventBus();
    SkinService service = new SkinService(new NoopSkinRepository(), bus);
    AtomicReference<UUID> received = new AtomicReference<>();

    bus.subscribe(
        EventTypes.SKIN_UPDATED,
        event -> received.set(UUID.fromString((String) event.payload().get("uuid"))));

    service.notifySkinUpdated(uuid);

    assertThat(received.get()).isEqualTo(uuid);
  }

  @Test
  void refreshSkinAppliesAndNotifies_whenRepositoryReturnsSkin() {
    LocalEventBus bus = new LocalEventBus();
    SkinDto skin = new SkinDto(uuid, name, "steve", "value", "signature", null);
    SkinRepository repo = new InMemorySkinRepository(skin);
    SkinService service = new SkinService(repo, bus);

    AtomicReference<UUID> applied = new AtomicReference<>();
    AtomicReference<UUID> updated = new AtomicReference<>();
    bus.subscribe(
        EventTypes.SKIN_APPLIED,
        event -> applied.set(UUID.fromString((String) event.payload().get("uuid"))));
    bus.subscribe(
        EventTypes.SKIN_UPDATED,
        event -> updated.set(UUID.fromString((String) event.payload().get("uuid"))));

    service.refreshSkin(uuid, name);

    assertThat(applied.get()).isEqualTo(uuid);
    assertThat(updated.get()).isEqualTo(uuid);
  }

  @Test
  void refreshSkinPublishesRefreshRequest_whenRepositoryReturnsEmpty() {
    LocalEventBus bus = new LocalEventBus();
    SkinService service = new SkinService(new NoopSkinRepository(), bus);
    AtomicReference<UUID> received = new AtomicReference<>();

    bus.subscribe(
        EventTypes.SKIN_REFRESH_REQUEST,
        event -> received.set(UUID.fromString((String) event.payload().get("uuid"))));

    service.refreshSkin(uuid, name);

    assertThat(received.get()).isEqualTo(uuid);
  }

  @Test
  void refreshSkinRejectsNullUuid() {
    SkinService service = new SkinService(new NoopSkinRepository(), new LocalEventBus());

    assertThatThrownBy(() -> service.refreshSkin(null, name))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void applySkinRejectsNullUuid() {
    SkinService service = new SkinService(new NoopSkinRepository(), new LocalEventBus());

    assertThatThrownBy(() -> service.applySkin(null)).isInstanceOf(NullPointerException.class);
  }

  private static final class InMemorySkinRepository implements SkinRepository {
    private final SkinDto skin;

    InMemorySkinRepository(SkinDto skin) {
      this.skin = skin;
    }

    @Override
    public Optional<SkinDto> findByPlayer(UUID uuid, String name) {
      return Optional.of(skin);
    }

    @Override
    public void setSkinId(UUID uuid, String skinId) {}

    @Override
    public void setSkinData(UUID uuid, String value, String signature) {}

    @Override
    public void clearSkin(UUID uuid) {}
  }
}
