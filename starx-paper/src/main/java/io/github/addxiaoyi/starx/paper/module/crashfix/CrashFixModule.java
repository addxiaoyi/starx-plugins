package io.github.addxiaoyi.starx.paper.module.crashfix;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import io.github.addxiaoyi.starx.paper.scheduler.SchedulerAdapter;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;

/** 崩溃修复模块：拦截危险命令、书籍溢出，防止服务器崩溃。 */
public final class CrashFixModule implements PaperModule, Listener {

  private static final int MAX_BOOK_PAGE_SIZE = 256;
  private static final int MAX_BOOK_PAGES = 50;

  private static final Set<String> DANGEROUS_COMMAND_PATTERNS =
      Set.of(
          "//calc",
          "//eval",
          "//solve",
          "/worldedit:",
          "/execute as @e",
          "run kill @e",
          "fill ~",
          "clone ~");

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private boolean enabled;

  public CrashFixModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this.plugin = plugin;
    this.configLoader = configLoader;
  }

  @Override
  public String getName() {
    return "crashfix";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    enabled = configLoader.isModuleEnabled("crashfix");
    plugin.getLogger().info("CrashFix module enabled state: " + enabled);
  }

  @EventHandler
  public void onCommand(PlayerCommandPreprocessEvent event) {
    if (!enabled) {
      return;
    }
    if (event.getPlayer().hasPermission("starx.crashfix.bypass")) {
      return;
    }
    String command = event.getMessage().toLowerCase();
    for (String pattern : DANGEROUS_COMMAND_PATTERNS) {
      if (command.contains(pattern)) {
        event.setCancelled(true);
        plugin
            .getLogger()
            .warning(
                "Blocked dangerous command from "
                    + event.getPlayer().getName()
                    + ": "
                    + event.getMessage());
        // TODO: 通过 Plugin Messaging 向 Velocity 上报拦截事件
        return;
      }
    }
    // TODO: 添加 NBT 数据大小检测（拦截过大的 NBT 数据）
  }

  @EventHandler
  public void onBookEdit(PlayerEditBookEvent event) {
    if (!enabled) {
      return;
    }
    if (event.getPlayer().hasPermission("starx.crashfix.bypass")) {
      return;
    }
    var meta = event.getNewBookMeta();
    if (meta.getPageCount() > MAX_BOOK_PAGES) {
      event.setCancelled(true);
      plugin
          .getLogger()
          .warning(
              "Blocked oversized book (" + meta.getPageCount() + " pages) from "
                  + event.getPlayer().getName());
      return;
    }
    for (int i = 1; i <= meta.getPageCount(); i++) {
      String page = meta.getPage(i);
      if (page != null && page.length() > MAX_BOOK_PAGE_SIZE * 10) {
        event.setCancelled(true);
        plugin
            .getLogger()
            .warning(
                "Blocked oversized book page from " + event.getPlayer().getName());
        return;
      }
    }
  }
}