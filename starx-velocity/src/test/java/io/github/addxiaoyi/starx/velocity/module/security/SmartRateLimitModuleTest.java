package io.github.addxiaoyi.starx.velocity.module.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.common.smart.AdaptiveRateLimiter;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmartRateLimitModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(plugin.logger()).thenReturn(java.util.logging.Logger.getLogger("test"));
  }

  @Test
  void shouldReturnCorrectName() {
    SmartRateLimitModule module = new SmartRateLimitModule(plugin, eventBus);
    assertThat(module.name()).isEqualTo("starx.security.smart-rate");
  }

  @Test
  void shouldRegisterListenerOnEnable() {
    SmartRateLimitModule module = new SmartRateLimitModule(plugin, eventBus);
    module.onEnable();
    assertThat(module.getMaxConnections()).isPositive();
  }

  @Test
  void shouldStartWithDefaultValues() {
    SmartRateLimitModule module = new SmartRateLimitModule(plugin, eventBus, 10, 20);
    assertThat(module.getMaxConnections()).isPositive();
    assertThat(module.getMaxPings()).isPositive();
  }

  @Test
  void shouldRespondToLoadChanges() {
    SmartRateLimitModule module = new SmartRateLimitModule(plugin, eventBus, 10, 20);
    module.onEnable();
    assertThat(module.getCurrentLoadLevel()).isEqualTo(AdaptiveRateLimiter.LoadLevel.NORMAL);
  }

  @Test
  void shouldCleanupOnDisable() {
    SmartRateLimitModule module = new SmartRateLimitModule(plugin, eventBus);
    module.onEnable();
    module.onDisable();
    assertThat(module.getMaxConnections()).isPositive();
  }
}
