package io.github.addxiaoyi.starx.paper.module.filecleaner;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

/** Paper 端文件清理模块：监听 Paper 特定文件（world 存档、日志等）。 */
public final class FileCleanerModule implements PaperModule, Listener {

  private static final int DEFAULT_AGE_DAYS = 7;

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private boolean enabled;

  public FileCleanerModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  @Override
  public String getName() {
    return "filecleaner";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    enabled = configLoader.isModuleEnabled("filecleaner");
    plugin.getLogger().info("FileCleaner module enabled: " + enabled);
    // TODO: 从配置读取文件夹和文件清理规则
    // TODO: 注册定时任务
  }

  @EventHandler
  public void onServerLoad(ServerLoadEvent event) {
    if (!enabled) {
      return;
    }
    runCleanup();
  }

  public void runCleanup() {
    if (!enabled) {
      return;
    }
    plugin.getLogger().info("Starting file cleanup...");
    int deleted = 0;

    deleted += cleanLogFiles();
    // TODO: 清理 world 存档备份
    // TODO: 清理 crash-reports
    // TODO: 清理插件缓存目录

    plugin.getLogger().info("File cleanup complete. Deleted " + deleted + " files.");
  }

  private int cleanLogFiles() {
    int deleted = 0;
    File logsDir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "logs");
    if (!logsDir.exists() || !logsDir.isDirectory()) {
      return deleted;
    }
    File[] files = logsDir.listFiles();
    if (files == null) {
      return deleted;
    }

    long cutoff = DEFAULT_AGE_DAYS * 24L * 60L * 60L * 1000L;
    long now = new Date().getTime();
    List<File> sortedFiles = new ArrayList<>(Arrays.asList(files));
    sortedFiles.sort(Comparator.comparingLong(File::lastModified));

    // TODO: 从配置读取 age/keep 参数
    for (File file : sortedFiles) {
      if (file.isFile() && (now - file.lastModified() > cutoff)) {
        if (file.delete()) {
          deleted++;
          plugin.getLogger().info("Deleted log: " + file.getName());
        } else {
          plugin.getLogger().log(Level.WARNING, "Failed to delete: " + file.getName());
        }
      }
    }
    return deleted;
  }
}
