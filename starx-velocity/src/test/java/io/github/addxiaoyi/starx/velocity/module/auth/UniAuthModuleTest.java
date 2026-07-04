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
class UniAuthModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  UniAuthModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config =
        new UniAuthModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public String apiUrl() {
            return "https://api.example.com/uniauth/";
          }

          @Override
          public String appId() {
            return "test-app-id";
          }

          @Override
          public String appSecret() {
            return "test-app-secret";
          }

          @Override
          public int timeout() {
            return 5000;
          }
        };
  }

  @Test
  void shouldReturnCorrectModuleName() {
    UniAuthModule module = new UniAuthModule(plugin, eventBus, config);
    assertThat(module.name()).isEqualTo("auth.uniauth");
  }

  @Test
  void shouldHaveRequiredApiFields() {
    UniAuthModule module = new UniAuthModule(plugin, eventBus, config);
    assertThat(module.getApiUrl()).isEqualTo("https://api.example.com/uniauth/");
    assertThat(module.getAppId()).isEqualTo("test-app-id");
  }

  @Test
  void shouldDefaultTimeoutBe5000() {
    UniAuthModule.Config defaultConfig = UniAuthModule.Config.defaultConfig();
    assertThat(defaultConfig.timeout()).isEqualTo(5000);
  }

  @Test
  void shouldBeDisabledByDefault() {
    UniAuthModule.Config defaultConfig = UniAuthModule.Config.defaultConfig();
    assertThat(defaultConfig.enabled()).isFalse();
  }
}
