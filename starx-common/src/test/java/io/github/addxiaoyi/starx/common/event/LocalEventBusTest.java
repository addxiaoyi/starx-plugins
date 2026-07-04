package io.github.addxiaoyi.starx.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.addxiaoyi.starx.api.event.StarxEvent;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LocalEventBusTest {

  @Test
  void subscriberReceivesPublishedEvent() {
    LocalEventBus bus = new LocalEventBus();
    AtomicReference<String> received = new AtomicReference<>();

    bus.subscribe("chat", event -> received.set((String) event.payload().get("message")));
    bus.publish("chat", Map.of("message", "hello"));

    assertThat(received.get()).isEqualTo("hello");
  }

  @Test
  void multipleSubscribersAllReceiveEvent() {
    LocalEventBus bus = new LocalEventBus();
    AtomicReference<String> first = new AtomicReference<>();
    AtomicReference<String> second = new AtomicReference<>();

    bus.subscribe("broadcast", event -> first.set(event.type()));
    bus.subscribe("broadcast", event -> second.set(event.type()));
    bus.publish("broadcast");

    assertThat(first.get()).isEqualTo("broadcast");
    assertThat(second.get()).isEqualTo("broadcast");
  }

  @Test
  void publishWithoutSubscriberDoesNothing() {
    LocalEventBus bus = new LocalEventBus();

    bus.publish(new StarxEvent("silent", Map.of()));

    assertThat(true).isTrue();
  }
}
