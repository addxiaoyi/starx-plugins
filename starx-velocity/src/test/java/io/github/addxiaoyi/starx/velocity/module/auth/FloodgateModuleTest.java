package io.github.addxiaoyi.starx.velocity.module.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FloodgateModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  FloodgateModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config =
        new FloodgateModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public boolean autoLogin() {
            return true;
          }

          @Override
          public String prefix() {
            return ".";
          }
        };
  }

  @Test
  void shouldReturnCorrectModuleName() {
    FloodgateModule module = new FloodgateModule(plugin, eventBus, config);
    assertThat(module.name()).isEqualTo("auth.floodgate");
  }

  @Test
  void shouldAutoLoginBeEnabledByDefault() {
    FloodgateModule.Config defaultConfig = FloodgateModule.Config.defaultConfig();
    assertThat(defaultConfig.autoLogin()).isTrue();
  }

  @Test
  void shouldDefaultPrefixBeDot() {
    FloodgateModule.Config defaultConfig = FloodgateModule.Config.defaultConfig();
    assertThat(defaultConfig.prefix()).isEqualTo(".");
  }

  @Test
  void shouldBeEnabledByDefault() {
    FloodgateModule.Config defaultConfig = FloodgateModule.Config.defaultConfig();
    assertThat(defaultConfig.enabled()).isTrue();
  }

  @Test
  void shouldPrefixBeConfigurable() {
    FloodgateModule.Config customConfig =
        new FloodgateModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public boolean autoLogin() {
            return true;
          }

          @Override
          public String prefix() {
            return "*";
          }
        };
    FloodgateModule module = new FloodgateModule(plugin, eventBus, customConfig);
    assertThat(module.getPrefix()).isEqualTo("*");
  }
}