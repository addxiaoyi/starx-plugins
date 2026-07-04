package io.github.addxiaoyi.starx.velocity.module.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialIntegrationModuleTest {

  @Mock StarxVelocityPlugin plugin;
  @Mock Logger logger;

  VelocityEventBus eventBus;
  SocialIntegrationModule.Config enabledConfig;
  SocialIntegrationModule.Config disabledConfig;

  @BeforeEach
  void setUp() {
    lenient().when(plugin.logger()).thenReturn(logger);
    eventBus = new VelocityEventBus();
    enabledConfig = new SocialIntegrationModule.Config() {
      @Override
      public boolean enabled() {
        return true;
      }

      @Override
      public SocialIntegrationModule.DiscordConfig discord() {
        return new SocialIntegrationModule.DiscordConfig() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public String token() {
            return "test-token";
          }
        };
      }

      @Override
      public SocialIntegrationModule.TelegramConfig telegram() {
        return new SocialIntegrationModule.TelegramConfig() {
          @Override
          public boolean enabled() {
            return false;
          }

          @Override
          public String token() {
            return "";
          }
        };
      }

      @Override
      public List<List<SocialIntegrationModule.KeyboardItem>> commands() {
        return List.of();
      }

      @Override
      public SocialIntegrationModule.Strings strings() {
        return new SocialIntegrationModule.Strings() {
          @Override
          public String linkSuccess() {
            return "Linked!";
          }

          @Override
          public String unlinkSuccess() {
            return "Unlinked!";
          }

          @Override
          public String notLinked() {
            return "Not linked";
          }

          @Override
          public String alreadyLinked() {
            return "Already linked";
          }
        };
      }
    };
    disabledConfig = new SocialIntegrationModule.Config() {
      @Override
      public boolean enabled() {
        return false;
      }

      @Override
      public SocialIntegrationModule.DiscordConfig discord() {
        return new SocialIntegrationModule.DiscordConfig() {
          @Override
          public boolean enabled() {
            return false;
          }

          @Override
          public String token() {
            return "";
          }
        };
      }

      @Override
      public SocialIntegrationModule.TelegramConfig telegram() {
        return new SocialIntegrationModule.TelegramConfig() {
          @Override
          public boolean enabled() {
            return false;
          }

          @Override
          public String token() {
            return "";
          }
        };
      }

      @Override
      public List<List<SocialIntegrationModule.KeyboardItem>> commands() {
        return List.of();
      }

      @Override
      public SocialIntegrationModule.Strings strings() {
        return new SocialIntegrationModule.Strings() {
          @Override
          public String linkSuccess() {
            return "Linked!";
          }

          @Override
          public String unlinkSuccess() {
            return "Unlinked!";
          }

          @Override
          public String notLinked() {
            return "Not linked";
          }

          @Override
          public String alreadyLinked() {
            return "Already linked";
          }
        };
      }
    };
  }

  @Test
  void shouldHaveCorrectModuleName() {
    SocialIntegrationModule module = new SocialIntegrationModule(plugin, eventBus, enabledConfig);
    assertThat(module.name()).isEqualTo("integrations.social");
  }

  @Test
  void shouldNotInitializeWhenDisabled() {
    SocialIntegrationModule module = new SocialIntegrationModule(plugin, eventBus, disabledConfig);
    module.onEnable();
    assertThat(module.isInitialized()).isFalse();
  }

  @Test
  void shouldInitializeWhenEnabled() {
    SocialIntegrationModule module = new SocialIntegrationModule(plugin, eventBus, enabledConfig);
    module.onEnable();
    assertThat(module.isInitialized()).isTrue();
  }

  @Test
  void shouldCleanUpOnDisable() {
    SocialIntegrationModule module = new SocialIntegrationModule(plugin, eventBus, enabledConfig);
    module.onEnable();
    assertThat(module.isInitialized()).isTrue();
    module.onDisable();
    assertThat(module.isInitialized()).isFalse();
  }

  @Test
  void shouldLinkPlayerAccount() {
    SocialIntegrationModule module = new SocialIntegrationModule(plugin, eventBus, enabledConfig);
    module.onEnable();
    module.linkPlayer("testPlayer", "discord", "123456789");
    assertThat(module.getLinkedDiscordId("testPlayer")).isEqualTo("123456789");
  }

  @Test
  void shouldUnlinkPlayerAccount() {
    SocialIntegrationModule module = new SocialIntegrationModule(plugin, eventBus, enabledConfig);
    module.onEnable();
    module.linkPlayer("testPlayer", "discord", "123456789");
    assertThat(module.getLinkedDiscordId("testPlayer")).isEqualTo("123456789");
    module.unlinkPlayer("testPlayer", "discord");
    assertThat(module.getLinkedDiscordId("testPlayer")).isNull();
  }
}