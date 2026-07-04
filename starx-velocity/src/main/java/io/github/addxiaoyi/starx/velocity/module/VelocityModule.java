package io.github.addxiaoyi.starx.velocity.module;

/** Velocity 端功能模块契约。 */
public interface VelocityModule {

  /** 模块唯一标识，用于配置项 modules.{name}.enabled。 */
  String name();

  /** 模块启用时调用。 */
  default void onEnable() {}

  /** 模块禁用时调用。 */
  default void onDisable() {}
}
