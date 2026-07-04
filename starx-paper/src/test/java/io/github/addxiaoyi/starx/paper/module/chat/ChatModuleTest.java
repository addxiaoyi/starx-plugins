package io.github.addxiaoyi.starx.paper.module.chat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock Server server;
  @Mock PluginManager pluginManager;

  Logger logger = Logger.getLogger(ChatModuleTest.class.getName());
  ChatModule module;

  @BeforeEach
  void setUp() {
    when(plugin.getServer()).thenReturn(server);
    when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
    lenient().when(configLoader.getChatFormat()).thenReturn("<{player}> {message}");
    lenient().when(configLoader.isModuleEnabled("chat")).thenReturn(true);
    module = new ChatModule(plugin, configLoader);
  }

  @Test
  void shouldRegisterListenerOnEnable() {
    module.onEnable();
    verify(pluginManager).registerEvents(module, plugin);
  }

  @Test
  void shouldBroadcastCrossServerMessageWhenEnabled() {
    module.onEnable();

    try (var bukkit = mockStatic(Bukkit.class)) {
      module.onPluginMessage(
          new PluginMessage("chat_broadcast", Map.of("sender", "Alice", "message", "hello")));

      bukkit.verify(() -> Bukkit.broadcastMessage("<Alice> hello"));
    }
  }

  @Test
  void shouldNotBroadcastWhenDisabled() {
    when(configLoader.isModuleEnabled("chat")).thenReturn(false);
    module.onEnable();

    try (var bukkit = mockStatic(Bukkit.class)) {
      module.onPluginMessage(
          new PluginMessage("chat_broadcast", Map.of("sender", "Alice", "message", "hello")));

      bukkit.verify(() -> Bukkit.broadcastMessage(anyString()), never());
    }
  }
}
