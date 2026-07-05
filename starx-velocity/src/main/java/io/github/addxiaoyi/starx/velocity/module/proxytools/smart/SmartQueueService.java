package io.github.addxiaoyi.starx.velocity.module.proxytools.smart;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能排队服务 — 基于优先级评分（VIP + 活跃度）的排队队列。
 *
 * <p>与原有 QueueService（FIFO）不同，SmartQueueService 使用优先级队列：
 *
 * <ul>
 *   <li>VIP 玩家获得 +500 基础分加成
 *   <li>在线时长越长的玩家获得 +0~100 活跃度加成
 *   <li>分数越高，排队位置越靠前
 * </ul>
 *
 * <p>线程安全：使用 ConcurrentHashMap 存储队列，PriorityQueue 在 processQueues 中单线程操作。
 */
public final class SmartQueueService {

  private final Map<String, PriorityQueue<SmartQueueEntry>> queues = new ConcurrentHashMap<>();
  private final Map<String, Long> joinTimestamps = new ConcurrentHashMap<>();

  /** 优先级分数比较器：分数高的排在前面 */
  private static final Comparator<SmartQueueEntry> PRIORITY_COMPARATOR =
      Comparator.comparingLong(SmartQueueEntry::score)
          .reversed()
          .thenComparingLong(SmartQueueEntry::enqueueTimeMs);

  public void enqueue(RegisteredServer server, Player player, int baseScore) {
    String serverName = server.getServerInfo().getName();
    long timestamp = System.currentTimeMillis();
    queues
        .computeIfAbsent(serverName, k -> new PriorityQueue<>(PRIORITY_COMPARATOR))
        .add(new SmartQueueEntry(player, baseScore + computeActivityScore(player), timestamp));
  }

  public Player dequeue(RegisteredServer server) {
    PriorityQueue<SmartQueueEntry> queue = queues.get(server.getServerInfo().getName());
    if (queue == null || queue.isEmpty()) {
      return null;
    }
    SmartQueueEntry entry = queue.poll();
    return entry != null ? entry.player : null;
  }

  public int size(RegisteredServer server) {
    PriorityQueue<SmartQueueEntry> queue = queues.get(server.getServerInfo().getName());
    return queue == null ? 0 : queue.size();
  }

  public boolean removeFromQueue(RegisteredServer server, Player player) {
    PriorityQueue<SmartQueueEntry> queue = queues.get(server.getServerInfo().getName());
    if (queue == null) {
      return false;
    }
    return queue.removeIf(e -> e.player.getUniqueId().equals(player.getUniqueId()));
  }

  /** 记录玩家加入时间，用于活跃度计算 */
  public void recordJoin(Player player) {
    joinTimestamps.putIfAbsent(player.getUniqueId().toString(), System.currentTimeMillis());
  }

  /** 移除玩家活跃度记录 */
  public void recordQuit(Player player) {
    joinTimestamps.remove(player.getUniqueId().toString());
  }

  /**
   * 处理所有队列并尝试放行玩家。
   *
   * @param connector 连接器（尝试将玩家连接到目标服务器）
   * @param maxRelease 本次最多放行多少玩家（由动态放行速率决定）
   * @return 实际放行的玩家数量
   */
  public int processQueues(PlayerConnector connector, int maxRelease) {
    int connected = 0;
    for (Map.Entry<String, PriorityQueue<SmartQueueEntry>> entry : queues.entrySet()) {
      if (connected >= maxRelease) {
        break;
      }
      PriorityQueue<SmartQueueEntry> queue = entry.getValue();
      while (!queue.isEmpty() && connected < maxRelease) {
        SmartQueueEntry queued = queue.peek();
        if (queued == null) {
          break;
        }
        if (connector.connect(queued.player, entry.getKey())) {
          queue.poll();
          connected++;
        } else {
          break;
        }
      }
    }
    return connected;
  }

  /** 计算活跃度分数（0-100），基于在线时长 */
  private int computeActivityScore(Player player) {
    Long joinTime = joinTimestamps.get(player.getUniqueId().toString());
    if (joinTime == null) {
      return 0;
    }
    long onlineMs = System.currentTimeMillis() - joinTime;
    long minutes = onlineMs / 60_000;
    return (int) Math.min(100, minutes);
  }

  public interface PlayerConnector {
    boolean connect(Player player, String serverName);
  }

  /** 排队条目：包含玩家、优先级分数和入队时间 */
  static final class SmartQueueEntry {
    final Player player;
    final long score;
    final long enqueueTimeMs;

    SmartQueueEntry(Player player, long score, long enqueueTimeMs) {
      this.player = player;
      this.score = score;
      this.enqueueTimeMs = enqueueTimeMs;
    }

    long score() {
      return score;
    }

    long enqueueTimeMs() {
      return enqueueTimeMs;
    }
  }
}
