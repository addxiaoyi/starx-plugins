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
class TabIntegrationModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  TabIntegrationModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config =
        new TabIntegrationModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public Map<String, String> placeholders() {
            return Map.of(
                "%starx_auth_status%", "auth_status",
                "%starx_2fa_status%", "2fa_status");
          }
        };
  }

  @Test
  void shouldReturnCorrectModuleName() {
    TabIntegrationModule module = new TabIntegrationModule(plugin, eventBus, config);
    assertThat(module.name()).isEqualTo("starx.auth.tab");
  }

  @Test
  void shouldProvideDefaultPlaceholders() {
    TabIntegrationModule.Config defaultConfig = TabIntegrationModule.Config.defaultConfig();
    assertThat(defaultConfig.placeholders()).isNotEmpty();
    assertThat(defaultConfig.placeholders()).containsKey("%starx_auth_status%");
  }

  @Test
  void shouldBeEnabledByDefault() {
    TabIntegrationModule.Config defaultConfig = TabIntegrationModule.Config.defaultConfig();
    assertThat(defaultConfig.enabled()).isTrue();
  }

  @Test
  void shouldRegisterCustomPlaceholders() {
    TabIntegrationModule module = new TabIntegrationModule(plugin, eventBus, config);
    Map<String, String> placeholders = module.getPlaceholders();
    assertThat(placeholders).hasSize(2);
    assertThat(placeholders).containsKeys("%starx_auth_status%", "%starx_2fa_status%");
  }
}
