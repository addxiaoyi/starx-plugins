package io.github.addxiaoyi.starx.velocity.module.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("deprecation")
class AnticheatModuleTest {

  private static final Gson GSON = new Gson();
  private static final UUID PLAYER_UUID = UUID.randomUUID();

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock EventManager eventManager;
  @Mock ChannelRegistrar channelRegistrar;
  @Mock CommandManager commandManager;

  AnticheatModule.Config config;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getChannelRegistrar()).thenReturn(channelRegistrar);
    lenient().when(proxy.getCommandManager()).thenReturn(commandManager);
    CommandMeta.Builder metaBuilder = mock(CommandMeta.Builder.class);
    CommandMeta meta = mock(CommandMeta.class);
    lenient().when(commandManager.metaBuilder("starx")).thenReturn(metaBuilder);
    lenient().when(metaBuilder.aliases("sx")).thenReturn(metaBuilder);
    lenient().when(metaBuilder.plugin(plugin)).thenReturn(metaBuilder);
    lenient().when(metaBuilder.build()).thenReturn(meta);
    config = AnticheatModule.Config.defaultConfig();
  }

  @Test
  void shouldReturnCorrectName() {
    AnticheatModule module = new AnticheatModule(plugin, eventBus, config);

    assertThat(module.name()).isEqualTo("security.anticheat");
  }

  @Test
  void shouldRegisterEventListenersOnEnable() {
    AnticheatModule module = new AnticheatModule(plugin, eventBus, config);

    module.onEnable();

    verify(channelRegistrar, times(1)).register(any(ChannelIdentifier.class));
    verify(eventManager, times(3)).register(eq(plugin), any());
  }

  @Test
  void shouldProcessPluginMessageDetectionAndPublishAlert() {
    AnticheatModule module = new AnticheatModule(plugin, eventBus, config);
    module.onEnable();

    PluginMessageEvent event =
        createDetectionMessage(PLAYER_UUID, "Speed", "flying", 6, "speed=1.5");

    module.onPluginMessage(event);

    verify(eventBus, atLeastOnce()).publish(any(StarxEvent.class));
  }

  @Test
  void shouldNotPublishAlertBelowThreshold() {
    AnticheatModule.Config strictConfig =
        new AnticheatModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public int alertThreshold() {
            return 10;
          }

          @Override
          public long collectIntervalMs() {
            return 60000;
          }

          @Override
          public List<String> enabledChecks() {
            return Arrays.asList("Speed", "Fly", "KillAura");
          }
        };

    AnticheatModule module = new AnticheatModule(plugin, eventBus, strictConfig);
    module.onEnable();

    PluginMessageEvent event = createDetectionMessage(PLAYER_UUID, "Speed", "flying", 3, "debug");

    module.onPluginMessage(event);

    verify(eventBus, never())
        .publish(
            org.mockito.Mockito.<StarxEvent>argThat(
                e -> SecurityEvents.SECURITY_ALERT.equals(e.type())));
  }

  @Test
  void shouldClearDetectionDataOnDisable() {
    AnticheatModule module = new AnticheatModule(plugin, eventBus, config);
    module.onEnable();

    PluginMessageEvent event = createDetectionMessage(PLAYER_UUID, "Speed", "flying", 6, "debug");
    module.onPluginMessage(event);

    module.onDisable();

    assertThat(module.getDetectionCount(PLAYER_UUID)).isEqualTo(0);
  }

  @Test
  void shouldTrackPlayerOnLogin() {
    AnticheatModule module = new AnticheatModule(plugin, eventBus, config);
    module.onEnable();

    LoginEvent loginEvent = createLoginEvent(PLAYER_UUID, "TestPlayer", "192.168.1.1");
    module.onLogin(loginEvent);

    assertThat(module.getDetectionCount(PLAYER_UUID)).isEqualTo(0);
    assertThat(module.isPlayerTracked(PLAYER_UUID)).isTrue();
  }

  @Test
  void shouldAggregateMultipleDetections() {
    AnticheatModule module = new AnticheatModule(plugin, eventBus, config);
    module.onEnable();

    module.onLogin(createLoginEvent(PLAYER_UUID, "TestPlayer", "192.168.1.1"));

    module.onPluginMessage(createDetectionMessage(PLAYER_UUID, "Speed", "flying", 2, "d1"));
    module.onPluginMessage(createDetectionMessage(PLAYER_UUID, "Fly", "flying", 2, "d2"));
    module.onPluginMessage(createDetectionMessage(PLAYER_UUID, "KillAura", "combat", 2, "d3"));

    assertThat(module.getDetectionCount(PLAYER_UUID)).isEqualTo(6);
  }

  @Test
  void shouldIgnoreDisabledChecks() {
    AnticheatModule.Config filteredConfig =
        new AnticheatModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public int alertThreshold() {
            return 5;
          }

          @Override
          public long collectIntervalMs() {
            return 60000;
          }

          @Override
          public List<String> enabledChecks() {
            return Arrays.asList("Speed");
          }
        };

    AnticheatModule module = new AnticheatModule(plugin, eventBus, filteredConfig);
    module.onEnable();

    module.onLogin(createLoginEvent(PLAYER_UUID, "TestPlayer", "192.168.1.1"));

    module.onPluginMessage(createDetectionMessage(PLAYER_UUID, "KillAura", "combat", 10, "debug"));

    assertThat(module.getDetectionCount(PLAYER_UUID)).isEqualTo(0);
  }

  private PluginMessageEvent createDetectionMessage(
      UUID playerUuid, String check, String category, int vl, String debug) {
    Player player = mock(Player.class);
    lenient().when(player.getUniqueId()).thenReturn(playerUuid);
    lenient().when(player.getUsername()).thenReturn("TestPlayer");

    Map<String, Object> payload =
        Map.of(
            "player", playerUuid.toString(),
            "check", check,
            "category", category,
            "vl", vl,
            "debug", debug);
    byte[] jsonBytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("anticheat:detection");
    out.writeInt(jsonBytes.length);
    out.write(jsonBytes);

    PluginMessageEvent event = mock(PluginMessageEvent.class);
    ChannelIdentifier channel = MinecraftChannelIdentifier.create("starx", "anticheat");
    when(event.getIdentifier()).thenReturn(channel);
    when(event.getSource()).thenReturn(player);
    when(event.getData()).thenReturn(out.toByteArray());
    return event;
  }

  private LoginEvent createLoginEvent(UUID uuid, String username, String ip) {
    LoginEvent event = mock(LoginEvent.class);
    Player player = mock(Player.class);
    lenient().when(player.getUniqueId()).thenReturn(uuid);
    lenient().when(player.getUsername()).thenReturn(username);
    lenient().when(player.getRemoteAddress()).thenReturn(new InetSocketAddress(ip, 25565));
    when(event.getPlayer()).thenReturn(player);
    return event;
  }
}
