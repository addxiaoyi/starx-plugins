package io.github.addxiaoyi.starx.common.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SchedulerTest {

  @Test
  void schedulerApiCanScheduleTasks() throws Exception {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    CountDownLatch latch = new CountDownLatch(3);

    Scheduler scheduler =
        new Scheduler() {
          @Override
          public void runAsync(Runnable task) {
            executor.submit(task);
          }

          @Override
          public void runLater(Runnable task, long delay, TimeUnit unit) {
            executor.schedule(task, delay, unit);
          }

          @Override
          public void runRepeating(Runnable task, long delay, long period, TimeUnit unit) {
            executor.scheduleAtFixedRate(task, delay, period, unit);
          }
        };

    scheduler.runAsync(latch::countDown);
    scheduler.runLater(latch::countDown, 10, TimeUnit.MILLISECONDS);
    scheduler.runRepeating(latch::countDown, 5, 100, TimeUnit.MILLISECONDS);

    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();

    executor.shutdownNow();
  }
}
