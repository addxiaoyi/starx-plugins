package io.github.addxiaoyi.starx.velocity.module.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrashFixModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;

  CrashFixModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    config = CrashFixModule.Config.defaultConfig();
  }

  @Test
  void shouldReturnCorrectName() {
    CrashFixModule module = new CrashFixModule(plugin, eventBus, config);
    assertThat(module.name()).isEqualTo("security.crash");
  }

  @Test
  void shouldRegisterEventListenersOnEnable() {
    CrashFixModule module = new CrashFixModule(plugin, eventBus, config);
    module.onEnable();

    verify(eventManager).register(any(), any());
  }

  @Test
  void shouldBlockOversizedPluginMessage() {
    CrashFixModule module = new CrashFixModule(plugin, eventBus, config);
    module.onEnable();

    boolean blocked = module.checkPacketSize(1024 * 1024 * 10);
    assertThat(blocked).isTrue();

    verify(eventBus, atLeastOnce()).publish(any(StarxEvent.class));
  }

  @Test
  void shouldAllowNormalSizedPacket() {
    CrashFixModule module = new CrashFixModule(plugin, eventBus, config);
    module.onEnable();

    boolean blocked = module.checkPacketSize(1024);
    assertThat(blocked).isFalse();
  }

  @Test
  void shouldDetectNbtOverflow() {
    CrashFixModule module = new CrashFixModule(plugin, eventBus, config);
    module.onEnable();

    boolean overflow = module.checkNbtDepth(256);
    assertThat(overflow).isTrue();
  }

  @Test
  void shouldAllowNormalNbtDepth() {
    CrashFixModule module = new CrashFixModule(plugin, eventBus, config);
    module.onEnable();

    boolean overflow = module.checkNbtDepth(10);
    assertThat(overflow).isFalse();
  }

  @Test
  void shouldDetectNegativeArraySize() {
    CrashFixModule module = new CrashFixModule(plugin, eventBus, config);
    module.onEnable();

    boolean suspicious = module.checkArraySize(-1);
    assertThat(suspicious).isTrue();
  }

  @Test
  void shouldAllowValidArraySize() {
    CrashFixModule module = new CrashFixModule(plugin, eventBus, config);
    module.onEnable();

    boolean suspicious = module.checkArraySize(100);
    assertThat(suspicious).isFalse();
  }
}
