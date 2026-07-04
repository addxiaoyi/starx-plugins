package io.github.addxiaoyi.starx.velocity.event;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** 基于内存的 Velocity 端事件总线实现。 */
public final class VelocityEventBus implements EventBus {

  private final Map<String, List<Consumer<StarxEvent>>> subscribers = new ConcurrentHashMap<>();

  @Override
  public void publish(StarxEvent event) {
    List<Consumer<StarxEvent>> listeners = subscribers.getOrDefault(event.type(), List.of());
    for (Consumer<StarxEvent> listener : listeners) {
      listener.accept(event);
    }
  }

  @Override
  public void subscribe(String type, Consumer<StarxEvent> subscriber) {
    subscribers.computeIfAbsent(type, k -> new ArrayList<>()).add(subscriber);
  }
}
