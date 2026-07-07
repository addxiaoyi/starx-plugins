package io.github.addxiaoyi.starx.common.stress;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.DatabaseManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("stress")
class DatabaseConnectionPoolStressTest {

  @Test
  void concurrentReadWriteOnH2() throws Exception {
    DatabaseConfig config =
        new DatabaseConfig(
            "h2",
            "",
            0,
            "stress_h2",
            "sa",
            "",
            "jdbc:h2:mem:stress_h2;DB_CLOSE_DELAY=-1",
            10,
            10_000L);
    try (DatabaseManager manager = new DatabaseManager(config)) {
      Jdbi jdbi = manager.getJdbi();
      prepareTable(jdbi);
      runConcurrentTest(jdbi, 20, 500);
    }
  }

  @Test
  void concurrentReadWriteOnSqlite() throws Exception {
    Path tempFile = Files.createTempFile("stress_sqlite", ".db");
    tempFile.toFile().deleteOnExit();
    DatabaseConfig config =
        new DatabaseConfig("sqlite", "", 0, tempFile.toString(), "", "", "", 10, 10_000L);
    try (DatabaseManager manager = new DatabaseManager(config)) {
      Jdbi jdbi = manager.getJdbi();
      prepareTable(jdbi);
      runConcurrentTest(jdbi, 4, 100);
    }
  }

  @Test
  void poolExhaustionRecovery() throws Exception {
    DatabaseConfig config =
        new DatabaseConfig(
            "h2",
            "",
            0,
            "stress_pool",
            "sa",
            "",
            "jdbc:h2:mem:stress_pool;DB_CLOSE_DELAY=-1",
            2,
            1_500L);
    try (DatabaseManager manager = new DatabaseManager(config)) {
      Jdbi jdbi = manager.getJdbi();
      prepareTable(jdbi);

      // Exhaust pool with 6 concurrent long-running transactions
      ExecutorService pool = Executors.newFixedThreadPool(6);
      CountDownLatch latch = new CountDownLatch(6);
      AtomicInteger failed = new AtomicInteger(0);
      for (int i = 0; i < 6; i++) {
        final int idx = i;
        pool.submit(
            () -> {
              try {
                jdbi.useTransaction(
                    handle -> {
                      handle.execute(
                          "INSERT INTO stress_test (id, val, version) VALUES (?, ?, 1)",
                          1000 + idx,
                          "pool-test-" + idx);
                      Thread.sleep(1_200);
                    });
              } catch (Exception e) {
                failed.incrementAndGet();
              }
              latch.countDown();
            });
      }
      latch.await(10, TimeUnit.SECONDS);
      pool.shutdown();
      assertThat(failed.get()).isGreaterThan(0);
    }
  }

  private void prepareTable(Jdbi jdbi) {
    jdbi.withHandle(
        handle -> {
          handle.execute(
              "CREATE TABLE IF NOT EXISTS stress_test ("
                  + "id INT PRIMARY KEY, val VARCHAR(255), version INT)");
          handle.execute("DELETE FROM stress_test");
          for (int i = 0; i < 100; i++) {
            handle.execute(
                "INSERT INTO stress_test (id, val, version) VALUES (?, ?, 1)", i, "initial-" + i);
          }
          return null;
        });
  }

  private void runConcurrentTest(Jdbi jdbi, int threadCount, int opsPerThread) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
    AtomicInteger totalOps = new AtomicInteger(0);

    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      executor.submit(
          () -> {
            try {
              for (int op = 0; op < opsPerThread; op++) {
                int fop = op;
                int fid = (threadId * opsPerThread + fop) % 100;
                jdbi.useHandle(
                    handle ->
                        handle.execute(
                            "UPDATE stress_test SET val = ?, version = version + 1 WHERE id = ?",
                            "written-by-" + threadId + "-" + fop,
                            fid));
                totalOps.incrementAndGet();
              }
            } catch (Throwable e) {
              errors.add(e);
            }
            latch.countDown();
          });
    }

    latch.await(60, TimeUnit.SECONDS);
    executor.shutdown();
    assertThat(errors).as("Concurrent stress test errors: " + errors).isEmpty();
    assertThat(totalOps.get()).isGreaterThan(0);
  }
}
