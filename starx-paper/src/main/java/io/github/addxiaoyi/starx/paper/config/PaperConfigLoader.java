package io.github.addxiaoyi.starx.paper.config;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/** 加载 plugins/starx/config.yml 并返回 starx-common 兼容的 {@link ConfigurationNode}。 */
public final class PaperConfigLoader {

  private static final String DEFAULT_CONFIG =
      "modules:\n"
          + "  maintenance:\n"
          + "    enabled: false\n"
          + "  chat:\n"
          + "    enabled: true\n"
          + "    format: '<{player}> {message}'\n"
          + "  skin:\n"
          + "    enabled: true\n";

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

  private void saveDefault(File configFile) throws ConfigurateException {
    try {
      Files.writeString(configFile.toPath(), DEFAULT_CONFIG, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new ConfigurateException("Failed to write default config to " + configFile, e);
    }
  }
}
