package io.github.addxiaoyi.starx.velocity.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModuleManagerTest {

  @Mock VelocityModule enabledModule;
  @Mock VelocityModule disabledModule;

  @Test
  void shouldEnableOnlyConfiguredModules() {
    StarxConfig config = configWithModules(Map.of("enabled", true, "disabled", false));
    ModuleManager manager = new ModuleManager(config);

    org.mockito.Mockito.when(enabledModule.name()).thenReturn("enabled");
    org.mockito.Mockito.when(disabledModule.name()).thenReturn("disabled");

    manager.register(enabledModule);
    manager.register(disabledModule);
    manager.enableAll();

    verify(enabledModule).onEnable();
    verify(disabledModule, never()).onEnable();
  }

  @Test
  void shouldDisableAllRegisteredModules() {
    StarxConfig config = configWithModules(Map.of("a", true));
    ModuleManager manager = new ModuleManager(config);

    org.mockito.Mockito.when(enabledModule.name()).thenReturn("a");

    manager.register(enabledModule);
    manager.disableAll();

    verify(enabledModule).onDisable();
  }

  @Test
  void shouldReturnRegisteredModuleByName() {
    StarxConfig config = configWithModules(Map.of("test", true));
    ModuleManager manager = new ModuleManager(config);

    org.mockito.Mockito.when(enabledModule.name()).thenReturn("test");

    manager.register(enabledModule);

    assertThat(manager.get("test")).isPresent().hasValue(enabledModule);
    assertThat(manager.get("missing")).isEmpty();
    assertThat(manager.all()).containsExactly(enabledModule);
  }

  private StarxConfig configWithModules(Map<String, Boolean> modules) {
    Map<String, StarxConfig.ModuleConfig> moduleConfigs = new HashMap<>();
    modules.forEach((k, v) -> moduleConfigs.put(k, new StarxConfig.ModuleConfig(v)));
    return new StarxConfig(
        "test-key",
        new StarxConfig.HttpConfig("127.0.0.1", 8788),
        new StarxConfig.WebhookConfig("", ""),
        moduleConfigs);
  }
}
