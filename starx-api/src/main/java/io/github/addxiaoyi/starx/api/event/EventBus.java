package io.github.addxiaoyi.starx.api.event;

import java.util.function.Consumer;

/** 平台无关事件总线契约。代理端与后端使用各自实现，但共享事件类型常量。 */
public interface EventBus {

  void publish(StarxEvent event);

  default void publish(String type) {
    publish(new StarxEvent(type, java.util.Map.of()));
  }

  default void publish(String type, java.util.Map<String, Object> payload) {
    publish(new StarxEvent(type, payload));
  }

  void subscribe(String type, Consumer<StarxEvent> subscriber);
}
