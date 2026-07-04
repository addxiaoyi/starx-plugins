package io.github.addxiaoyi.starx.paper.module.networking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;
import io.github.addxiaoyi.starx.api.messaging.PluginMessageChannels;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NetworkingModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock PaperConfigLoader configLoader;
  @Mock Server server;
  @Mock PluginManager pluginManager;
  @Mock Messenger messenger;

  Logger logger = Logger.getLogger(NetworkingModuleTest.class.getName());
  NetworkingModule module;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(server.getMessenger()).thenReturn(messenger);
    lenient().when(plugin.getLogger()).thenReturn(logger);
    lenient().when(configLoader.isModuleEnabled("networking")).thenReturn(true);
    module = new NetworkingModule(plugin, configLoader);
  }

  @Test
  void shouldRegisterCustomChannelOnEnable() {
    module.onEnable();
    verify(messenger).registerOutgoingPluginChannel(plugin, "starx:networking");
    verify(messenger).registerIncomingPluginChannel(eq(plugin), eq("starx:networking"), any());
  }

  @Test
  void shouldHandleSkinSyncMessage() {
    module.onEnable();

    PluginMessage msg =
        new PluginMessage(
            PluginMessageChannels.CMD_NETWORKING_SYNC,
            Map.of("type", "skin_sync", "player", "test"));

    assertThat(module.isEnabled()).isTrue();
  }

  @Test
  void shouldHandleConfigSyncMessage() {
    module.onEnable();

    PluginMessage msg =
        new PluginMessage(PluginMessageChannels.CMD_NETWORKING_SYNC, Map.of("type", "config_sync"));

    assertThat(module.isEnabled()).isTrue();
  }
}
