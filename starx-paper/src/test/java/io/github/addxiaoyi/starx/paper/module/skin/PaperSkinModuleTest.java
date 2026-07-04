package io.github.addxiaoyi.starx.paper.module.skin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.common.event.LocalEventBus;
import io.github.addxiaoyi.starx.common.skin.NoopSkinRepository;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaperSkinModuleTest {

  @Mock StarxPaperPlugin plugin;
  @Mock Server server;
  @Mock PluginManager pluginManager;
  @Mock Plugin skinsRestorerPlugin;

  Logger logger = Logger.getLogger(PaperSkinModuleTest.class.getName());
  LocalEventBus eventBus = new LocalEventBus();

  @BeforeEach
  void setUp() {
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getPluginManager()).thenReturn(pluginManager);
    lenient().when(plugin.getLogger()).thenReturn(logger);
  }

  @Test
  void enablesWhenSkinsRestorerIsPresent() {
    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
      when(pluginManager.getPlugin("SkinsRestorer")).thenReturn(skinsRestorerPlugin);

      PaperSkinModule module = new PaperSkinModule(plugin, eventBus, NoopSkinRepository::new);
      module.onEnable();

      assertThat(module.isEnabled()).isTrue();
    }
  }

  @Test
  void staysDisabledWhenSkinsRestorerIsAbsent() {
    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
      when(pluginManager.getPlugin("SkinsRestorer")).thenReturn(null);

      PaperSkinModule module = new PaperSkinModule(plugin, eventBus, NoopSkinRepository::new);
      module.onEnable();

      assertThat(module.isEnabled()).isFalse();
    }
  }

  @Test
  void refreshSkinDoesNothingWhenDisabled() {
    AtomicReference<UUID> received = new AtomicReference<>();
    eventBus.subscribe(
        EventTypes.SKIN_REFRESH_REQUEST,
        event -> received.set(UUID.fromString((String) event.payload().get("uuid"))));

    PaperSkinModule module = new PaperSkinModule(plugin, eventBus, NoopSkinRepository::new);
    module.refreshSkin(UUID.randomUUID(), "TestPlayer");

    assertThat(received.get()).isNull();
  }

  @Test
  void refreshSkinPublishesRefreshRequestWhenEnabledAndRepositoryEmpty() {
    UUID uuid = UUID.randomUUID();
    AtomicReference<UUID> received = new AtomicReference<>();
    eventBus.subscribe(
        EventTypes.SKIN_REFRESH_REQUEST,
        event -> received.set(UUID.fromString((String) event.payload().get("uuid"))));

    PaperSkinModule module = new PaperSkinModule(plugin, eventBus, NoopSkinRepository::new);
    try (var bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
      when(pluginManager.getPlugin("SkinsRestorer")).thenReturn(skinsRestorerPlugin);
      module.onEnable();
    }

    module.refreshSkin(uuid, "TestPlayer");

    assertThat(received.get()).isEqualTo(uuid);
  }
}
