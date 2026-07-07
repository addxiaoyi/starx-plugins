package io.github.addxiaoyi.starx.velocity.module.auth;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import io.github.addxiaoyi.starx.common.auth.AuthResult;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

/** 注册 Velocity 层认证管理命令（/2fa）。 */
public final class AuthCommands implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final AuthService authService;

  public AuthCommands(StarxVelocityPlugin plugin, AuthService authService) {
    this.plugin = plugin;
    this.authService = authService;
  }

  @Override
  public String name() {
    return "starx.auth-commands";
  }

  @Override
  public void onEnable() {
    registerCommands();
  }

  private void registerCommands() {
    plugin
        .proxy()
        .getCommandManager()
        .register(
            plugin.proxy().getCommandManager().metaBuilder("2fa").build(), new TwoFactorCommand());
  }

  private final class TwoFactorCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (!(invocation.source() instanceof Player player)) {
        invocation.source().sendMessage(Component.text("此命令仅限玩家使用", NamedTextColor.RED));
        return;
      }
      String[] args = invocation.arguments();
      if (args.length == 0) {
        showUsage(invocation);
        return;
      }
      switch (args[0].toLowerCase()) {
        case "enable" -> {
          if (args.length < 2) {
            invocation
                .source()
                .sendMessage(Component.text("用法: /2fa enable <密码>", NamedTextColor.RED));
            return;
          }
          AuthResult result = authService.enableTotp(player.getUniqueId(), args[1]);
          if (result.success()) {
            invocation.source().sendMessage(Component.text("§a二步验证已开启！", NamedTextColor.GREEN));
            invocation
                .source()
                .sendMessage(
                    Component.text(
                        "请使用 Google Authenticator 或同类 App 扫描下方密钥：", NamedTextColor.YELLOW));
            invocation
                .source()
                .sendMessage(
                    Component.text(
                            result.message().substring(result.message().indexOf("密钥:")),
                            NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.copyToClipboard(extractSecret(result.message()))));
            invocation
                .source()
                .sendMessage(Component.text("§c请务必保存此密钥，丢失后无法找回！", NamedTextColor.RED));
          } else {
            invocation
                .source()
                .sendMessage(Component.text("§c" + result.message(), NamedTextColor.RED));
          }
        }
        case "disable" -> {
          if (args.length < 2) {
            invocation
                .source()
                .sendMessage(Component.text("用法: /2fa disable <密码>", NamedTextColor.RED));
            return;
          }
          AuthResult result = authService.disableTotp(player.getUniqueId(), args[1]);
          if (result.success()) {
            invocation.source().sendMessage(Component.text("§a二步验证已关闭", NamedTextColor.GREEN));
          } else {
            invocation
                .source()
                .sendMessage(Component.text("§c" + result.message(), NamedTextColor.RED));
          }
        }
        case "status" -> {
          boolean enabled = authService.isTotpEnabled(player.getUniqueId());
          String status =
              enabled
                  ? "§a二步验证：已开启§r（使用 /2fa disable <密码> 关闭）"
                  : "§e二步验证：未开启§r（使用 /2fa enable <密码> 开启，增强账户安全）";
          invocation.source().sendMessage(Component.text(status));
        }
        default -> showUsage(invocation);
      }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
      return true;
    }

    private void showUsage(Invocation invocation) {
      invocation
          .source()
          .sendMessage(Component.text("===== 二步验证 (2FA) =====", NamedTextColor.GOLD));
      invocation
          .source()
          .sendMessage(
              Component.text("/2fa status", NamedTextColor.YELLOW)
                  .append(Component.text(" - 查看二步验证状态", NamedTextColor.GRAY)));
      invocation
          .source()
          .sendMessage(
              Component.text("/2fa enable <密码>", NamedTextColor.YELLOW)
                  .append(Component.text(" - 开启二步验证", NamedTextColor.GRAY)));
      invocation
          .source()
          .sendMessage(
              Component.text("/2fa disable <密码>", NamedTextColor.YELLOW)
                  .append(Component.text(" - 关闭二步验证", NamedTextColor.GRAY)));
      invocation
          .source()
          .sendMessage(Component.text("§c开启后每次登录需要输入验证码，请妥善保存密钥", NamedTextColor.RED));
    }

    private String extractSecret(String message) {
      int start = message.indexOf("密钥: ") + 4;
      int end = message.indexOf(" | ", start);
      if (end < 0) {
        end = message.length();
      }
      return message.substring(start, end);
    }
  }
}
