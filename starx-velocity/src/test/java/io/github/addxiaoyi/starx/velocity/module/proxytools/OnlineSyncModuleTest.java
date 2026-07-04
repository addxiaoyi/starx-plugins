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
class OnlineSyncModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventManager eventManager;
  @Mock CommandManager commandManager;

  OnlineSyncModule.Config enabledConfig;
  OnlineSyncModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getCommandManager()).thenReturn(commandManager);
    enabledConfig =
        new OnlineSyncModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }
        };
    disabledConfig =
        new OnlineSyncModule.Config() {
          @Override
          public boolean enabled() {
            return false;
          }
        };
  }

  @Test
  void shouldRegisterListenersOnEnable() {
    OnlineSyncModule module = new OnlineSyncModule(plugin, enabledConfig);
    module.onEnable();
    verify(eventManager).register(eq(plugin), any());
  }

  @Test
  void shouldRegisterListCommandOnEnable() {
    OnlineSyncModule module = new OnlineSyncModule(plugin, enabledConfig);
    module.onEnable();
    verify(commandManager).register(eq("list"), any(SimpleCommand.class));
  }

  @Test
  void shouldNotRegisterWhenDisabled() {
    OnlineSyncModule module = new OnlineSyncModule(plugin, disabledConfig);
    module.onEnable();
    verify(eventManager, never()).register(eq(plugin), any());
    verify(commandManager, never()).register(any(String.class), any(SimpleCommand.class));
  }
}
