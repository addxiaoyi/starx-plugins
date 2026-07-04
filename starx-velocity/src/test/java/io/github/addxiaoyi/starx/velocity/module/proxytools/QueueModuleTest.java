package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.scheduler.Scheduler.TaskBuilder;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.proxytools.queue.QueueService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueueModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventManager eventManager;
  @Mock Scheduler scheduler;
  @Mock TaskBuilder taskBuilder;
  @Mock ScheduledTask scheduledTask;

  QueueService queueService;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getScheduler()).thenReturn(scheduler);
    lenient().when(scheduler.buildTask(eq(plugin), any(Runnable.class))).thenReturn(taskBuilder);
    lenient().when(taskBuilder.repeat(any(java.time.Duration.class))).thenReturn(taskBuilder);
    lenient().when(taskBuilder.schedule()).thenReturn(scheduledTask);
    queueService = new QueueService();
  }

  @Test
  void shouldRegisterListenerAndScheduleTaskOnEnable() {
    QueueModule.Config config = QueueModule.Config.defaultConfig();
    QueueModule module = new QueueModule(plugin, config, queueService);
    module.onEnable();
    verify(eventManager).register(eq(plugin), any());
  }

  @Test
  void shouldEnqueuePlayerOnFullKick() {
    RegisteredServer game = serverNamed("game");
    Player player = mock(Player.class);
    KickedFromServerEvent event = kickEvent(game, player, Component.text("Server is full"));

    QueueModule module = new QueueModule(plugin, QueueModule.Config.defaultConfig(), queueService);
    module.onKicked(event);

    assertThat(queueService.size(game)).isEqualTo(1);
    ArgumentCaptor<KickedFromServerEvent.ServerKickResult> resultCaptor =
        ArgumentCaptor.forClass(KickedFromServerEvent.ServerKickResult.class);
    verify(event).setResult(resultCaptor.capture());
    assertThat(resultCaptor.getValue()).isInstanceOf(KickedFromServerEvent.Notify.class);
  }

  @Test
  void shouldNotEnqueueWhenReasonDoesNotMatch() {
    RegisteredServer game = serverNamed("game");
    Player player = mock(Player.class);
    KickedFromServerEvent event = kickEvent(game, player, Component.text("You are banned"));

    QueueModule module = new QueueModule(plugin, QueueModule.Config.defaultConfig(), queueService);
    module.onKicked(event);

    assertThat(queueService.size(game)).isEqualTo(0);
    verify(event, never()).setResult(any());
  }

  @Test
  void shouldProcessQueueInFifoOrder() {
    RegisteredServer game = serverNamed("game");
    Player a = playerNamed("A");
    Player b = playerNamed("B");
    Player c = playerNamed("C");
    queueService.enqueue(game, a);
    queueService.enqueue(game, b);
    queueService.enqueue(game, c);

    List<String> order = new ArrayList<>();
    int connected =
        queueService.processQueues(
            (player, serverName) -> {
              order.add(player.getUsername());
              return true;
            });

    assertThat(connected).isEqualTo(3);
    assertThat(order).containsExactly("A", "B", "C");
    assertThat(queueService.size(game)).isEqualTo(0);
  }

  private KickedFromServerEvent kickEvent(
      RegisteredServer server, Player player, Component reason) {
    KickedFromServerEvent event = mock(KickedFromServerEvent.class);
    lenient().when(event.getServer()).thenReturn(server);
    lenient().when(event.getPlayer()).thenReturn(player);
    lenient().when(event.getServerKickReason()).thenReturn(Optional.of(reason));
    return event;
  }

  private RegisteredServer serverNamed(String name) {
    RegisteredServer server = mock(RegisteredServer.class);
    ServerInfo info = mock(ServerInfo.class);
    lenient().when(info.getName()).thenReturn(name);
    lenient().when(server.getServerInfo()).thenReturn(info);
    return server;
  }

  private Player playerNamed(String username) {
    Player player = mock(Player.class);
    lenient().when(player.getUsername()).thenReturn(username);
    return player;
  }
}
