package io.github.addxiaoyi.starx.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

  @TempDir Path tempDir;

  @Test
  void loadMissingFileReturnsDefaults() throws IOException {
    Path missing = tempDir.resolve("not-there.yml");
    StarxConfig config = ConfigLoader.load(missing);

    assertThat(config.httpApi().bind()).isEqualTo("0.0.0.0");
    assertThat(config.httpApi().port()).isEqualTo(8080);
    assertThat(config.httpApi().apiKey()).isEmpty();
    assertThat(config.database().type()).isEqualTo("sqlite");
    assertThat(config.database().poolMaxSize()).isEqualTo(10);
    assertThat(config.modules()).isEmpty();
  }

  @Test
  void loadFullConfigOverridesDefaults() throws IOException {
    String yaml =
        """
        http-api:
          bind: 127.0.0.1
          port: 9090
          api-key: test-key
        database:
          type: postgresql
          host: db.example.com
          port: 5432
          database: starx_prod
          username: starx
          password: secret
          url: jdbc:postgresql://db.example.com/starx_prod
          pool-max-size: 20
          connection-timeout-ms: 5000
        modules:
          auth:
            enabled: true
          skin:
            enabled: false
        """;
    Path file = tempDir.resolve("config.yml");
    Files.writeString(file, yaml);

    StarxConfig config = ConfigLoader.load(file);

    assertThat(config.httpApi().bind()).isEqualTo("127.0.0.1");
    assertThat(config.httpApi().port()).isEqualTo(9090);
    assertThat(config.httpApi().apiKey()).isEqualTo("test-key");
    assertThat(config.database().type()).isEqualTo("postgresql");
    assertThat(config.database().host()).isEqualTo("db.example.com");
    assertThat(config.database().port()).isEqualTo(5432);
    assertThat(config.database().database()).isEqualTo("starx_prod");
    assertThat(config.database().username()).isEqualTo("starx");
    assertThat(config.database().password()).isEqualTo("secret");
    assertThat(config.database().url()).isEqualTo("jdbc:postgresql://db.example.com/starx_prod");
    assertThat(config.database().poolMaxSize()).isEqualTo(20);
    assertThat(config.database().connectionTimeoutMs()).isEqualTo(5000);
    assertThat(config.modules()).containsEntry("auth", new ModuleConfig(true));
    assertThat(config.modules()).containsEntry("skin", new ModuleConfig(false));
  }

  @Test
  void defaultsProvidesExpectedValues() {
    StarxConfig config = StarxConfig.defaults();

    assertThat(config.httpApi().bind()).isEqualTo("0.0.0.0");
    assertThat(config.httpApi().port()).isEqualTo(8080);
    assertThat(config.database().type()).isEqualTo("sqlite");
    assertThat(config.modules()).isEmpty();
  }

  @Test
  void mergeOverlaysHttpApiAndModules() {
    StarxConfig base = StarxConfig.defaults();
    StarxConfig overlay =
        new StarxConfig(
            new HttpApiConfig("127.0.0.1", 9090, "key"),
            base.database(),
            base.uniauth(),
            java.util.Map.of("auth", new ModuleConfig(true)));

    StarxConfig merged = base.merge(overlay);

    assertThat(merged.httpApi().bind()).isEqualTo("127.0.0.1");
    assertThat(merged.httpApi().port()).isEqualTo(9090);
    assertThat(merged.httpApi().apiKey()).isEqualTo("key");
    assertThat(merged.modules()).containsEntry("auth", new ModuleConfig(true));
  }
}
