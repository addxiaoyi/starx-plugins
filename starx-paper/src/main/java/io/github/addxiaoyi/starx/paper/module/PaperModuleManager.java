package io.github.addxiaoyi.starx.paper.module;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.anticheat.AnticheatModule;
import io.github.addxiaoyi.starx.paper.module.chat.ChatModule;
import io.github.addxiaoyi.starx.paper.module.crashfix.CrashFixModule;
import io.github.addxiaoyi.starx.paper.module.filecleaner.FileCleanerModule;
import io.github.addxiaoyi.starx.paper.module.maintenance.MaintenanceModule;
import io.github.addxiaoyi.starx.paper.module.mapmod.MapModModule;
import io.github.addxiaoyi.starx.paper.module.networking.NetworkingModule;
import io.github.addxiaoyi.starx.paper.module.plan.PlanModule;
import io.github.addxiaoyi.starx.paper.module.qq.QqModule;
import io.github.addxiaoyi.starx.paper.module.security.BlossomGuardModule;
import io.github.addxiaoyi.starx.paper.module.skin.PaperSkinModule;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** 管理后端模块，根据依赖存在性和配置启用状态加载模块。 */
public final class PaperModuleManager {

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private final List<PaperModule> modules = new ArrayList<>();

  public PaperModuleManager(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  public void loadModules() {
    modules.add(new MaintenanceModule(plugin, configLoader));
    modules.add(new ChatModule(plugin, configLoader));
    if (isSkinModuleLoadable()) {
      modules.add(new PaperSkinModule(plugin));
    }

    if (configLoader.isModuleEnabled("anticheat")) {
      modules.add(new AnticheatModule(plugin, configLoader));
    }
    if (configLoader.isModuleEnabled("crashfix")) {
      modules.add(new CrashFixModule(plugin, configLoader));
    }
    if (configLoader.isModuleEnabled("networking")) {
      modules.add(new NetworkingModule(plugin, configLoader));
    }
    if (configLoader.isModuleEnabled("mapmod")) {
      modules.add(new MapModModule(plugin, configLoader));
    }
    if (configLoader.isModuleEnabled("qq")) {
      modules.add(new QqModule(plugin, configLoader));
    }
    if (configLoader.isModuleEnabled("plan")) {
      modules.add(new PlanModule(plugin, configLoader));
    }
    if (configLoader.isModuleEnabled("filecleaner")) {
      modules.add(new FileCleanerModule(plugin, configLoader));
    }
    if (configLoader.isModuleEnabled("security.blossom")) {
      modules.add(new BlossomGuardModule(plugin, configLoader));
    }

    for (PaperModule module : modules) {
      try {
        module.onEnable();
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Failed to enable module: " + module.getName(), e);
      }
    }
  }

  public void unloadModules() {
    for (PaperModule module : modules) {
      try {
        module.onDisable();
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Failed to disable module: " + module.getName(), e);
      }
    }
    modules.clear();
  }

  public List<PaperModule> getModules() {
    return List.copyOf(modules);
  }

  public void handlePluginMessage(PluginMessage message) {
    for (PaperModule module : modules) {
      try {
        module.onPluginMessage(message);
      } catch (Exception e) {
        plugin
            .getLogger()
            .log(
                Level.SEVERE,
                "Failed to dispatch plugin message to module: " + module.getName(),
                e);
      }
    }
  }

  private boolean isSkinModuleLoadable() {
    Plugin skinsRestorer = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
    return skinsRestorer != null && configLoader.isModuleEnabled("skin");
  }
}
