package io.github.addxiaoyi.starx.common.stress;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.github.addxiaoyi.starx.common.auth.AuthSession;
import io.github.addxiaoyi.starx.common.auth.SessionManager;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.DatabaseManager;
import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.common.event.LocalEventBus;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("stress")
class AuthServiceStressTest {

  @Test
  void concurrentRegisterAndLogin() throws Exception {
    DatabaseConfig config = new DatabaseConfig("h2", "", 0, "stress_auth", "sa", "",
        "jdbc:h2:mem:stress_auth;DB_CLOSE_DELAY=-1", 10, 10_000L);
    try (DatabaseManager manager = new DatabaseManager(config)) {
      JdbiUserRepository repo = new JdbiUserRepository(manager.getJdbi());
      LocalEventBus bus = new LocalEventBus();
      SessionManager sessions = new SessionManager(Duration.ofMillis(300_000), Instant::now);
      AuthService auth = new AuthService(repo, bus, sessions);

      int threadCount = 10;
      int usersPerThread = 50;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicInteger registered = new AtomicInteger(0);
      AtomicInteger loggedIn = new AtomicInteger(0);
      AtomicInteger errors = new AtomicInteger(0);

      for (int t = 0; t < threadCount; t++) {
        final int tid = t;
        executor.submit(() -> {
          try {
            for (int u = 0; u < usersPerThread; u++) {
              String user = "stress_user_" + tid + "_" + u;
              UUID uuid = UUID.nameUUIDFromBytes(user.getBytes());
              auth.register(uuid, user, "Str0ng!Pass", user + "@test.com");
              registered.incrementAndGet();

              var result = auth.login(uuid, user, "Str0ng!Pass", null,
                  InetAddress.getLocalHost(), "device-" + tid);
              if (result.state() == AuthSession.State.AUTHENTICATED) {
                loggedIn.incrementAndGet();
              }
            }
          } catch (Exception e) {
            errors.incrementAndGet();
          }
          latch.countDown();
        });
      }

      latch.await(120, TimeUnit.SECONDS);
      executor.shutdown();

      assertThat(errors.get()).as("Concurrent auth errors: %d", errors.get()).isEqualTo(0);
      assertThat(registered.get()).isEqualTo(threadCount * usersPerThread);
    }
  }

  @Test
  void bruteForceUnderConcurrentLoad() throws Exception {
    DatabaseConfig config = new DatabaseConfig("h2", "", 0, "stress_brute", "sa", "",
        "jdbc:h2:mem:stress_brute;DB_CLOSE_DELAY=-1", 5, 5_000L);
    try (DatabaseManager manager = new DatabaseManager(config)) {
      JdbiUserRepository repo = new JdbiUserRepository(manager.getJdbi());
      LocalEventBus bus = new LocalEventBus();
      SessionManager sessions = new SessionManager(Duration.ofMillis(300_000), Instant::now);
      AuthService auth = new AuthService(repo, bus, sessions);

      UUID uuid = UUID.randomUUID();
      auth.register(uuid, "brute_target", "CorrectPass1!", "bt@test.com");

      int attempts = 50;
      ExecutorService executor = Executors.newFixedThreadPool(10);
      CountDownLatch latch = new CountDownLatch(attempts);

      for (int i = 0; i < attempts; i++) {
        executor.submit(() -> {
          try {
            auth.login(uuid, "brute_target", "WrongPass!", null,
                InetAddress.getLocalHost(), "brute-device");
          } catch (Exception ignored) {
          }
          latch.countDown();
        });
      }

      latch.await(30, TimeUnit.SECONDS);
      executor.shutdown();

      int recorded = auth.bruteForceProtector().getAttemptCount(uuid);
      assertThat(recorded).isGreaterThan(0);
    }
  }
}
