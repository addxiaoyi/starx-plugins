package io.github.addxiaoyi.starx.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/** 基于 Configurate 读取 {@code config.yml} 并返回 {@link StarxConfig}。 */
public final class ConfigLoader {

  private ConfigLoader() {}

  public static StarxConfig load(Path path) throws IOException {
    if (!Files.isRegularFile(path)) {
      return StarxConfig.defaults();
    }

    YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
    ConfigurationNode root = loader.load();

    HttpApiConfig httpApi = loadHttpApi(root.node("http-api"));
    DatabaseConfig database = loadDatabase(root.node("database"));
    Map<String, ModuleConfig> modules = loadModules(root.node("modules"));

    return new StarxConfig(httpApi, database, modules);
  }

  private static HttpApiConfig loadHttpApi(ConfigurationNode node) {
    String bind = node.node("bind").getString(HttpApiConfig.defaults().bind());
    int port = node.node("port").getInt(HttpApiConfig.defaults().port());
    String apiKey = node.node("api-key").getString(HttpApiConfig.defaults().apiKey());
    return new HttpApiConfig(bind, port, apiKey);
  }

  private static DatabaseConfig loadDatabase(ConfigurationNode node) {
    DatabaseConfig defaults = DatabaseConfig.defaults();
    String type = node.node("type").getString(defaults.type());
    String host = node.node("host").getString(defaults.host());
    int port = node.node("port").getInt(defaults.port());
    String database = node.node("database").getString(defaults.database());
    String username = node.node("username").getString(defaults.username());
    String password = node.node("password").getString(defaults.password());
    String url = node.node("url").getString(defaults.url());
    int poolMaxSize = node.node("pool-max-size").getInt(defaults.poolMaxSize());
    long connectionTimeoutMs =
        node.node("connection-timeout-ms").getLong(defaults.connectionTimeoutMs());
    return new DatabaseConfig(
        type, host, port, database, username, password, url, poolMaxSize, connectionTimeoutMs);
  }

  private static Map<String, ModuleConfig> loadModules(ConfigurationNode node) {
    Map<String, ModuleConfig> modules = new HashMap<>();
    for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
      String name = String.valueOf(entry.getKey());
      boolean enabled = entry.getValue().node("enabled").getBoolean(false);
      modules.put(name, new ModuleConfig(enabled));
    }
    return modules;
  }
}
