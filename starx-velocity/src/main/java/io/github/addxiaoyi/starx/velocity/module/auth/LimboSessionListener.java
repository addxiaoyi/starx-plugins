package io.github.addxiaoyi.starx.velocity.module.auth;

import com.velocitypowered.api.proxy.Player;
import io.github.addxiaoyi.starx.common.auth.AuthCommandHandler;
import io.github.addxiaoyi.starx.common.auth.AuthResult;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import java.net.InetAddress;
import java.util.function.BiConsumer;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** Limbo 内事件/命令适配器。玩家直接在聊天框输入密码即可登录/注册。 */
public final class LimboSessionListener implements LimboSessionHandler {

  private final Player player;
  private final AuthService authService;
  private final AuthCommandHandler commandHandler;
  private final BiConsumer<Player, AuthResult> resultConsumer;
  private final String deviceId;

  public LimboSessionListener(
      Player player,
      AuthService authService,
      AuthCommandHandler commandHandler,
      BiConsumer<Player, AuthResult> resultConsumer,
      String deviceId) {
    this.player = player;
    this.authService = authService;
    this.commandHandler = commandHandler;
    this.resultConsumer = resultConsumer;
    this.deviceId = deviceId;
  }

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    if (authService.isUserRegistered(this.player.getUniqueId())) {
      this.player.sendMessage(
          Component.text("请输入密码登录", NamedTextColor.YELLOW));
    } else {
      this.player.sendMessage(
          Component.text("欢迎来到 StarMC！", NamedTextColor.GREEN)
              .append(Component.newline())
              .append(Component.text("请在聊天框输入密码完成注册", NamedTextColor.WHITE)));
    }
  }

  @Override
  public void onChat(String chat) {
    InetAddress address =
        player.getRemoteAddress() != null ? player.getRemoteAddress().getAddress() : null;
    AuthResult result =
        commandHandler.handle(player.getUniqueId(), player.getUsername(), chat, address, deviceId);
    resultConsumer.accept(player, result);
  }

  @Override
  public void onDisconnect() {
    // 玩家断开连接时清理会话（无需额外操作，会话会超时）
  }
}