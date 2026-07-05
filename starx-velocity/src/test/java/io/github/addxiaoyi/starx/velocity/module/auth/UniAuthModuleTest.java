package io.github.addxiaoyi.starx.velocity.module.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthConfig;
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

  UniAuthConfig config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config =
        new UniAuthConfig(
            true,
            "https://api.example.com/uniauth/",
            "test-app-id",
            "test-app-secret",
            5000,
            false);
  }

  @Test
  void shouldReturnCorrectModuleName() {
    UniAuthModule module = new UniAuthModule(plugin, eventBus, config);
    assertThat(module.name()).isEqualTo("auth.uniauth");
  }

  @Test
  void shouldHaveRequiredApiFields() {
    UniAuthModule module = new UniAuthModule(plugin, eventBus, config);
    assertThat(module.getConfig().apiUrl()).isEqualTo("https://api.example.com/uniauth/");
    assertThat(module.getConfig().appId()).isEqualTo("test-app-id");
  }

  @Test
  void shouldDefaultTimeoutBe5000() {
    UniAuthConfig defaultConfig = UniAuthConfig.defaults();
    assertThat(defaultConfig.timeoutMs()).isEqualTo(5000);
  }

  @Test
  void shouldBeDisabledByDefault() {
    UniAuthConfig defaultConfig = UniAuthConfig.defaults();
    assertThat(defaultConfig.enabled()).isFalse();
  }
}
