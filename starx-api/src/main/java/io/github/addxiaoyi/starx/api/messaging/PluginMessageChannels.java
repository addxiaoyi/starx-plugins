package io.github.addxiaoyi.starx.api.messaging;

/** Velocity 与 Paper 之间 Plugin Messaging 通道常量。 */
public final class PluginMessageChannels {

  private PluginMessageChannels() {}

  /** 主通道，所有 StarX 内部消息均通过此通道发送。 */
  public static final String MAIN = "starx:main";

  // 子命令类型（消息头第一个字节/字符串）
  public static final String CMD_SKIN_SYNC = "skin_sync";
  public static final String CMD_PLAYER_STATE = "player_state";
  public static final String CMD_CONFIG_SYNC = "config_sync";
  public static final String CMD_SECURITY_ALERT = "security_alert";
}
