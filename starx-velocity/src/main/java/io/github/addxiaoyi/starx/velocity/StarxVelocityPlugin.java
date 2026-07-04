package io.github.addxiaoyi.starx.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.config.ConfigLoader;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.database.DatabaseManager;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import io.github.addxiaoyi.starx.velocity.http.HttpApiServer;
import io.github.addxiaoyi.starx.velocity.http.WebhookClient;
import io.github.addxiaoyi.starx.velocity.messaging.VelocityMessageBridge;
import io.github.addxiaoyi.starx.velocity.module.ModuleManager;
import io.github.addxiaoyi.starx.velocity.module.auth.AuthModule;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import io.github.addxiaoyi.starx.velocity.security.HmacWebhookSigner;
import java.nio.file.Path;
import java.util.logging.Logger;

/** StarX Velocity 代理端入口。 */
@Plugin(
    id = "starx",
    name = "StarX",
    version = "0.1.0-SNAPSHOT",
    dependencies = {
      @Dependency(id = "limboapi"),
      @Dependency(id = "skinsrestorer", optional = true)
    })
public class StarxVelocityPlugin {

  private final ProxyServer proxy;
  private final Logger logger;
  private final Path dataDirectory;

  private StarxConfig config;
  private DatabaseManager databaseManager;
  private EventBus eventBus;
  private HttpApiServer httpApiServer;
  private WebhookClient webhookClient;
  private ModuleManager moduleManager;

  @Inject
  public StarxVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
    this.proxy = proxy;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  public ProxyServer proxy() {
    return proxy;
  }

  public Logger logger() {
    return logger;
  }

  public Path dataDirectory() {
    return dataDirectory;
  }

  public StarxConfig config() {
    return config;
  }

  public EventBus eventBus() {
    return eventBus;
  }

  public HttpApiServer httpApiServer() {
    return httpApiServer;
  }

  public WebhookClient webhookClient() {
    return webhookClient;
  }

  public ModuleManager moduleManager() {
    return moduleManager;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) throws Exception {
    logger.info("StarX Velocity 初始化中...");

    config = ConfigLoader.load(dataDirectory.resolve("config.yml"));
    eventBus = new VelocityEventBus();
    databaseManager = new DatabaseManager(config);
    databaseManager.initialize();

    moduleManager = new ModuleManager(config);
    SkinBridgeModule skinBridge = new SkinBridgeModule(proxy, null, eventBus);
    moduleManager.register(new AuthModule(this, eventBus));
    moduleManager.register(skinBridge);
    moduleManager.register(new VelocityMessageBridge(this, proxy, eventBus));

    httpApiServer = new HttpApiServer(config, skinBridge);
    webhookClient =
        new WebhookClient(config.webhook(), new HmacWebhookSigner(config.webhook().secret()));

    moduleManager.enableAll();
    httpApiServer.start();

    logger.info("StarX Velocity 初始化完成");
  }
}
