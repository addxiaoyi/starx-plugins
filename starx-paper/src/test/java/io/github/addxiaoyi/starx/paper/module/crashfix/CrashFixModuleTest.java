package io.github.addxiaoyi.starx.paper.module.crashfix;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrashFixModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock Server server;
  @Mock PluginManager pluginManager;
  @Mock Player player;

  Logger logger = Logger.getLogger(CrashFixModuleTest.class.getName());
  CrashFixModule module;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
    lenient().when(configLoader.isModuleEnabled("crashfix")).thenReturn(true);
    module = new CrashFixModule(plugin, configLoader);
  }

  @Test
  void shouldRegisterListenerOnEnable() {
    module.onEnable();
    verify(pluginManager).registerEvents(module, plugin);
  }

  @Test
  void shouldBlockDangerousCommand() {
    module.onEnable();

    PlayerCommandPreprocessEvent event = mock(PlayerCommandPreprocessEvent.class);
    lenient().when(event.getPlayer()).thenReturn(player);
    lenient().when(event.getMessage()).thenReturn("/execute as @e run kill @e");
    module.onCommand(event);

    verify(event).setCancelled(true);
  }

  @Test
  void shouldBlockOversizedBook() {
    module.onEnable();

    BookMeta meta = createOversizedBookMeta();
    PlayerEditBookEvent event = mock(PlayerEditBookEvent.class);
    lenient().when(event.getPlayer()).thenReturn(player);
    lenient().when(event.getNewBookMeta()).thenReturn(meta);
    module.onBookEdit(event);

    verify(event).setCancelled(true);
  }

  @Test
  void shouldNotBlockSafeCommand() {
    module.onEnable();

    PlayerCommandPreprocessEvent event = mock(PlayerCommandPreprocessEvent.class);
    lenient().when(event.getPlayer()).thenReturn(player);
    lenient().when(event.getMessage()).thenReturn("/help");
    module.onCommand(event);

    verify(event, never()).setCancelled(true);
  }

  private BookMeta createOversizedBookMeta() {
    BookMeta meta = mock(BookMeta.class);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      sb.append("abc");
    }
    lenient().when(meta.getPage(1)).thenReturn(sb.toString());
    lenient().when(meta.getPageCount()).thenReturn(1);
    return meta;
  }
}
