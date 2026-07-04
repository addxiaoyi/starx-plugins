package io.github.addxiaoyi.starx.paper.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import java.util.concurrent.TimeUnit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchedulerAdapterTest {

  @Mock Plugin plugin;
  @Mock Server server;
  @Mock BukkitScheduler bukkitScheduler;

  @BeforeEach
  void setUp() {
    when(plugin.getServer()).thenReturn(server);
  }

  @Test
  void runGlobal_usesBukkitScheduler_whenNotFolia() {
    when(server.getScheduler()).thenReturn(bukkitScheduler);
    SchedulerAdapter adapter = new SchedulerAdapter(plugin, false);

    Runnable task = () -> {};
    adapter.runGlobal(task);

    verify(bukkitScheduler).runTask(eq(plugin), eq(task));
  }

  @Test
  void runGlobalDelayed_usesBukkitScheduler_whenNotFolia() {
    when(server.getScheduler()).thenReturn(bukkitScheduler);
    SchedulerAdapter adapter = new SchedulerAdapter(plugin, false);

    Runnable task = () -> {};
    adapter.runGlobalDelayed(task, 20L);

    verify(bukkitScheduler).runTaskLater(eq(plugin), eq(task), eq(20L));
  }

  @Test
  void runAsync_usesBukkitScheduler_whenNotFolia() {
    when(server.getScheduler()).thenReturn(bukkitScheduler);
    SchedulerAdapter adapter = new SchedulerAdapter(plugin, false);

    Runnable task = () -> {};
    adapter.runAsync(task);

    verify(bukkitScheduler).runTaskAsynchronously(eq(plugin), eq(task));
  }

  @Test
  void runAsyncDelayed_usesBukkitScheduler_whenNotFolia() {
    when(server.getScheduler()).thenReturn(bukkitScheduler);
    SchedulerAdapter adapter = new SchedulerAdapter(plugin, false);

    Runnable task = () -> {};
    adapter.runAsyncDelayed(task, 1L, TimeUnit.SECONDS);

    verify(bukkitScheduler).runTaskLaterAsynchronously(eq(plugin), eq(task), eq(1L));
  }

  @Test
  void runGlobal_usesGlobalRegionScheduler_whenFolia(@Mock GlobalRegionScheduler globalScheduler) {
    when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
    SchedulerAdapter adapter = new SchedulerAdapter(plugin, true);

    Runnable task = () -> {};
    adapter.runGlobal(task);

    verify(globalScheduler).execute(eq(plugin), eq(task));
  }

  @Test
  void runGlobalDelayed_usesGlobalRegionScheduler_whenFolia(
      @Mock GlobalRegionScheduler globalScheduler) {
    when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
    SchedulerAdapter adapter = new SchedulerAdapter(plugin, true);

    Runnable task = () -> {};
    adapter.runGlobalDelayed(task, 20L);

    verify(globalScheduler).runDelayed(eq(plugin), any(), eq(20L));
  }

  @Test
  void runAsync_usesAsyncScheduler_whenFolia(
      @Mock GlobalRegionScheduler globalScheduler, @Mock AsyncScheduler asyncScheduler) {
    when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
    when(server.getAsyncScheduler()).thenReturn(asyncScheduler);
    SchedulerAdapter adapter = new SchedulerAdapter(plugin, true);

    Runnable task = () -> {};
    adapter.runAsync(task);

    verify(asyncScheduler).runNow(eq(plugin), any());
  }

  @Test
  void runAsyncDelayed_usesAsyncScheduler_whenFolia(
      @Mock GlobalRegionScheduler globalScheduler, @Mock AsyncScheduler asyncScheduler) {
    when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
    when(server.getAsyncScheduler()).thenReturn(asyncScheduler);
    SchedulerAdapter adapter = new SchedulerAdapter(plugin, true);

    Runnable task = () -> {};
    adapter.runAsyncDelayed(task, 500L, TimeUnit.MILLISECONDS);

    verify(asyncScheduler).runDelayed(eq(plugin), any(), eq(500L), eq(TimeUnit.MILLISECONDS));
  }
}
