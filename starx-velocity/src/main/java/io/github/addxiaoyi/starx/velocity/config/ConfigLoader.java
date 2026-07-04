package io.github.addxiaoyi.starx.velocity.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/** 基于 Configurate-YAML 的配置加载器。 */
public final class ConfigLoader {

  private static final String DEFAULT_CONFIG =
      """
      # StarX Velocity 配置文件
      api-key: ""

      http:
        bind: "127.0.0.1"
        port: 8788

      webhook:
        url: ""
        secret: ""

      modules:
        auth:
          enabled: true
        skin-bridge:
          enabled: true
        messaging:
          enabled: true
      """;

  private ConfigLoader() {}

  public static StarxConfig load(Path path) throws IOException {
    if (!Files.exists(path)) {
      Files.createDirectories(path.getParent());
      Files.writeString(path, DEFAULT_CONFIG, StandardCharsets.UTF_8);
    }

    YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
    ConfigurationNode root = loader.load();

    String apiKey = root.node("api-key").getString("");

    ConfigurationNode httpNode = root.node("http");
    StarxConfig.HttpConfig http =
        new StarxConfig.HttpConfig(
            httpNode.node("bind").getString("127.0.0.1"), httpNode.node("port").getInt(8788));

    ConfigurationNode webhookNode = root.node("webhook");
    StarxConfig.WebhookConfig webhook =
        new StarxConfig.WebhookConfig(
            webhookNode.node("url").getString(""), webhookNode.node("secret").getString(""));

    Map<String, StarxConfig.ModuleConfig> modules = new HashMap<>();
    ConfigurationNode modulesNode = root.node("modules");
    if (!modulesNode.virtual() && modulesNode.isMap()) {
      for (Map.Entry<Object, ? extends ConfigurationNode> entry :
          modulesNode.childrenMap().entrySet()) {
        String name = entry.getKey().toString();
        boolean enabled = entry.getValue().node("enabled").getBoolean(false);
        modules.put(name, new StarxConfig.ModuleConfig(enabled));
      }
    }

    return new StarxConfig(apiKey, http, webhook, modules);
  }
}
