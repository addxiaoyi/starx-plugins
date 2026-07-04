package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnhancedProxyModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock CommandManager commandManager;

  EnhancedProxyModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getCommandManager()).thenReturn(commandManager);
    config = EnhancedProxyModule.Config.simpleDefault();
  }

  @Test
  void shouldHaveCorrectModuleName() {
    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    assertEquals("enhanced", module.name());
  }

  @Test
  void shouldRegisterCommandsOnEnable() {
    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    module.onEnable();

    verify(commandManager).register(eq("glist"), any(SimpleCommand.class));
    verify(commandManager).register(eq("find"), any(SimpleCommand.class));
    verify(commandManager).register(eq("send"), any(SimpleCommand.class));
    verify(commandManager).register(eq("alert"), any(SimpleCommand.class));
    verify(commandManager).register(eq("ping"), any(SimpleCommand.class));
    verify(commandManager).register(eq("kickall"), any(SimpleCommand.class));
  }

  @Test
  void shouldNotRegisterCommandsWhenDisabled() {
    EnhancedProxyModule module =
        new EnhancedProxyModule(plugin, EnhancedProxyModule.Config.disabled());
    module.onEnable();

    verify(commandManager, never()).register(any(String.class), any(SimpleCommand.class));
  }

  @Test
  void shouldFindPlayerOnServer() {
    RegisteredServer lobby = serverNamed("lobby");
    Player player = playerOnServer("Alice", lobby);
    when(proxy.getPlayer("Alice")).thenReturn(Optional.of(player));

    SimpleCommand.Invocation invocation = invocationWithArgs("Alice");
    when(invocation.source().hasPermission("starx.commands.find")).thenReturn(true);

    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    module.onEnable();

    SimpleCommand findCmd = module.getFindCommand();
    findCmd.execute(invocation);

    verify(invocation.source()).sendMessage(any(Component.class));
  }

  @Test
  void shouldShowPlayerNotFoundForUnknownPlayer() {
    SimpleCommand.Invocation invocation = invocationWithArgs("UnknownPlayer");
    when(invocation.source().hasPermission("starx.commands.find")).thenReturn(true);
    when(proxy.getPlayer("UnknownPlayer")).thenReturn(Optional.empty());

    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    module.onEnable();

    SimpleCommand findCmd = module.getFindCommand();
    findCmd.execute(invocation);

    verify(invocation.source()).sendMessage(any(Component.class));
  }

  @Test
  void shouldSendPlayerToTargetServer() {
    RegisteredServer target = serverNamed("survival");
    Player player = mock(Player.class);
    when(player.getUsername()).thenReturn("Alice");
    when(player.createConnectionRequest(target))
        .thenReturn(mock(com.velocitypowered.api.proxy.ConnectionRequestBuilder.class));

    when(proxy.getPlayer("Alice")).thenReturn(Optional.of(player));
    when(proxy.getServer("survival")).thenReturn(Optional.of(target));

    SimpleCommand.Invocation invocation = invocationWithArgs("Alice", "survival");
    when(invocation.source().hasPermission("starx.commands.send")).thenReturn(true);

    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    module.onEnable();

    SimpleCommand sendCmd = module.getSendCommand();
    sendCmd.execute(invocation);

    verify(player).createConnectionRequest(target);
    verify(invocation.source()).sendMessage(any(Component.class));
  }

  @Test
  void shouldAlertBroadcastToAllPlayers() {
    Player alice = mock(Player.class);
    Player bob = mock(Player.class);
    RegisteredServer lobby = serverNamed("lobby");
    RegisteredServer game = serverNamed("game");

    when(lobby.getPlayersConnected()).thenReturn(List.of(alice));
    when(game.getPlayersConnected()).thenReturn(List.of(bob));
    when(proxy.getAllServers()).thenReturn(List.of(lobby, game));

    SimpleCommand.Invocation invocation = invocationWithArgs("Server restart");
    when(invocation.source().hasPermission("starx.commands.alert")).thenReturn(true);

    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    module.onEnable();

    SimpleCommand alertCmd = module.getAlertCommand();
    alertCmd.execute(invocation);

    verify(alice).sendMessage(any(Component.class));
    verify(bob).sendMessage(any(Component.class));
  }

  @Test
  void shouldPingSelf() {
    Player player = mock(Player.class);
    when(player.getPing()).thenReturn(42L);

    SimpleCommand.Invocation invocation = invocationWithArgs();
    when(invocation.source()).thenReturn(player);
    when(invocation.source().hasPermission("starx.commands.ping")).thenReturn(true);

    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    module.onEnable();

    SimpleCommand pingCmd = module.getPingCommand();
    pingCmd.execute(invocation);

    verify(invocation.source()).sendMessage(any(Component.class));
  }

  @Test
  void shouldPingTargetPlayer() {
    Player target = mock(Player.class);
    when(target.getUsername()).thenReturn("Bob");
    when(target.getPing()).thenReturn(100L);

    when(proxy.getPlayer("Bob")).thenReturn(Optional.of(target));

    SimpleCommand.Invocation invocation = invocationWithArgs("Bob");
    when(invocation.source().hasPermission("starx.commands.ping")).thenReturn(true);

    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    module.onEnable();

    SimpleCommand pingCmd = module.getPingCommand();
    pingCmd.execute(invocation);

    verify(invocation.source()).sendMessage(any(Component.class));
  }

  @Test
  void shouldKickAllFromServer() {
    Player noBypass = mock(Player.class);
    when(noBypass.hasPermission("starx.kickall.bypass")).thenReturn(false);

    RegisteredServer survival = serverNamed("survival");
    RegisteredServer fallback = serverNamed("fallback");
    when(survival.getPlayersConnected()).thenReturn(List.of(noBypass));
    when(proxy.getAllServers()).thenReturn(List.of(survival, fallback));
    when(proxy.getServer("survival")).thenReturn(Optional.of(survival));

    com.velocitypowered.api.proxy.ConnectionRequestBuilder builder =
        mock(com.velocitypowered.api.proxy.ConnectionRequestBuilder.class);
    when(noBypass.createConnectionRequest(fallback)).thenReturn(builder);

    SimpleCommand.Invocation invocation = invocationWithArgs("survival");
    when(invocation.source().hasPermission("starx.commands.kickall")).thenReturn(true);

    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    module.onEnable();

    SimpleCommand kickAllCmd = module.getKickAllCommand();
    kickAllCmd.execute(invocation);

    verify(noBypass).createConnectionRequest(fallback);
    verify(invocation.source()).sendMessage(any(Component.class));
  }

  @Test
  void shouldBypassKickAllForPlayersWithPermission() {
    Player bypassPlayer = mock(Player.class);
    when(bypassPlayer.hasPermission("starx.kickall.bypass")).thenReturn(true);

    Player noBypass = mock(Player.class);
    when(noBypass.hasPermission("starx.kickall.bypass")).thenReturn(false);

    RegisteredServer survival = serverNamed("survival");
    RegisteredServer fallback = serverNamed("fallback");
    when(survival.getPlayersConnected()).thenReturn(List.of(bypassPlayer, noBypass));
    when(proxy.getAllServers()).thenReturn(List.of(survival, fallback));
    when(proxy.getServer("survival")).thenReturn(Optional.of(survival));

    com.velocitypowered.api.proxy.ConnectionRequestBuilder builder =
        mock(com.velocitypowered.api.proxy.ConnectionRequestBuilder.class);
    when(noBypass.createConnectionRequest(fallback)).thenReturn(builder);

    SimpleCommand.Invocation invocation = invocationWithArgs("survival");
    when(invocation.source().hasPermission("starx.commands.kickall")).thenReturn(true);

    EnhancedProxyModule module = new EnhancedProxyModule(plugin, config);
    module.onEnable();

    SimpleCommand kickAllCmd = module.getKickAllCommand();
    kickAllCmd.execute(invocation);

    verify(bypassPlayer, never()).createConnectionRequest(any(RegisteredServer.class));
    verify(noBypass).createConnectionRequest(fallback);
  }

  private RegisteredServer serverNamed(String name) {
    RegisteredServer server = mock(RegisteredServer.class);
    ServerInfo info =
        new ServerInfo(name, java.net.InetSocketAddress.createUnresolved("localhost", 25565));
    lenient().when(server.getServerInfo()).thenReturn(info);
    lenient().when(server.getPlayersConnected()).thenReturn(List.of());
    return server;
  }

  private Player playerOnServer(String username, RegisteredServer server) {
    Player player = mock(Player.class);
    ServerConnection connection = mock(ServerConnection.class);
    ServerInfo serverInfo = server.getServerInfo();
    lenient().when(connection.getServer()).thenReturn(server);
    lenient().when(connection.getServerInfo()).thenReturn(serverInfo);
    lenient().when(player.getCurrentServer()).thenReturn(Optional.of(connection));
    lenient().when(player.getUsername()).thenReturn(username);
    lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    return player;
  }

  private SimpleCommand.Invocation invocationWithArgs(String... args) {
    SimpleCommand.Invocation invocation = mock(SimpleCommand.Invocation.class);
    com.velocitypowered.api.command.CommandSource source =
        mock(com.velocitypowered.api.command.CommandSource.class);
    lenient().when(invocation.source()).thenReturn(source);
    lenient().when(invocation.arguments()).thenReturn(args);
    return invocation;
  }
}
