package io.github.addxiaoyi.starx.velocity.module.proxytools;

import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/** 文件清理模块：基于年龄、数量、大小的文件自动清理，参考 SilverstoneMC/FileCleaner。 */
public final class FileCleanerModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;
  private final File basePath;
  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final AtomicInteger filesDeleted = new AtomicInteger(0);
  private double mbSaved;

  public FileCleanerModule(StarxVelocityPlugin plugin, Config config) {
    this(plugin, config, new File(".").getAbsoluteFile());
  }

  FileCleanerModule(StarxVelocityPlugin plugin, Config config, File basePath) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
    this.basePath = Objects.requireNonNull(basePath, "basePath");
  }

  @Override
  public String name() {
    return "proxytools.filecleaner";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) {
      return;
    }
    initialized.set(true);
    // TODO: 注册 Cron 定时任务 scheduler
    // TODO: 注册 /starx filecleaner run 命令
    // TODO: 注册 /starx filecleaner status 命令
  }

  @Override
  public void onDisable() {
    initialized.set(false);
    // TODO: 取消 Cron 定时任务
  }

  public boolean isInitialized() {
    return initialized.get();
  }

  public int getFilesDeleted() {
    return filesDeleted.get();
  }

  public double getMbSaved() {
    return mbSaved;
  }

  public void runCleanup() {
    filesDeleted.set(0);
    mbSaved = 0;

    for (FolderConfig folderCfg : config.folders()) {
      scanFilesInDir(folderCfg);
    }
    for (FileConfig fileCfg : config.files()) {
      scanFile(fileCfg);
    }
  }

  private void scanFilesInDir(FolderConfig folderCfg) {
    String loc = folderCfg.location();
    File folder = new File(loc);
    if (!folder.isAbsolute()) {
      folder = new File(basePath, loc);
    }
    File[] files = folder.listFiles();
    if (files == null) {
      plugin.logger().log(Level.SEVERE, "Could not find folder: " + folder.getPath());
      return;
    }
    List<File> fileList = new ArrayList<>(Arrays.asList(files));
    if (fileList.isEmpty()) {
      return;
    }

    List<String> excluded = folderCfg.exclude();
    if (excluded != null && !excluded.isEmpty()) {
      filterExcludedFiles(fileList, excluded);
    }

    int age = folderCfg.age();
    if (age > -1) {
      deleteByAge(fileList, age);
    }

    int count = folderCfg.count();
    if (count > -1) {
      deleteByCount(fileList, count);
    }

    long size = folderCfg.size();
    if (size > -1) {
      deleteBySize(fileList, size);
    }
  }

  private void filterExcludedFiles(List<File> fileList, List<String> excludedRules) {
    List<File> toRemove = new ArrayList<>();
    Map<String, Pattern> compiledPatterns = new HashMap<>();
    Set<String> invalidPatterns = new HashSet<>();

    for (File file : fileList) {
      for (String rule : excludedRules) {
        if (rule.equals(file.getName())) {
          toRemove.add(file);
          break;
        }
        Pattern pattern = compiledPatterns.get(rule);
        if (pattern == null && !invalidPatterns.contains(rule)) {
          try {
            pattern = Pattern.compile(rule);
            compiledPatterns.put(rule, pattern);
          } catch (PatternSyntaxException e) {
            invalidPatterns.add(rule);
            plugin.logger().log(Level.SEVERE, "Invalid regex: " + rule);
          }
        }
        if (pattern != null && pattern.matcher(file.getName()).matches()) {
          toRemove.add(file);
          break;
        }
      }
    }
    fileList.removeAll(toRemove);
  }

  private void deleteByAge(List<File> fileList, int age) {
    List<File> toRemove = new ArrayList<>();
    long cutoff = age * 24L * 60L * 60L * 1000L;
    long now = new Date().getTime();

    for (File file : fileList) {
      if (now - file.lastModified() > cutoff) {
        deleteFile(file);
        toRemove.add(file);
      }
    }
    fileList.removeAll(toRemove);
  }

  private void deleteByCount(List<File> fileList, int count) {
    List<File> toRemove = new ArrayList<>();
    fileList.sort(Comparator.comparingLong(File::lastModified));
    for (int i = 0; i < fileList.size() - count; i++) {
      deleteFile(fileList.get(i));
      toRemove.add(fileList.get(i));
    }
    fileList.removeAll(toRemove);
  }

  private void deleteBySize(List<File> fileList, long sizeLimit) {
    fileList.sort(Comparator.comparingLong(File::length));
    for (File file : fileList) {
      if (Math.round(file.length() / 1024.0) > (double) sizeLimit) {
        deleteFile(file);
      }
    }
  }

  private void scanFile(FileConfig fileCfg) {
    String loc = fileCfg.location();
    File file = new File(loc);
    if (!file.isAbsolute()) {
      file = new File(basePath, loc);
    }
    if (!file.exists()) {
      return;
    }
    int age = fileCfg.age();
    if (age > -1) {
      long cutoff = age * 24L * 60L * 60L * 1000L;
      if (new Date().getTime() - file.lastModified() > cutoff) {
        deleteFile(file);
        return;
      }
    }
    long size = fileCfg.size();
    if (size > -1 && Math.round(file.length() / 1024.0) > (double) size) {
      deleteFile(file);
    }
  }

  private void deleteFile(File file) {
    if (!file.isFile()) {
      return;
    }
    long fileSize = file.length();
    if (file.delete()) {
      filesDeleted.incrementAndGet();
      mbSaved += Math.round((fileSize / (1024.0 * 1024.0)) * 100.0) / 100.0;
      plugin.logger().info("Deleted file: " + file.getPath());
    } else {
      plugin.logger().log(Level.SEVERE, "Could not delete file: " + file.getPath());
    }
  }

  public interface Config {
    boolean enabled();

    String schedule();

    List<FolderConfig> folders();

    List<FileConfig> files();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public String schedule() {
          return "0 0 * * *";
        }

        @Override
        public List<FolderConfig> folders() {
          return List.of();
        }

        @Override
        public List<FileConfig> files() {
          return List.of();
        }
      };
    }
  }

  public interface FolderConfig {
    String location();

    int age();

    int count();

    long size();

    List<String> exclude();
  }

  public interface FileConfig {
    String location();

    int age();

    long size();
  }
}