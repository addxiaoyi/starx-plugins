package io.github.addxiaoyi.starx.velocity.module.auth;

import com.velocitypowered.api.proxy.Player;
import io.github.addxiaoyi.starx.common.auth.AuthCommandHandler;
import io.github.addxiaoyi.starx.common.auth.AuthResult;
import java.net.InetAddress;
import java.util.function.BiConsumer;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;

/** Limbo 内事件/命令适配器。 */
public final class LimboSessionListener implements LimboSessionHandler {

  private final Player player;
  private final AuthCommandHandler commandHandler;
  private final BiConsumer<Player, AuthResult> resultConsumer;
  private final String deviceId;

  public LimboSessionListener(
      Player player,
      AuthCommandHandler commandHandler,
      BiConsumer<Player, AuthResult> resultConsumer,
      String deviceId) {
    this.player = player;
    this.commandHandler = commandHandler;
    this.resultConsumer = resultConsumer;
    this.deviceId = deviceId;
  }

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    this.player.sendMessage(
        net.kyori.adventure.text.Component.text("请输入 /login <密码> 或 /register <密码> [邮箱]"));
  }

  @Override
  public void onChat(String chat) {
    String raw = chat.startsWith("/") ? chat.substring(1) : chat;
    InetAddress address =
        player.getRemoteAddress() != null ? player.getRemoteAddress().getAddress() : null;
    AuthResult result =
        commandHandler.handle(player.getUniqueId(), player.getUsername(), raw, address, deviceId);
    resultConsumer.accept(player, result);
  }

  @Override
  public void onDisconnect() {
    // 玩家断开连接时清理会话（无需额外操作，会话会超时）
  }
}
