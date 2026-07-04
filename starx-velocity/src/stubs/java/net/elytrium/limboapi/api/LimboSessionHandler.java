package net.elytrium.limboapi.api;

import net.elytrium.limboapi.api.player.LimboPlayer;

/** Stub for LimboAPI LimboSessionHandler. */
public interface LimboSessionHandler {

  default void onSpawn(Limbo server, LimboPlayer player) {}

  default void onChat(String chat) {}

  default void onDisconnect() {}
}
