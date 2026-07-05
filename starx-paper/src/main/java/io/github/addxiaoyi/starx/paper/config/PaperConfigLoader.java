package io.github.addxiaoyi.starx.paper.config;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/** 加载 plugins/starx/config.yml 并返回 starx-common 兼容的 {@link ConfigurationNode}。 */
public final class PaperConfigLoader {

  private static final String DEFAULT_CONFIG =
      "# StarX Paper 配置文件\n"
          + "# 首次启动自动生成，修改后重启生效\n"
          + "modules:\n"
          + "  # 维护模式：禁止非管理员玩家加入\n"
          + "  maintenance:\n"
          + "    enabled: false\n"
          + "  # 聊天格式控制\n"
          + "  chat:\n"
          + "    enabled: true\n"
          + "    format: '<{player}> {message}'\n"
          + "  # 皮肤刷新\n"
          + "  skin:\n"
          + "    enabled: true\n"
          + "  # 反作弊检测（speed/break/interact）\n"
          + "  anticheat:\n"
          + "    enabled: false\n"
          + "    enabled_checks:\n"
          + "      - speed\n"
          + "      - break\n"
          + "      - interact\n"
          + "    vl_threshold: 10\n"
          + "  # 崩溃修复\n"
          + "  crashfix:\n"
          + "    enabled: true\n"
          + "  # 网络优化\n"
          + "  networking:\n"
          + "    enabled: true\n"
          + "  # 地图模组兼容\n"
          + "  mapmod:\n"
          + "    enabled: false\n"
          + "  # QQ 机器人集成\n"
          + "  qq:\n"
          + "    enabled: false\n"
          + "  # Plan 统计集成\n"
          + "  plan:\n"
          + "    enabled: false\n"
          + "  # 日志/临时文件自动清理\n"
          + "  filecleaner:\n"
          + "    enabled: false\n"
          + "    schedule: '0 0 * * *'\n"
          + "    folders:\n"
          + "      logs:\n"
          + "        location: '/logs'\n"
          + "        age: 7\n"
          + "        count: -1\n"
          + "        size: -1\n"
          + "        exclude:\n"
          + "          - latest.log\n"
          + "    files:\n"
          + "      serverlog:\n"
          + "        location: '/logs/latest.log'\n"
          + "        age: 30\n"
          + "        size: -1\n";

  private final StarxPaperPlugin plugin;
  private ConfigurationNode root;

  public PaperConfigLoader(StarxPaperPlugin plugin) {
    this.plugin = plugin;
  }

  public void load() throws ConfigurateException {
    File configDir = new File(plugin.getDataFolder().getParentFile(), "starx");
    File configFile = new File(configDir, "config.yml");
    if (!configFile.exists()) {
      configDir.mkdirs();
      saveDefault(configFile);
    }
    YamlConfigurationLoader loader =
        YamlConfigurationLoader.builder().path(configFile.toPath()).build();
    root = loader.load();
  }

  public ConfigurationNode root() {
    return root;
  }

  public boolean isModuleEnabled(String module) {
    if (root == null) {
      return false;
    }
    return root.node("modules", module, "enabled").getBoolean(true);
  }

  public String getChatFormat() {
    if (root == null) {
      return "<{player}> {message}";
    }
    return root.node("modules", "chat", "format").getString("<{player}> {message}");
  }

  @SuppressWarnings("unchecked")
  public List<String> getEnabledChecks() {
    if (root == null) {
      return List.of("speed", "break", "interact");
    }
    try {
      return root.node("modules", "anticheat", "enabled_checks").getList(String.class);
    } catch (Exception e) {
      return List.of("speed", "break", "interact");
    }
  }

  private void saveDefault(File configFile) throws ConfigurateException {
    try {
      Files.writeString(configFile.toPath(), DEFAULT_CONFIG, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new ConfigurateException("Failed to write default config to " + configFile, e);
    }
  }
}
