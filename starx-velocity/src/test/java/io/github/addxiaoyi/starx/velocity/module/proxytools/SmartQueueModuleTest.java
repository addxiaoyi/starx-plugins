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
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.proxytools.smart.SmartQueueService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmartQueueModuleTest {

  private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID UUID_C = UUID.fromString("00000000-0000-0000-0000-000000000003");
  private static final UUID UUID_VIP = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventManager eventManager;
  @Mock Scheduler scheduler;
  @Mock Scheduler.TaskBuilder taskBuilder;
  @Mock ScheduledTask scheduledTask;

  SmartQueueService queueService;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getScheduler()).thenReturn(scheduler);
    lenient().when(scheduler.buildTask(eq(plugin), any(Runnable.class))).thenReturn(taskBuilder);
    lenient().when(taskBuilder.repeat(any(Duration.class))).thenReturn(taskBuilder);
    lenient().when(taskBuilder.schedule()).thenReturn(scheduledTask);
    lenient().when(plugin.logger()).thenReturn(java.util.logging.Logger.getLogger("test"));
    queueService = new SmartQueueService();
  }

  @Test
  void shouldReturnCorrectName() {
    SmartQueueModule module =
        new SmartQueueModule(plugin, SmartQueueModule.Config.defaultConfig(), queueService);
    assertThat(module.name()).isEqualTo("proxytools.smart-queue");
  }

  @Test
  void shouldRegisterListenerOnEnable() {
    SmartQueueModule module =
        new SmartQueueModule(plugin, SmartQueueModule.Config.defaultConfig(), queueService);
    module.onEnable();
    verify(eventManager).register(eq(plugin), any());
  }

  @Test
  void shouldEnqueueOnFullKick() {
    RegisteredServer game = serverNamed("game");
    Player player = vipPlayer("VipPlayer", UUID_VIP);
    KickedFromServerEvent event = kickEvent(game, player, Component.text("Server is full"));

    SmartQueueModule module =
        new SmartQueueModule(plugin, SmartQueueModule.Config.defaultConfig(), queueService);
    module.onKicked(event);

    assertThat(queueService.size(game)).isEqualTo(1);
    ArgumentCaptor<KickedFromServerEvent.ServerKickResult> resultCaptor =
        ArgumentCaptor.forClass(KickedFromServerEvent.ServerKickResult.class);
    verify(event).setResult(resultCaptor.capture());
    assertThat(resultCaptor.getValue()).isInstanceOf(KickedFromServerEvent.Notify.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldNotEnqueueWhenReasonDoesNotMatch() {
    RegisteredServer game = serverNamed("game");
    Player player = mock(Player.class);
    lenient().when(player.getUniqueId()).thenReturn(UUID_A);
    KickedFromServerEvent event = kickEvent(game, player, Component.text("You are banned"));

    SmartQueueModule module =
        new SmartQueueModule(plugin, SmartQueueModule.Config.defaultConfig(), queueService);
    module.onKicked(event);

    assertThat(queueService.size(game)).isEqualTo(0);
    verify(event, never()).setResult(any());
  }

  @Test
  void shouldProcessQueueByPriority() {
    RegisteredServer game = serverNamed("game");
    Player normalA = normalPlayer("NormalA", UUID_A);
    Player normalB = normalPlayer("NormalB", UUID_B);
    Player vip = vipPlayer("VipPlayer", UUID_VIP);

    queueService.recordJoin(vip);
    queueService.recordJoin(normalA);
    queueService.recordJoin(normalB);

    queueService.enqueue(game, vip, 500);
    queueService.enqueue(game, normalA, 200);
    queueService.enqueue(game, normalB, 100);

    List<String> order = new ArrayList<>();
    queueService.processQueues(
        (player, serverName) -> {
          order.add(player.getUsername());
          return true;
        },
        5);

    assertThat(order.get(0)).isEqualTo("VipPlayer");
    assertThat(order).hasSize(3);
    assertThat(queueService.size(game)).isEqualTo(0);
  }

  @Test
  void shouldRespectMaxReleaseLimit() {
    RegisteredServer game = serverNamed("game");
    Player a = normalPlayer("A", UUID_A);
    Player b = normalPlayer("B", UUID_B);
    Player c = normalPlayer("C", UUID_C);

    queueService.enqueue(game, a, 100);
    queueService.enqueue(game, b, 100);
    queueService.enqueue(game, c, 100);

    List<String> order = new ArrayList<>();
    int connected =
        queueService.processQueues(
            (player, serverName) -> {
              order.add(player.getUsername());
              return true;
            },
            2);

    assertThat(connected).isEqualTo(2);
    assertThat(order).hasSize(2);
    assertThat(queueService.size(game)).isEqualTo(1);
  }

  @Test
  void shouldReportReleaseRate() {
    SmartQueueModule module =
        new SmartQueueModule(plugin, SmartQueueModule.Config.defaultConfig(), queueService);
    assertThat(module.getReleaseRate()).isPositive();
  }

  @Test
  void shouldReportLoadLevel() {
    SmartQueueModule module =
        new SmartQueueModule(plugin, SmartQueueModule.Config.defaultConfig(), queueService);
    assertThat(module.getCurrentLoadLevel()).isNotNull();
  }

  private RegisteredServer serverNamed(String name) {
    RegisteredServer server = mock(RegisteredServer.class);
    ServerInfo info = mock(ServerInfo.class);
    lenient().when(info.getName()).thenReturn(name);
    lenient().when(server.getServerInfo()).thenReturn(info);
    return server;
  }

  private KickedFromServerEvent kickEvent(
      RegisteredServer server, Player player, Component reason) {
    KickedFromServerEvent event = mock(KickedFromServerEvent.class);
    lenient().when(event.getServer()).thenReturn(server);
    lenient().when(event.getPlayer()).thenReturn(player);
    lenient().when(event.getServerKickReason()).thenReturn(Optional.of(reason));
    return event;
  }

  private Player vipPlayer(String username, UUID uuid) {
    Player player = mock(Player.class);
    lenient().when(player.getUsername()).thenReturn(username);
    lenient().when(player.getUniqueId()).thenReturn(uuid);
    lenient().when(player.hasPermission("starx.vip")).thenReturn(true);
    return player;
  }

  private Player normalPlayer(String username, UUID uuid) {
    Player player = mock(Player.class);
    lenient().when(player.getUsername()).thenReturn(username);
    lenient().when(player.getUniqueId()).thenReturn(uuid);
    lenient().when(player.hasPermission("starx.vip")).thenReturn(false);
    return player;
  }
}
