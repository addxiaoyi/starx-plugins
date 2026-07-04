package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForgeCompatModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventManager eventManager;

  ForgeCompatModule.Config enabledConfig;
  ForgeCompatModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    enabledConfig =
        new ForgeCompatModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public boolean debug() {
            return false;
          }
        };
    disabledConfig =
        new ForgeCompatModule.Config() {
          @Override
          public boolean enabled() {
            return false;
          }

          @Override
          public boolean debug() {
            return false;
          }
        };
  }

  @Test
  void shouldRegisterListenersOnEnable() {
    ForgeCompatModule module = new ForgeCompatModule(plugin, enabledConfig);
    module.onEnable();
    verify(eventManager).register(eq(plugin), any());
  }

  @Test
  void shouldNotRegisterListenersWhenDisabled() {
    ForgeCompatModule module = new ForgeCompatModule(plugin, disabledConfig);
    module.onEnable();
    verify(eventManager, never()).register(eq(plugin), any());
  }
}
