package io.github.addxiaoyi.starx.common.scheduler;

import java.util.concurrent.TimeUnit;

/** 平台无关调度器接口。实现类负责提供异步、延迟与周期任务执行能力，不依赖具体平台（如 Velocity/Paper）。 */
public interface Scheduler {

  /**
   * 异步执行任务。
   *
   * @param task 待执行的任务
   */
  void runAsync(Runnable task);

  /**
   * 延迟执行任务。
   *
   * @param task 待执行的任务
   * @param delay 延迟时间
   * @param unit 时间单位
   */
  void runLater(Runnable task, long delay, TimeUnit unit);

  /**
   * 以固定周期重复执行任务。
   *
   * @param task 待执行的任务
   * @param delay 首次执行的延迟时间
   * @param period 周期
   * @param unit 时间单位
   */
  void runRepeating(Runnable task, long delay, long period, TimeUnit unit);
}
