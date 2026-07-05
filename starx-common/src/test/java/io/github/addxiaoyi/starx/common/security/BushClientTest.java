package io.github.addxiaoyi.starx.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BushClientTest {

  @Test
  void shouldConstructWithDefaultUrl() {
    BushClient client = new BushClient();
    assertThat(client).isNotNull();
  }

  @Test
  void shouldConstructWithCustomUrl() {
    BushClient client = new BushClient("https://custom.example.com/v1/");
    assertThat(client).isNotNull();
  }

  @Test
  void shouldReturnFalseForLocalhost() {
    BushClient client = new BushClient();
    assertThat(client.isIpBlacklisted("127.0.0.1")).isFalse();
  }

  @Test
  void shouldReturnFalseForPrivateIp() {
    BushClient client = new BushClient();
    assertThat(client.isIpBlacklisted("192.168.1.1")).isFalse();
  }

  @Test
  void shouldReturnFalseForInvalidUrl() {
    BushClient client = new BushClient("https://invalid.example.com/");
    assertThat(client.isIpBlacklisted("10.0.0.1")).isFalse();
  }
}
