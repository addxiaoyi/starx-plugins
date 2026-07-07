package io.github.addxiaoyi.starx.velocity.module.auth;

import com.velocitypowered.api.command.SimpleCommand;
import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthClient;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** 迁移管理命令。 */
public final class MigrationCommands implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final JdbcUserRepository userRepository;
  private final MigrationModule migrationModule;
  private final UniAuthClient uniAuthClient;

  public MigrationCommands(
      StarxVelocityPlugin plugin,
      JdbcUserRepository userRepository,
      MigrationModule migrationModule,
      UniAuthClient uniAuthClient) {
    this.plugin = plugin;
    this.userRepository = userRepository;
    this.migrationModule = migrationModule;
    this.uniAuthClient = uniAuthClient;
  }

  @Override
  public String name() {
    return "starx.auth.migration.commands";
  }

  @Override
  public void onEnable() {
    plugin
        .proxy()
        .getCommandManager()
        .register(
            plugin.proxy().getCommandManager().metaBuilder("authx").build(), new AuthxCommand());
  }

  private final class AuthxCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
      String[] args = invocation.arguments();
      if (args.length == 0) {
        showHelp(invocation);
        return;
      }

      switch (args[0].toLowerCase()) {
        case "migrate":
          handleMigrate(invocation, args);
          break;
        case "migrate-status":
          handleMigrateStatus(invocation);
          break;
        default:
          showHelp(invocation);
      }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
      // 只有控制台或有管理权限的用户可以执行
      return true; // 实际项目中应该检查权限
    }

    private void showHelp(Invocation invocation) {
      invocation
          .source()
          .sendMessage(Component.text("===== StarX 迁移管理 =====", NamedTextColor.GOLD));
      invocation
          .source()
          .sendMessage(
              Component.text("/authx migrate-status", NamedTextColor.YELLOW)
                  .append(Component.text(" - 查看迁移状态统计", NamedTextColor.GRAY)));
      invocation
          .source()
          .sendMessage(
              Component.text("/authx migrate starvc import-meta [--dry-run]", NamedTextColor.YELLOW)
                  .append(Component.text(" - 从 StarVC 导入用户元数据", NamedTextColor.GRAY)));
      invocation
          .source()
          .sendMessage(Component.text("  --dry-run - 试运行，不实际修改数据", NamedTextColor.GRAY));
    }

    private void handleMigrateStatus(Invocation invocation) {
      if (userRepository == null) {
        invocation.source().sendMessage(Component.text("用户仓库不可用", NamedTextColor.RED));
        return;
      }

      try {
        int total = userRepository.countAll();
        int starvcUsers = userRepository.countBySourceSystem("starvc");
        int starvcPending =
            userRepository.countBySourceSystemAndMigrationState("starvc", "pending");
        int starvcCompleted =
            userRepository.countBySourceSystemAndMigrationState("starvc", "completed");

        invocation.source().sendMessage(Component.text("===== 迁移状态 =====", NamedTextColor.GOLD));
        invocation
            .source()
            .sendMessage(
                Component.text("总用户数: ", NamedTextColor.WHITE)
                    .append(Component.text(total, NamedTextColor.AQUA)));
        invocation
            .source()
            .sendMessage(
                Component.text("StarVC 用户: ", NamedTextColor.WHITE)
                    .append(Component.text(starvcUsers, NamedTextColor.AQUA)));
        invocation
            .source()
            .sendMessage(
                Component.text("StarVC 待迁移: ", NamedTextColor.WHITE)
                    .append(Component.text(starvcPending, NamedTextColor.YELLOW)));
        invocation
            .source()
            .sendMessage(
                Component.text("StarVC 已迁移: ", NamedTextColor.WHITE)
                    .append(Component.text(starvcCompleted, NamedTextColor.GREEN)));

        if (starvcUsers > 0) {
          double progress = ((double) starvcCompleted / starvcUsers) * 100;
          invocation
              .source()
              .sendMessage(
                  Component.text("StarVC 迁移进度: ", NamedTextColor.WHITE)
                      .append(
                          Component.text(String.format("%.1f%%", progress), NamedTextColor.GREEN)));
        }
      } catch (Exception e) {
        invocation
            .source()
            .sendMessage(Component.text("获取迁移状态失败: " + e.getMessage(), NamedTextColor.RED));
        plugin.logger().log(java.util.logging.Level.SEVERE, "获取迁移状态失败", e);
      }
    }

    private void handleMigrate(Invocation invocation, String[] args) {
      if (args.length < 2) {
        showMigrateHelp(invocation);
        return;
      }

      String subCommand = args[1].toLowerCase();
      if ("starvc".equals(subCommand) && args.length >= 3 && "import-meta".equals(args[2])) {
        boolean dryRun = false;
        for (int i = 3; i < args.length; i++) {
          if ("--dry-run".equals(args[i])) {
            dryRun = true;
            break;
          }
        }
        handleStarVCImportMeta(invocation, dryRun);
      } else {
        showMigrateHelp(invocation);
      }
    }

    private void showMigrateHelp(Invocation invocation) {
      invocation.source().sendMessage(Component.text("===== 迁移命令 =====", NamedTextColor.GOLD));
      invocation
          .source()
          .sendMessage(
              Component.text(
                  "/authx migrate starvc import-meta [--dry-run]", NamedTextColor.YELLOW));
    }

    private void handleStarVCImportMeta(Invocation invocation, boolean dryRun) {
      if (MigrationModule.isRunning()) {
        invocation.source().sendMessage(Component.text("迁移正在进行中，请稍后再试", NamedTextColor.RED));
        return;
      }

      if (migrationModule == null) {
        invocation.source().sendMessage(Component.text("迁移模块不可用", NamedTextColor.RED));
        return;
      }

      if (dryRun) {
        invocation
            .source()
            .sendMessage(Component.text("开始 StarVC 用户元数据导入（试运行）...", NamedTextColor.YELLOW));
      } else {
        invocation
            .source()
            .sendMessage(Component.text("开始 StarVC 用户元数据导入...", NamedTextColor.YELLOW));
      }

      plugin
          .proxy()
          .getScheduler()
          .buildTask(
              plugin,
              () -> {
                try {
                  MigrationModule.MigrationResult result = migrationModule.importStarVCMeta(dryRun);
                  showMigrationResult(invocation, result);
                } catch (Exception e) {
                  invocation
                      .source()
                      .sendMessage(Component.text("导入失败: " + e.getMessage(), NamedTextColor.RED));
                  plugin.logger().log(java.util.logging.Level.SEVERE, "导入失败", e);
                }
              })
          .schedule();
    }

    private void showMigrationResult(
        Invocation invocation, MigrationModule.MigrationResult result) {
      invocation.source().sendMessage(Component.text("===== 迁移完成 =====", NamedTextColor.GOLD));
      if (result.dryRun()) {
        invocation.source().sendMessage(Component.text("(试运行模式，无实际修改)", NamedTextColor.GRAY));
      }
      invocation
          .source()
          .sendMessage(
              Component.text("总计: ", NamedTextColor.WHITE)
                  .append(Component.text(result.total(), NamedTextColor.AQUA)));
      invocation
          .source()
          .sendMessage(
              Component.text("导入: ", NamedTextColor.WHITE)
                  .append(Component.text(result.imported(), NamedTextColor.GREEN)));
      invocation
          .source()
          .sendMessage(
              Component.text("跳过(已存在): ", NamedTextColor.WHITE)
                  .append(Component.text(result.skippedExisting(), NamedTextColor.GRAY)));
      invocation
          .source()
          .sendMessage(
              Component.text("跳过(无效): ", NamedTextColor.WHITE)
                  .append(Component.text(result.skippedInvalid(), NamedTextColor.YELLOW)));
      invocation
          .source()
          .sendMessage(
              Component.text("错误: ", NamedTextColor.WHITE)
                  .append(Component.text(result.errors(), NamedTextColor.RED)));
      invocation
          .source()
          .sendMessage(
              Component.text("耗时: ", NamedTextColor.WHITE)
                  .append(Component.text(result.durationMs() + "ms", NamedTextColor.AQUA)));

      // 计算成功率
      if (result.total() > 0) {
        double successRate =
            ((double) (result.imported() + result.skippedExisting())) / result.total() * 100;
        invocation
            .source()
            .sendMessage(
                Component.text("成功率: ", NamedTextColor.WHITE)
                    .append(
                        Component.text(
                            String.format("%.1f%%", successRate), NamedTextColor.GREEN)));
      }
    }
  }
}
