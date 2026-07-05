package io.github.addxiaoyi.starx.velocity.http;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.addxiaoyi.starx.api.dto.WebhookPayload;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WebhookEventPublisherTest {

  private final EventBus eventBus = new VelocityEventBus();
  private final WebhookClient webhookClient = mock(WebhookClient.class);

  @Test
  void shouldSendLoginSuccessWebhook() throws Exception {
    new WebhookEventPublisher(eventBus, webhookClient).register();
    eventBus.publish(EventTypes.PLAYER_LOGIN_SUCCESS, Map.of("username", "alice"));
    Thread.sleep(200);

    verify(webhookClient)
        .send(
            argThat(
                (WebhookPayload payload) ->
                    payload.eventType().equals(EventTypes.PLAYER_LOGIN_SUCCESS)
                        && "alice".equals(payload.data().get("username"))));
  }

  @Test
  void shouldSendRegisterWebhook() throws Exception {
    new WebhookEventPublisher(eventBus, webhookClient).register();
    eventBus.publish(EventTypes.PLAYER_REGISTER, Map.of("username", "bob"));
    Thread.sleep(200);

    verify(webhookClient)
        .send(
            argThat(
                (WebhookPayload payload) ->
                    payload.eventType().equals(EventTypes.PLAYER_REGISTER)
                        && "bob".equals(payload.data().get("username"))));
  }

  @Test
  void shouldSendSkinUpdatedWebhook() throws Exception {
    new WebhookEventPublisher(eventBus, webhookClient).register();
    eventBus.publish(
        EventTypes.SKIN_UPDATED,
        Map.of("username", "charlie", "skinUrl", "https://example.com/skin.png"));
    Thread.sleep(200);

    verify(webhookClient)
        .send(
            argThat(
                (WebhookPayload payload) ->
                    payload.eventType().equals(EventTypes.SKIN_UPDATED)
                        && "charlie".equals(payload.data().get("username"))));
  }

  @Test
  void shouldSendSkinAppliedWebhook() throws Exception {
    new WebhookEventPublisher(eventBus, webhookClient).register();
    eventBus.publish(EventTypes.SKIN_APPLIED, Map.of("username", "dave"));
    Thread.sleep(200);

    verify(webhookClient)
        .send(
            argThat(
                (WebhookPayload payload) ->
                    payload.eventType().equals(EventTypes.SKIN_APPLIED)
                        && "dave".equals(payload.data().get("username"))));
  }

  @Test
  void shouldSendBanWebhook() throws Exception {
    new WebhookEventPublisher(eventBus, webhookClient).register();
    eventBus.publish(EventTypes.ADMIN_BAN_PLAYER, Map.of("username", "eve", "reason", "cheating"));
    Thread.sleep(200);

    verify(webhookClient)
        .send(
            argThat(
                (WebhookPayload payload) ->
                    payload.eventType().equals(EventTypes.ADMIN_BAN_PLAYER)
                        && "eve".equals(payload.data().get("username"))));
  }

  @Test
  void shouldSendKickWebhook() throws Exception {
    new WebhookEventPublisher(eventBus, webhookClient).register();
    eventBus.publish(EventTypes.ADMIN_KICK_PLAYER, Map.of("username", "frank", "reason", "afk"));
    Thread.sleep(200);

    verify(webhookClient)
        .send(
            argThat(
                (WebhookPayload payload) ->
                    payload.eventType().equals(EventTypes.ADMIN_KICK_PLAYER)
                        && "frank".equals(payload.data().get("username"))));
  }
}
