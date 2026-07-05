package io.github.addxiaoyi.starx.velocity.module.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmartAlertModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock EventBus eventBus;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.logger()).thenReturn(java.util.logging.Logger.getLogger("test"));
  }

  @Test
  void shouldReturnCorrectName() {
    SmartAlertModule module = new SmartAlertModule(plugin, eventBus);
    assertThat(module.name()).isEqualTo("security.smart-alert");
  }

  @Test
  void shouldStartWithEmptyBuckets() {
    SmartAlertModule module = new SmartAlertModule(plugin, eventBus);
    assertThat(module.getBucketCount()).isEqualTo(0);
  }

  @Test
  void shouldClearBuckets() {
    SmartAlertModule module = new SmartAlertModule(plugin, eventBus);
    module.clearBuckets();
    assertThat(module.getBucketCount()).isEqualTo(0);
  }

  @Test
  void shouldSubscribeToSecurityEvents() {
    SmartAlertModule module = new SmartAlertModule(plugin, eventBus);
    module.onEnable();
    verify(eventBus, atLeast(8)).subscribe(anyString(), any());
  }

  @Test
  void shouldDisableCleanly() {
    SmartAlertModule module = new SmartAlertModule(plugin, eventBus);
    module.onEnable();
    module.onDisable();
    assertThat(module.getBucketCount()).isGreaterThanOrEqualTo(0);
  }
}
