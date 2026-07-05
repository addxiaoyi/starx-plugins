package io.github.addxiaoyi.starx.velocity.module.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.common.security.BushClient;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlossomGuardModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventManager eventManager;
  @Mock BushClient bushClient;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
  }

  @Test
  void shouldReturnCorrectName() {
    BlossomGuardModule module = new BlossomGuardModule(plugin, bushClient);
    assertThat(module.name()).isEqualTo("security.blossom");
  }

  @Test
  void shouldRegisterEventListenersOnEnable() {
    BlossomGuardModule module = new BlossomGuardModule(plugin, bushClient);
    module.onEnable();
    verify(eventManager, times(1)).register(org.mockito.ArgumentMatchers.eq(plugin), any());
  }

  @Test
  void shouldConstructWithDefaultBushClient() {
    BlossomGuardModule module = new BlossomGuardModule(plugin);
    assertThat(module.getBushClient()).isNotNull();
  }

  @Test
  void shouldExposeBushClient() {
    BlossomGuardModule module = new BlossomGuardModule(plugin, bushClient);
    assertThat(module.getBushClient()).isSameAs(bushClient);
  }
}
