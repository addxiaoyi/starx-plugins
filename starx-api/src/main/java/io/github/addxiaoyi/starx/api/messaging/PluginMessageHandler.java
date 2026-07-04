package io.github.addxiaoyi.starx.api.messaging;

/** Plugin Messaging 消息处理器契约。Velocity 与 Paper 分别实现。 */
public interface PluginMessageHandler {

  void handle(PluginMessage message);
}
