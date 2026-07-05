package io.github.addxiaoyi.starx.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.config.ConfigLoader;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.database.DatabaseManager;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import io.github.addxiaoyi.starx.velocity.http.HttpApiServer;
import io.github.addxiaoyi.starx.velocity.http.WebhookClient;
import io.github.addxiaoyi.starx.velocity.http.WebhookEventPublisher;
import io.github.addxiaoyi.starx.velocity.messaging.VelocityMessageBridge;
import io.github.addxiaoyi.starx.velocity.module.ModuleManager;
import io.github.addxiaoyi.starx.velocity.module.auth.AuthModule;
import io.github.addxiaoyi.starx.velocity.module.auth.FloodgateModule;
import io.github.addxiaoyi.starx.velocity.module.auth.MigrationModule;
import io.github.addxiaoyi.starx.velocity.module.auth.TabIntegrationModule;
import io.github.addxiaoyi.starx.velocity.module.auth.UniAuthModule;
import io.github.addxiaoyi.starx.velocity.module.auth.YggdrasilModule;
import io.github.addxiaoyi.starx.velocity.module.integrations.MapModIntegrationModule;
import io.github.addxiaoyi.starx.velocity.module.integrations.PlanIntegrationModule;
import io.github.addxiaoyi.starx.velocity.module.integrations.QqIntegrationModule;
import io.github.addxiaoyi.starx.velocity.module.integrations.SocialIntegrationModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.ChatModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.EnhancedProxyModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.FileCleanerModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.ForgeCompatModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.LimboHubModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.MaintenanceModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.MotdModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.OnlineSyncModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.ProxyInfoModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.QueueModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.RakNetModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.ReconnectModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.RedirectModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.queue.QueueService;
import io.github.addxiaoyi.starx.velocity.module.security.AnticheatModule;
import io.github.addxiaoyi.starx.velocity.module.security.BlossomGuardModule;
import io.github.addxiaoyi.starx.velocity.module.security.BotFilterModule;
import io.github.addxiaoyi.starx.velocity.module.security.CrashFixModule;
import io.github.addxiaoyi.starx.velocity.module.security.RiskModule;
import io.github.addxiaoyi.starx.velocity.module.skin.SkinBridgeModule;
import io.github.addxiaoyi.starx.velocity.security.HmacWebhookSigner;
import java.nio.file.Path;
import java.util.logging.Logger;

/** StarX Velocity 代理端入口，插件元数据由 velocity-plugin.json 定义。 */
public class StarxVelocityPlugin {

  private final ProxyServer proxy;
  private final Logger logger;
  private final Path dataDirectory;

  private StarxConfig config;
  private DatabaseManager databaseManager;
  private VelocityEventBus eventBus;
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
    databaseManager = new DatabaseManager(config.database());

    moduleManager = new ModuleManager(config);
    SkinBridgeModule skinBridge = new SkinBridgeModule(proxy, eventBus);
    AuthModule authModule = new AuthModule(this, eventBus);
    VelocityMessageBridge messageBridge = new VelocityMessageBridge(this, proxy, eventBus);
    moduleManager.register(authModule);
    moduleManager.register(skinBridge);
    moduleManager.register(messageBridge);
    moduleManager.register(
        new YggdrasilModule(this, eventBus, YggdrasilModule.Config.defaultConfig()));
    moduleManager.register(new UniAuthModule(this, eventBus, UniAuthModule.Config.defaultConfig()));
    moduleManager.register(
        new FloodgateModule(this, eventBus, FloodgateModule.Config.defaultConfig()));
    moduleManager.register(
        new TabIntegrationModule(this, eventBus, TabIntegrationModule.Config.defaultConfig()));
    moduleManager.register(
        new MigrationModule(this, eventBus, MigrationModule.Config.defaultConfig()));
    moduleManager.register(
        new MaintenanceModule(
            this, eventBus, messageBridge, MaintenanceModule.Config.defaultConfig()));
    moduleManager.register(new MotdModule(this, eventBus, MotdModule.Config.defaultConfig()));
    moduleManager.register(new ChatModule(this, messageBridge, ChatModule.Config.defaultConfig()));
    moduleManager.register(new RedirectModule(this, RedirectModule.Config.defaultConfig()));
    moduleManager.register(
        new QueueModule(this, QueueModule.Config.defaultConfig(), new QueueService()));
    moduleManager.register(new LimboHubModule(this, LimboHubModule.Config.defaultConfig()));
    moduleManager.register(new ReconnectModule(this, ReconnectModule.Config.defaultConfig()));
    moduleManager.register(new ProxyInfoModule(this, ProxyInfoModule.Config.defaultConfig()));
    moduleManager.register(new ForgeCompatModule(this, ForgeCompatModule.Config.defaultConfig()));
    moduleManager.register(new OnlineSyncModule(this, OnlineSyncModule.Config.defaultConfig()));
    moduleManager.register(
        new EnhancedProxyModule(this, EnhancedProxyModule.Config.simpleDefault()));
    moduleManager.register(new FileCleanerModule(this, FileCleanerModule.Config.defaultConfig()));
    moduleManager.register(new RakNetModule(this, RakNetModule.Config.defaultConfig()));
    moduleManager.register(
        new BotFilterModule(this, eventBus, BotFilterModule.Config.defaultConfig()));
    moduleManager.register(
        new CrashFixModule(this, eventBus, CrashFixModule.Config.defaultConfig()));
    moduleManager.register(new RiskModule(this, eventBus, RiskModule.Config.defaultConfig()));
    moduleManager.register(
        new AnticheatModule(this, eventBus, AnticheatModule.Config.defaultConfig()));
    moduleManager.register(new BlossomGuardModule(this));
    moduleManager.register(
        new QqIntegrationModule(this, webhookClient, QqIntegrationModule.Config.defaultConfig()));
    moduleManager.register(
        new PlanIntegrationModule(
            this, eventBus, messageBridge, PlanIntegrationModule.Config.defaultConfig()));
    moduleManager.register(
        new MapModIntegrationModule(this, MapModIntegrationModule.Config.defaultConfig()));
    moduleManager.register(
        new SocialIntegrationModule(
            this, eventBus, SocialIntegrationModule.Config.defaultConfig()));

    httpApiServer =
        new HttpApiServer(
            config,
            eventBus,
            proxy,
            authModule.userRepository(),
            authModule.authService(),
            skinBridge);
    webhookClient =
        new WebhookClient(config.webhook(), new HmacWebhookSigner(config.webhook().secret()));
    new WebhookEventPublisher(eventBus, webhookClient).register();

    moduleManager.enableAll();
    httpApiServer.start();

    logger.info("StarX Velocity 初始化完成");
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    logger.info("StarX Velocity 正在关闭...");
    if (httpApiServer != null) {
      httpApiServer.stop();
    }
    if (moduleManager != null) {
      moduleManager.disableAll();
    }
    if (databaseManager != null) {
      databaseManager.close();
    }
    logger.info("StarX Velocity 已关闭");
  }
}
