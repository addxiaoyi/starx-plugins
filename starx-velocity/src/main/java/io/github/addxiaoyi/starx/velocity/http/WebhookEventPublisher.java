package io.github.addxiaoyi.starx.velocity.http;

import io.github.addxiaoyi.starx.api.dto.WebhookPayload;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import java.util.Objects;

/** 订阅 {@link EventBus} 事件并转换为 Webhook 出站请求。 */
public final class WebhookEventPublisher {

  private static final String[] SUBSCRIBED_EVENTS = {
    EventTypes.PLAYER_LOGIN_SUCCESS,
    EventTypes.PLAYER_REGISTER,
    EventTypes.SKIN_UPDATED,
    EventTypes.SKIN_APPLIED,
    EventTypes.ADMIN_BAN_PLAYER,
    EventTypes.ADMIN_KICK_PLAYER
  };

  private final EventBus eventBus;
  private final WebhookClient webhookClient;

  public WebhookEventPublisher(EventBus eventBus, WebhookClient webhookClient) {
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.webhookClient = Objects.requireNonNull(webhookClient, "webhookClient");
  }

  /** 注册所有事件监听器。 */
  public void register() {
    for (String type : SUBSCRIBED_EVENTS) {
      eventBus.subscribe(type, this::onEvent);
    }
  }

  private void onEvent(StarxEvent event) {
    webhookClient.send(new WebhookPayload(event.type(), event.payload()));
  }
}
