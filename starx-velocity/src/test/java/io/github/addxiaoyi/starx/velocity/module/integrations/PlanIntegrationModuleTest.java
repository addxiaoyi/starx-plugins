package io.github.addxiaoyi.starx.velocity.module.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.messaging.VelocityMessageBridge;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanIntegrationModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock ProxyServer proxy;
  @Mock EventBus eventBus;
  @Mock VelocityMessageBridge messageBridge;
  @Mock EventManager eventManager;
  @Mock Scheduler scheduler;

  PlanIntegrationModule.Config enabledConfig;
  PlanIntegrationModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.proxy()).thenReturn(proxy);
    lenient().when(proxy.getEventManager()).thenReturn(eventManager);
    lenient().when(proxy.getScheduler()).thenReturn(scheduler);
    Scheduler.TaskBuilder taskBuilder = mock(Scheduler.TaskBuilder.class);
    ScheduledTask scheduledTask = mock(ScheduledTask.class);
    lenient().when(scheduler.buildTask(any(), any(Runnable.class))).thenReturn(taskBuilder);
    lenient()
        .when(taskBuilder.repeat(any(Long.class), any(TimeUnit.class)))
        .thenReturn(taskBuilder);
    lenient().when(taskBuilder.schedule()).thenReturn(scheduledTask);
    enabledConfig =
        new PlanIntegrationModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public int collectIntervalSec() {
            return 60;
          }

          @Override
          public int maxDataPoints() {
            return 10080;
          }
        };
    disabledConfig =
        new PlanIntegrationModule.Config() {
          @Override
          public boolean enabled() {
            return false;
          }

          @Override
          public int collectIntervalSec() {
            return 60;
          }

          @Override
          public int maxDataPoints() {
            return 10080;
          }
        };
  }

  @Test
  void shouldHaveCorrectModuleName() {
    PlanIntegrationModule module =
        new PlanIntegrationModule(plugin, eventBus, messageBridge, enabledConfig);
    assertThat(module.name()).isEqualTo("integrations.plan");
  }

  @Test
  void shouldStartDataCollectionOnEnable() {
    PlanIntegrationModule module =
        new PlanIntegrationModule(plugin, eventBus, messageBridge, enabledConfig);
    module.onEnable();
    assertThat(module.isCollecting()).isTrue();
  }

  @Test
  void shouldStopDataCollectionOnDisable() {
    PlanIntegrationModule module =
        new PlanIntegrationModule(plugin, eventBus, messageBridge, enabledConfig);
    module.onEnable();
    assertThat(module.isCollecting()).isTrue();
    module.onDisable();
    assertThat(module.isCollecting()).isFalse();
  }

  @Test
  void shouldNotStartCollectionWhenDisabled() {
    PlanIntegrationModule module =
        new PlanIntegrationModule(plugin, eventBus, messageBridge, disabledConfig);
    module.onEnable();
    assertThat(module.isCollecting()).isFalse();
  }

  @Test
  void shouldCollectAndStoreDataPoints() {
    PlanIntegrationModule module =
        new PlanIntegrationModule(plugin, eventBus, messageBridge, enabledConfig);
    when(proxy.getPlayerCount()).thenReturn(5);

    module.collectDataPoint();

    assertThat(module.getDataPoints()).isNotEmpty();
    assertThat(module.getDataPoints()).hasSize(1);
    assertThat(module.getDataPoints().get(0)).containsEntry("online_players", 5);
  }

  @Test
  void shouldTrimDataPointsWhenExceedingMax() {
    PlanIntegrationModule.Config smallConfig =
        new PlanIntegrationModule.Config() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public int collectIntervalSec() {
            return 1;
          }

          @Override
          public int maxDataPoints() {
            return 3;
          }
        };
    PlanIntegrationModule module =
        new PlanIntegrationModule(plugin, eventBus, messageBridge, smallConfig);
    when(proxy.getPlayerCount()).thenReturn(1);

    for (int i = 0; i < 5; i++) {
      module.collectDataPoint();
    }

    assertThat(module.getDataPoints()).hasSize(3);
  }

  @Test
  void shouldProvideHttpEndpointData() {
    PlanIntegrationModule module =
        new PlanIntegrationModule(plugin, eventBus, messageBridge, enabledConfig);
    when(proxy.getPlayerCount()).thenReturn(10);
    module.collectDataPoint();

    Map<String, Object> snapshot = module.getSnapshot();

    assertThat(snapshot).containsKeys("online_players", "data_points", "collect_interval_sec");
    assertThat(snapshot.get("online_players")).isEqualTo(10);
    assertThat(snapshot.get("collect_interval_sec")).isEqualTo(60);
  }
}
