package io.github.addxiaoyi.starx.paper.module;

import io.github.addxiaoyi.starx.api.messaging.PluginMessage;

/** StarX Paper 后端模块契约。 */
public interface PaperModule {

  String getName();

  boolean isEnabled();

  void onEnable();

  default void onDisable() {}

  default void onPluginMessage(PluginMessage message) {}
}
