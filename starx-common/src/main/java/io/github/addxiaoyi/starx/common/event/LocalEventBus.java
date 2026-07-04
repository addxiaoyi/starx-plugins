package io.github.addxiaoyi.starx.common.event;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** 基于 {@link ConcurrentHashMap} 与 {@link CopyOnWriteArrayList} 的本地事件总线实现。 */
public class LocalEventBus implements EventBus {

  private final Map<String, List<Consumer<StarxEvent>>> listeners = new ConcurrentHashMap<>();

  @Override
  public void subscribe(String type, Consumer<StarxEvent> subscriber) {
    listeners.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(subscriber);
  }

  @Override
  public void publish(StarxEvent event) {
    List<Consumer<StarxEvent>> subscribers = listeners.get(event.type());
    if (subscribers == null) {
      return;
    }
    for (Consumer<StarxEvent> subscriber : subscribers) {
      subscriber.accept(event);
    }
  }
}
