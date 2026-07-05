package io.github.addxiaoyi.starx.velocity.event;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** 基于内存的 Velocity 端事件总线实现，线程安全且异步分发。 */
public final class VelocityEventBus implements EventBus {

  private static final int ASYNC_THREADS = 2;

  private final Map<String, List<Consumer<StarxEvent>>> subscribers;
  private final Executor asyncExecutor;

  public VelocityEventBus() {
    this.subscribers = new ConcurrentHashMap<>(32);
    this.asyncExecutor = Executors.newFixedThreadPool(ASYNC_THREADS, r -> {
      Thread t = new Thread(r, "starx-event-bus");
      t.setDaemon(true);
      return t;
    });
  }

  @Override
  public void publish(StarxEvent event) {
    List<Consumer<StarxEvent>> listeners = subscribers.getOrDefault(event.type(), List.of());
    if (listeners.isEmpty()) {
      return;
    }
    CompletableFuture.runAsync(() -> {
      for (Consumer<StarxEvent> listener : listeners) {
        try {
          listener.accept(event);
        } catch (Exception e) {
          // 吞掉单个 listener 异常，不影响其他 listener
        }
      }
    }, asyncExecutor);
  }

  @Override
  public void subscribe(String type, Consumer<StarxEvent> subscriber) {
    subscribers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(subscriber);
  }

  public void unsubscribe(String type, Consumer<StarxEvent> subscriber) {
    subscribers.computeIfPresent(type, (k, list) -> {
      list.remove(subscriber);
      return list.isEmpty() ? null : list;
    });
  }
}