package io.github.addxiaoyi.starx.velocity.module.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MigrationModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  MigrationModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config =
        new MigrationModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public String source() {
            return "starx.multilogin";
          }

          @Override
          public String backend() {
            return "starx.mysql";
          }

          @Override
          public Map<String, Object> connection() {
            return Map.of(
                "host", "localhost",
                "port", 3306,
                "database", "multilogin",
                "username", "root",
                "password", "");
          }
        };
  }

  @Test
  void shouldReturnCorrectModuleName() {
    MigrationModule module = new MigrationModule(plugin, eventBus, config);
    assertThat(module.name()).isEqualTo("starx.auth.migration");
  }

  @Test
  void shouldSupportMultipleSources() {
    MigrationModule.Config multiLoginConfig = config;
    assertThat(multiLoginConfig.source()).isEqualTo("starx.multilogin");

    MigrationModule.Config authmeConfig =
        new MigrationModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public String source() {
            return "starx.authme";
          }

          @Override
          public String backend() {
            return "starx.sqlite";
          }

          @Override
          public Map<String, Object> connection() {
            return Map.of("path", "/data/authme.db");
          }
        };
    assertThat(authmeConfig.source()).isEqualTo("starx.authme");
    assertThat(authmeConfig.backend()).isEqualTo("starx.sqlite");
  }

  @Test
  void shouldBeDisabledByDefault() {
    MigrationModule.Config defaultConfig = MigrationModule.Config.defaultConfig();
    assertThat(defaultConfig.enabled()).isFalse();
  }
}
