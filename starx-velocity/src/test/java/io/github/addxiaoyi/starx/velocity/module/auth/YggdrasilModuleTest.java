package io.github.addxiaoyi.starx.velocity.module.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YggdrasilModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  YggdrasilModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config =
        new YggdrasilModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public Map<String, String> servers() {
            return Map.of("mojang", "https://sessionserver.mojang.com/");
          }

          @Override
          public boolean verifyIp() {
            return false;
          }

          @Override
          public int timeout() {
            return 5000;
          }
        };
  }

  @Test
  void shouldReturnCorrectModuleName() {
    YggdrasilModule module = new YggdrasilModule(plugin, eventBus, config);
    assertThat(module.name()).isEqualTo("auth.yggdrasil");
  }

  @Test
  void shouldHaveDefaultServersConfig() {
    YggdrasilModule.Config defaultConfig = YggdrasilModule.Config.defaultConfig();
    assertThat(defaultConfig.servers()).isNotEmpty();
    assertThat(defaultConfig.servers()).containsKey("mojang");
  }

  @Test
  void shouldDefaultTimeoutBe5000() {
    YggdrasilModule.Config defaultConfig = YggdrasilModule.Config.defaultConfig();
    assertThat(defaultConfig.timeout()).isEqualTo(5000);
  }

  @Test
  void shouldVerifyIpBeDisabledByDefault() {
    YggdrasilModule.Config defaultConfig = YggdrasilModule.Config.defaultConfig();
    assertThat(defaultConfig.verifyIp()).isFalse();
  }

  @Test
  void shouldBeEnabledByDefault() {
    YggdrasilModule.Config defaultConfig = YggdrasilModule.Config.defaultConfig();
    assertThat(defaultConfig.enabled()).isTrue();
  }

  @Test
  void shouldSupportMultipleAuthServers() {
    YggdrasilModule.Config multiConfig =
        new YggdrasilModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public Map<String, String> servers() {
            return Map.of(
                "mojang", "https://sessionserver.mojang.com/",
                "littleskin", "https://littleskin.cn/api/yggdrasil/");
          }

          @Override
          public boolean verifyIp() {
            return true;
          }

          @Override
          public int timeout() {
            return 10000;
          }
        };
    YggdrasilModule module = new YggdrasilModule(plugin, eventBus, multiConfig);
    assertThat(module.getServers()).hasSize(2);
    assertThat(module.getServers()).containsKeys("mojang", "littleskin");
  }

  @Test
  void shouldResolveServerUrl() {
    YggdrasilModule module = new YggdrasilModule(plugin, eventBus, config);
    String url = module.resolveServerUrl("mojang", "session/minecraft/hasJoined");
    assertThat(url).startsWith("https://sessionserver.mojang.com/");
    assertThat(url).endsWith("session/minecraft/hasJoined");
  }

  @Test
  void shouldReturnNullForUnknownServer() {
    YggdrasilModule module = new YggdrasilModule(plugin, eventBus, config);
    String url = module.resolveServerUrl("unknown", "test");
    assertThat(url).isNull();
  }
}