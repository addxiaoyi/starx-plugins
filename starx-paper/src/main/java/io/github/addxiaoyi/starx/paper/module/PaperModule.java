package io.github.addxiaoyi.starx.paper.module;

/** StarX Paper 后端模块契约。 */
public interface PaperModule {

  String getName();

  boolean isEnabled();

  void onEnable();

  default void onDisable() {}
}
