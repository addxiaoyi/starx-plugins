package net.elytrium.limboapi.api.event;

import com.velocitypowered.api.proxy.Player;

/** LimboAPI 事件桩，仅用于编译期类型检查；运行时由 LimboAPI 真实类替换。 */
public final class LoginLimboRegisterEvent {

  private final Player player;

  public LoginLimboRegisterEvent(Player player) {
    this.player = player;
  }

  public Player getPlayer() {
    return player;
  }
}
