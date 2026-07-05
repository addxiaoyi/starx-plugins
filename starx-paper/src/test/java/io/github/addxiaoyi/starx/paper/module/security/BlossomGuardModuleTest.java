package io.github.addxiaoyi.starx.paper.module.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import io.github.addxiaoyi.starx.common.security.BushClient;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlossomGuardModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock BushClient bushClient;

  Logger logger = Logger.getLogger(BlossomGuardModuleTest.class.getName());

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getLogger()).thenReturn(logger);
    lenient().when(configLoader.isModuleEnabled("security.blossom")).thenReturn(true);
  }

  @Test
  void shouldReturnCorrectName() {
    BlossomGuardModule module = new BlossomGuardModule(plugin, configLoader, bushClient);
    assertThat(module.getName()).isEqualTo("security.blossom");
  }

  @Test
  void shouldReturnEnabledStatus() {
    BlossomGuardModule module = new BlossomGuardModule(plugin, configLoader, bushClient);
    assertThat(module.isEnabled()).isTrue();
  }

  @Test
  void shouldReturnDisabledWhenConfigDisabled() {
    lenient().when(configLoader.isModuleEnabled("security.blossom")).thenReturn(false);
    BlossomGuardModule module = new BlossomGuardModule(plugin, configLoader, bushClient);
    assertThat(module.isEnabled()).isFalse();
  }

  @Test
  void shouldConstructWithDefaultBushClient() {
    BlossomGuardModule module = new BlossomGuardModule(plugin, configLoader);
    assertThat(module.getBushClient()).isNotNull();
  }

  @Test
  void shouldExposeBushClient() {
    BlossomGuardModule module = new BlossomGuardModule(plugin, configLoader, bushClient);
    assertThat(module.getBushClient()).isSameAs(bushClient);
  }
}
