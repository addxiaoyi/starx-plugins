package io.github.addxiaoyi.starx.paper.config;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

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
  private Map<String, Object> root;

  public PaperConfigLoader(StarxPaperPlugin plugin) {
    this.plugin = plugin;
  }

  @SuppressWarnings("unchecked")
  public void load() {
    File configDir = new File(plugin.getDataFolder().getParentFile(), "starx");
    File configFile = new File(configDir, "config.yml");
    if (!configFile.exists()) {
      configDir.mkdirs();
      saveDefault(configFile);
    }
    try (InputStream in = Files.newInputStream(configFile.toPath())) {
      root = new Yaml().load(in);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load config from " + configFile, e);
    }
  }

  public boolean isModuleEnabled(String module) {
    return bool(nested("modules", module), "enabled", true);
  }

  public String getChatFormat() {
    return str(nested("modules", "chat"), "format", "<{player}> {message}");
  }

  @SuppressWarnings("unchecked")
  public List<String> getEnabledChecks() {
    Map<String, Object> anticheat = nested("modules", "anticheat");
    if (anticheat.containsKey("enabled_checks")) {
      Object checks = anticheat.get("enabled_checks");
      if (checks instanceof List) {
        List<String> result = new ArrayList<>();
        for (Object item : (List<Object>) checks) {
          result.add(item.toString());
        }
        return result;
      }
    }
    return List.of("speed", "break", "interact");
  }

  private void saveDefault(File configFile) {
    try {
      Files.writeString(configFile.toPath(), DEFAULT_CONFIG, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write default config to " + configFile, e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> nested(String... keys) {
    if (root == null) return Map.of();
    Map<String, Object> current = root;
    for (String key : keys) {
      Object v = current.get(key);
      if (!(v instanceof Map)) return Map.of();
      current = (Map<String, Object>) v;
    }
    return current;
  }

  private static String str(Map<String, Object> map, String key, String def) {
    Object v = map.get(key);
    return v != null ? v.toString() : def;
  }

  private static boolean bool(Map<String, Object> map, String key, boolean def) {
    Object v = map.get(key);
    return v instanceof Boolean ? (Boolean) v : def;
  }
}
