package io.github.addxiaoyi.starx.paper;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.messaging.PaperMessageBridge;
import io.github.addxiaoyi.starx.paper.module.PaperModuleManager;
import io.github.addxiaoyi.starx.paper.scheduler.SchedulerAdapter;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/** StarX Paper 后端插件入口。 */
public class StarxPaperPlugin extends JavaPlugin {

  private PaperConfigLoader configLoader;
  private SchedulerAdapter scheduler;
  private PaperModuleManager moduleManager;
  private PaperMessageBridge messageBridge;

  @Override
  public void onEnable() {
    configLoader = new PaperConfigLoader(this);
    try {
      configLoader.load();
    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Failed to load configuration", e);
    }

    scheduler = new SchedulerAdapter(this);
    moduleManager = new PaperModuleManager(this, configLoader);
    moduleManager.loadModules();

    messageBridge = new PaperMessageBridge(this, this::handlePluginMessage);
    messageBridge.register();

    getLogger().info("StarX Paper backend enabled.");
  }

  @Override
  public void onDisable() {
    if (messageBridge != null) {
      messageBridge.unregister();
    }
    if (moduleManager != null) {
      moduleManager.unloadModules();
    }
    getLogger().info("StarX Paper backend disabled.");
  }

  private void handlePluginMessage(PluginMessage message) {
    if (moduleManager != null) {
      moduleManager.handlePluginMessage(message);
    }
  }

  public SchedulerAdapter getSchedulerAdapter() {
    return scheduler;
  }

  public PaperModuleManager getModuleManager() {
    return moduleManager;
  }

  public PaperConfigLoader getConfigLoader() {
    return configLoader;
  }
}
