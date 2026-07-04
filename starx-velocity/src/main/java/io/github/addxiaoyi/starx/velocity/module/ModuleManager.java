package io.github.addxiaoyi.starx.velocity.module;

import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** 负责模块的注册、按配置启用与禁用。 */
public final class ModuleManager {

  private final Map<String, VelocityModule> modules = new LinkedHashMap<>();
  private final StarxConfig config;

  public ModuleManager(StarxConfig config) {
    this.config = Objects.requireNonNull(config, "config");
  }

  /** 注册模块，若名称重复则覆盖。 */
  public void register(VelocityModule module) {
    Objects.requireNonNull(module, "module");
    modules.put(module.name(), module);
  }

  /** 启用所有配置中开启的模块。 */
  public void enableAll() {
    for (VelocityModule module : modules.values()) {
      if (config.isModuleEnabled(module.name())) {
        module.onEnable();
      }
    }
  }

  /** 禁用所有已注册模块。 */
  public void disableAll() {
    for (VelocityModule module : modules.values()) {
      module.onDisable();
    }
  }

  public Optional<VelocityModule> get(String name) {
    return Optional.ofNullable(modules.get(name));
  }

  public Collection<VelocityModule> all() {
    return modules.values();
  }
}
