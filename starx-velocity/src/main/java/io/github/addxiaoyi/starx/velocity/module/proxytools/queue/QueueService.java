package io.github.addxiaoyi.starx.velocity.module.proxytools.queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 按 FIFO 管理每个子服排队玩家的服务。 */
public final class QueueService {

  private final Map<String, Deque<Player>> queues = new ConcurrentHashMap<>();

  public void enqueue(RegisteredServer server, Player player) {
    queues.computeIfAbsent(serverName(server), k -> new ArrayDeque<>()).addLast(player);
  }

  public Player dequeue(RegisteredServer server) {
    Deque<Player> queue = queues.get(serverName(server));
    return queue == null ? null : queue.pollFirst();
  }

  public int size(RegisteredServer server) {
    Deque<Player> queue = queues.get(serverName(server));
    return queue == null ? 0 : queue.size();
  }

  public boolean removeFromQueue(RegisteredServer server, Player player) {
    Deque<Player> queue = queues.get(serverName(server));
    return queue != null && queue.remove(player);
  }

  public int processQueues(PlayerConnector connector) {
    int connected = 0;
    for (Map.Entry<String, Deque<Player>> entry : queues.entrySet()) {
      Deque<Player> queue = entry.getValue();
      while (!queue.isEmpty()) {
        Player player = queue.peekFirst();
        if (connector.connect(player, entry.getKey())) {
          queue.pollFirst();
          connected++;
        } else {
          break;
        }
      }
    }
    return connected;
  }

  private static String serverName(RegisteredServer server) {
    return server.getServerInfo().getName();
  }

  public interface PlayerConnector {
    boolean connect(Player player, String serverName);
  }
}
