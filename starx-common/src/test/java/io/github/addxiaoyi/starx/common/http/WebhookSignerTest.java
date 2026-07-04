package io.github.addxiaoyi.starx.common.http;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.api.dto.WebhookPayload;
import io.github.addxiaoyi.starx.common.crypto.HmacSigner;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebhookSignerTest {

  @Test
  void signProducesTimestampAndSignatureHeaders() {
    Instant timestamp = Instant.ofEpochSecond(1_700_000_000L);
    WebhookPayload payload =
        new WebhookPayload(UUID.randomUUID(), "login", timestamp, Map.of("player", "alice"));
    String secret = "webhook-secret";

    Map<String, String> headers = WebhookSigner.sign(payload, secret);

    assertThat(headers).containsOnlyKeys("X-VLA-Timestamp", "X-VLA-Signature");
    assertThat(headers.get("X-VLA-Timestamp")).isEqualTo("1700000000");
    String expected = HmacSigner.sign(secret, "1700000000", WebhookSigner.toJson(payload));
    assertThat(headers.get("X-VLA-Signature")).isEqualToIgnoringCase(expected);
  }

  @Test
  void signatureChangesWhenPayloadChanges() {
    Instant timestamp = Instant.ofEpochSecond(1_700_000_000L);
    WebhookPayload payload1 =
        new WebhookPayload(UUID.randomUUID(), "login", timestamp, Map.of("player", "alice"));
    WebhookPayload payload2 =
        new WebhookPayload(UUID.randomUUID(), "login", timestamp, Map.of("player", "bob"));
    String secret = "webhook-secret";

    Map<String, String> headers1 = WebhookSigner.sign(payload1, secret);
    Map<String, String> headers2 = WebhookSigner.sign(payload2, secret);

    assertThat(headers1.get("X-VLA-Signature"))
        .isNotEqualToIgnoringCase(headers2.get("X-VLA-Signature"));
  }
}
