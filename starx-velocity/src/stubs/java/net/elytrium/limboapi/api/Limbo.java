package net.elytrium.limboapi.api;

import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.player.GameMode;

/** Stub for LimboAPI Limbo. */
public interface Limbo {

  Limbo setName(String name);

  Limbo setGameMode(GameMode gameMode);

  void spawnPlayer(Player player, LimboSessionHandler handler);

  void dispose();
}
