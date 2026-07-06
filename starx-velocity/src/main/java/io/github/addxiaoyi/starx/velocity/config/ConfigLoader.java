package io.github.addxiaoyi.starx.velocity.config;

import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthConfig;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
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

      database:
        type: "sqlite"
        host: ""
        port: 3306
        database: "plugins/starx/data.db"
        username: "starx"
        password: ""
        url: ""
        pool-max-size: 2
        connection-timeout-ms: 30000

      uniauth:
        enabled: false
        api-url: "https://api.example.com/uniauth/"
        app-id: ""
        app-secret: ""
        timeout-ms: 5000
        bridge-mode: false

      modules:
        auth:
          enabled: true
        auth.yggdrasil:
          enabled: true
        auth.uniauth:
          enabled: false
        auth.floodgate:
          enabled: true
        auth.tab:
          enabled: true
        auth.migration:
          enabled: false
        skin-bridge:
          enabled: true
        messaging:
          enabled: true
        proxytools.maintenance:
          enabled: true
        proxytools.motd:
          enabled: true
        proxytools.chat:
          enabled: true
        proxytools.redirect:
          enabled: true
        proxytools.queue:
          enabled: true
        proxytools.limbo:
          enabled: true
        proxytools.reconnect:
          enabled: true
        proxytools.info:
          enabled: true
        proxytools.forge:
          enabled: false
        proxytools.raknet:
          enabled: false
        proxytools.online:
          enabled: true
        proxytools.enhanced:
          enabled: true
        proxytools.filecleaner:
          enabled: false
        security.bot:
          enabled: true
        security.crash:
          enabled: true
        security.risk:
          enabled: true
        security.anticheat:
          enabled: true
        integrations.qq:
          enabled: false
        integrations.plan:
          enabled: false
        integrations.mapmod:
          enabled: false
        integrations.social:
          enabled: false
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

    DatabaseConfig database = parseDatabaseConfig(root.node("database"));
    UniAuthConfig uniauth = parseUniAuthConfig(root.node("uniauth"));

    return new StarxConfig(apiKey, http, webhook, database, uniauth, modules);
  }

  private static UniAuthConfig parseUniAuthConfig(ConfigurationNode node) {
    boolean enabled = node.node("enabled").getBoolean(false);
    String apiUrl = node.node("api-url").getString("https://api.example.com/uniauth/");
    String appId = node.node("app-id").getString("");
    String appSecret = node.node("app-secret").getString("");
    int timeoutMs = node.node("timeout-ms").getInt(5000);
    boolean bridgeMode = node.node("bridge-mode").getBoolean(false);
    return new UniAuthConfig(enabled, apiUrl, appId, appSecret, timeoutMs, bridgeMode);
  }

  private static DatabaseConfig parseDatabaseConfig(ConfigurationNode node) {
    String type = node.node("type").getString("h2");
    String host = node.node("host").getString("");
    int port = node.node("port").getInt(3306);
    String database = node.node("database").getString("starx");
    String username = node.node("username").getString("starx");
    String password = node.node("password").getString("");
    String url = node.node("url").getString("");
    int poolMaxSize = node.node("pool-max-size").getInt(10);
    long connectionTimeoutMs = node.node("connection-timeout-ms").getLong(30_000L);
    return new DatabaseConfig(
        type, host, port, database, username, password, url, poolMaxSize, connectionTimeoutMs);
  }
}
