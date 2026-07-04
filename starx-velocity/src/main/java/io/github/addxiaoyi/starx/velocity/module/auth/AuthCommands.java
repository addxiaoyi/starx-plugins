package io.github.addxiaoyi.starx.velocity.module.auth;

import io.github.addxiaoyi.starx.common.auth.AuthCommandHandler;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;

/** Velocity 命令注册占位类（实际注册在 AuthModule 完成）。 */
public final class AuthCommands {

  private final StarxVelocityPlugin plugin;
  private final AuthCommandHandler commandHandler;

  public AuthCommands(StarxVelocityPlugin plugin, AuthCommandHandler commandHandler) {
    this.plugin = plugin;
    this.commandHandler = commandHandler;
  }

  /** Velocity 原生命令注册占位方法。 */
  public void register() {
    // 认证命令在 Limbo 内通过 LimboSessionListener 处理，Velocity 层不注册额外命令。
  }
}
