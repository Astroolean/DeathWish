package com.astroolean.deathwish;

import org.bukkit.NamespacedKey;

public class HardcoreUtil {
   public static NamespacedKey KEY_BOUND_TO;

   public static void init(DeathWish plugin) {
      KEY_BOUND_TO = new NamespacedKey(plugin, "bound_to");
   }
}
