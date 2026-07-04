package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimboHubModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventManager eventManager;
  @Mock CommandManager commandManager;

  LimboHubModule.Config enabledConfig;
  LimboHubModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getCommandManager()).thenReturn(commandManager);
    enabledConfig =
        new LimboHubModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public String hubServerName() {
            return "lobby";
          }
        };
    disabledConfig =
        new LimboHubModule.Config() {
          @Override
          public boolean enabled() {
            return false;
          }

          @Override
          public String hubServerName() {
            return "lobby";
          }
        };
  }

  @Test
  void shouldRegisterHubCommandOnEnable() {
    LimboHubModule module = new LimboHubModule(plugin, enabledConfig);
    module.onEnable();
    verify(commandManager).register(eq("hub"), any(SimpleCommand.class));
  }

  @Test
  void shouldRegisterLobbyCommandOnEnable() {
    LimboHubModule module = new LimboHubModule(plugin, enabledConfig);
    module.onEnable();
    verify(commandManager).register(eq("lobby"), any(SimpleCommand.class));
  }

  @Test
  void shouldNotRegisterCommandsWhenDisabled() {
    LimboHubModule module = new LimboHubModule(plugin, disabledConfig);
    module.onEnable();
    verify(commandManager, never()).register(any(String.class), any(SimpleCommand.class));
  }
}
