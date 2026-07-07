package io.github.addxiaoyi.starx.velocity.module.proxytools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RakNetModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock Logger logger;

  RakNetModule.Config enabledConfig;
  RakNetModule.Config disabledConfig;
  RakNetModule.Config debugConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.logger()).thenReturn(logger);
    enabledConfig =
        new RakNetModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public int port() {
            return 19132;
          }

          @Override
          public boolean debug() {
            return false;
          }
        };
    disabledConfig =
        new RakNetModule.Config() {
          @Override
          public boolean enabled() {
            return false;
          }

          @Override
          public int port() {
            return 19132;
          }

          @Override
          public boolean debug() {
            return false;
          }
        };
    debugConfig =
        new RakNetModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public int port() {
            return 19133;
          }

          @Override
          public boolean debug() {
            return true;
          }
        };
  }

  @Test
  void shouldHaveCorrectModuleName() {
    RakNetModule module = new RakNetModule(plugin, enabledConfig);
    assertThat(module.name()).isEqualTo("starx.proxytools.raknet");
  }

  @Test
  void shouldInitializeWhenEnabled() {
    RakNetModule module = new RakNetModule(plugin, enabledConfig);
    module.onEnable();
    assertThat(module.isInitialized()).isTrue();
  }

  @Test
  void shouldNotInitializeWhenDisabled() {
    RakNetModule module = new RakNetModule(plugin, disabledConfig);
    module.onEnable();
    assertThat(module.isInitialized()).isFalse();
  }

  @Test
  void shouldCleanUpOnDisable() {
    RakNetModule module = new RakNetModule(plugin, enabledConfig);
    module.onEnable();
    assertThat(module.isInitialized()).isTrue();
    module.onDisable();
    assertThat(module.isInitialized()).isFalse();
  }

  @Test
  void shouldUseConfiguredPort() {
    RakNetModule module = new RakNetModule(plugin, debugConfig);
    assertThat(module.port()).isEqualTo(19133);
  }
}
