package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconnectModuleTest {

  @Mock
  StarxVelocityPlugin plugin;
  @Mock
  ProxyServer proxy;
  @Mock
  EventManager eventManager;
  @Mock
  Scheduler scheduler;

  ReconnectModule.Config enabledConfig;
  ReconnectModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getScheduler()).thenReturn(scheduler);
    ScheduledTask mockTask = mock(ScheduledTask.class);
    Scheduler.TaskBuilder taskBuilder = mock(Scheduler.TaskBuilder.class);
    lenient().when(scheduler.buildTask(any(), any(Runnable.class))).thenReturn(taskBuilder);
    lenient().when(taskBuilder.delay(any(Long.class), any(TimeUnit.class))).thenReturn(taskBuilder);
    lenient().when(taskBuilder.schedule()).thenReturn(mockTask);
    enabledConfig = new ReconnectModule.Config() {
      @Override
      public boolean enabled() {
        return true;
      }
    };
    disabledConfig = new ReconnectModule.Config() {
      @Override
      public boolean enabled() {
        return false;
      }
    };
  }

  @Test
  void shouldRegisterEventListenersOnEnable() {
    ReconnectModule module = new ReconnectModule(plugin, enabledConfig);
    module.onEnable();
    verify(eventManager).register(eq(plugin), any());
  }

  @Test
  void shouldRecordLastServerOnDisconnect() {
    RegisteredServer server = serverNamed("game");
    Player player = playerWithServer("Alice", server);

    ReconnectModule module = new ReconnectModule(plugin, enabledConfig);
    module.onDisconnect(new DisconnectEvent(player, LoginStatus.SUCCESSFUL_LOGIN));

    Optional<String> lastServer = module.getLastServer(player.getUniqueId());
    assert lastServer.isPresent();
    assert lastServer.get().equals("game");
  }

  @Test
  void shouldNotRecordWhenDisabled() {
    Player player = playerWithServer("Alice", serverNamed("game"));
    ReconnectModule module = new ReconnectModule(plugin, disabledConfig);
    module.onDisconnect(new DisconnectEvent(player, LoginStatus.SUCCESSFUL_LOGIN));
    assert module.getLastServer(player.getUniqueId()).isEmpty();
  }

  @Test
  void shouldReturnEmptyForUnknownPlayer() {
    ReconnectModule module = new ReconnectModule(plugin, enabledConfig);
    assert module.getLastServer(UUID.randomUUID()).isEmpty();
  }

  @Test
  void shouldAttemptRedirectOnLoginIfLastServerExists() {
    RegisteredServer targetServer = serverNamed("game");
    when(proxy.getServer("game")).thenReturn(Optional.of(targetServer));
    UUID fixedUuid = UUID.randomUUID();
    Player player = playerWithFixedUuid("Alice", targetServer, fixedUuid);

    ReconnectModule module = new ReconnectModule(plugin, enabledConfig);
    module.onDisconnect(new DisconnectEvent(player, LoginStatus.SUCCESSFUL_LOGIN));

    LoginEvent loginEvent = mock(LoginEvent.class);
    when(loginEvent.getPlayer()).thenReturn(player);
    module.onLogin(loginEvent);

    assert module.getLastServer(fixedUuid).isPresent();
  }

  private Player playerWithServer(String username, RegisteredServer server) {
    return playerWithFixedUuid(username, server, UUID.randomUUID());
  }

  private Player playerWithFixedUuid(String username, RegisteredServer server, UUID uuid) {
    Player player = mock(Player.class);
    ServerConnection connection = mock(ServerConnection.class);
    lenient().when(connection.getServer()).thenReturn(server);
    lenient().when(player.getCurrentServer()).thenReturn(Optional.of(connection));
    lenient().when(player.getUsername()).thenReturn(username);
    lenient().when(player.getUniqueId()).thenReturn(uuid);
    return player;
  }

  private RegisteredServer serverNamed(String name) {
    RegisteredServer server = mock(RegisteredServer.class);
    ServerInfo info = mock(ServerInfo.class);
    lenient().when(info.getName()).thenReturn(name);
    lenient().when(server.getServerInfo()).thenReturn(info);
    return server;
  }
}