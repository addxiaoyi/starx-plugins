package io.github.addxiaoyi.starx.common.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SessionManagerTest {

  @Test
  void getOrCreateReturnsGuestSession() throws Exception {
    SessionManager manager = new SessionManager(Duration.ofMinutes(10), () -> Instant.now());
    UUID uuid = UUID.randomUUID();

    AuthSession session = manager.getOrCreate(uuid, "alice", InetAddress.getByName("127.0.0.1"));

    assertThat(session.uuid()).isEqualTo(uuid);
    assertThat(session.username()).isEqualTo("alice");
    assertThat(session.state()).isEqualTo(AuthSession.State.GUEST);
  }

  @Test
  void getReturnsExistingSession() throws Exception {
    SessionManager manager = new SessionManager(Duration.ofMinutes(10), () -> Instant.now());
    UUID uuid = UUID.randomUUID();

    AuthSession created = manager.getOrCreate(uuid, "bob", InetAddress.getByName("127.0.0.1"));
    Optional<AuthSession> found = manager.get(uuid);

    assertThat(found).isPresent();
    assertThat(found.get()).isSameAs(created);
  }

  @Test
  void removeDeletesSession() throws Exception {
    SessionManager manager = new SessionManager(Duration.ofMinutes(10), () -> Instant.now());
    UUID uuid = UUID.randomUUID();
    manager.getOrCreate(uuid, "carol", InetAddress.getByName("127.0.0.1"));

    manager.remove(uuid);

    assertThat(manager.get(uuid)).isEmpty();
  }

  @Test
  void expiredSessionIsRemoved() throws Exception {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.ofEpochSecond(1_000_000L));
    Supplier<Instant> clock = now::get;
    SessionManager manager = new SessionManager(Duration.ofMinutes(10), clock);
    UUID uuid = UUID.randomUUID();
    manager.getOrCreate(uuid, "dave", InetAddress.getByName("127.0.0.1"));

    now.set(now.get().plus(Duration.ofMinutes(11)));

    assertThat(manager.get(uuid)).isEmpty();
  }
}
