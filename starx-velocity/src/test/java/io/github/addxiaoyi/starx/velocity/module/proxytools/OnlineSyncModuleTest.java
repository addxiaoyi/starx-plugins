package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
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
@SuppressWarnings("deprecation")
class OnlineSyncModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventManager eventManager;
  @Mock CommandManager commandManager;
  @Mock CommandMeta.Builder metaBuilder;
  @Mock CommandMeta meta;

  OnlineSyncModule.Config enabledConfig;
  OnlineSyncModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getCommandManager()).thenReturn(commandManager);
    lenient().when(commandManager.metaBuilder(any(String.class))).thenReturn(metaBuilder);
    lenient().when(metaBuilder.build()).thenReturn(meta);
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
    verify(commandManager).register(any(CommandMeta.class), any(SimpleCommand.class));
  }

  @Test
  void shouldNotRegisterWhenDisabled() {
    OnlineSyncModule module = new OnlineSyncModule(plugin, disabledConfig);
    module.onEnable();
    verify(eventManager, never()).register(eq(plugin), any());
    verify(commandManager, never()).register(any(CommandMeta.class), any(SimpleCommand.class));
  }
}
