package io.github.addxiaoyi.starx.paper.module;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.chat.ChatModule;
import io.github.addxiaoyi.starx.paper.module.maintenance.MaintenanceModule;
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

  private boolean isSkinModuleLoadable() {
    Plugin skinsRestorer = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
    return skinsRestorer != null && configLoader.isModuleEnabled("skin");
  }
}
