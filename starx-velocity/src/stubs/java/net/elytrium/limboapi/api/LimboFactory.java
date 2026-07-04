package net.elytrium.limboapi.api;

import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;

/** Stub for LimboAPI LimboFactory. */
public interface LimboFactory {

  VirtualWorld createVirtualWorld(
      Dimension dimension, double posX, double posY, double posZ, float yaw, float pitch);

  Limbo createLimbo(VirtualWorld world);

  void passLoginLimbo(Player player);
}
