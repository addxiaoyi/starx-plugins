package io.github.addxiaoyi.starx.velocity.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.api.dto.WebhookPayload;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import io.github.addxiaoyi.starx.velocity.security.WebhookSigner;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookClientTest {

  @Mock WebhookSigner signer;
  @Mock WebhookHttpTransport transport;
  @Captor ArgumentCaptor<String> bodyCaptor;
  @Captor ArgumentCaptor<Map<String, String>> headersCaptor;

  private WebhookClient client;

  @BeforeEach
  void setUp() {
    StarxConfig.WebhookConfig config =
        new StarxConfig.WebhookConfig("https://example.com/hook", "secret");
    client = new WebhookClient(config, signer, transport);
  }

  @Test
  void shouldSkipSendingWhenWebhookUrlNotConfigured() throws Exception {
    WebhookClient unconfiguredClient =
        new WebhookClient(new StarxConfig.WebhookConfig("", ""), signer, transport);

    unconfiguredClient.send(new WebhookPayload("test", Map.of())).get();

    verify(transport, never()).post(anyString(), anyString(), anyMap());
    verify(signer, never()).sign(anyString());
  }

  @Test
  void shouldSendPayloadWithSignatureHeader() throws Exception {
    when(signer.sign(anyString())).thenReturn("signature-value");
    when(transport.post(anyString(), anyString(), anyMap()))
        .thenReturn(CompletableFuture.completedFuture(null));

    WebhookPayload payload =
        new WebhookPayload("player:login:success", Map.of("username", "alice"));
    client.send(payload).get();

    verify(transport).post(anyString(), bodyCaptor.capture(), headersCaptor.capture());

    String body = bodyCaptor.getValue();
    Map<String, String> headers = headersCaptor.getValue();

    assertThat(body).contains("player:login:success").contains("alice");
    assertThat(headers).containsEntry("Content-Type", "application/json");
    assertThat(headers).containsEntry(WebhookClient.SIGNATURE_HEADER, "signature-value");
    verify(signer).sign(body);
  }

  @Test
  void shouldPropagateDeliveryFailure() {
    when(signer.sign(anyString())).thenReturn("signature-value");
    when(transport.post(anyString(), anyString(), anyMap()))
        .thenReturn(CompletableFuture.failedFuture(new WebhookDeliveryException("boom")));

    WebhookPayload payload = new WebhookPayload("test", Map.of());

    assertThatThrownBy(() -> client.send(payload).get())
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(WebhookDeliveryException.class);
  }
}
