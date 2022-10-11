package net.runelite.client.plugins.azulrah;

import java.util.Objects;
import net.runelite.api.Player;

class Victim {
   private final Player player;
   private final Victim.Type type;
   private int ticks;

   Victim(Player player, Victim.Type type) {
      super();
      this.player = player;
      this.type = type;
      this.ticks = type.getTicks();
   }

   void updateTicks() {
      if (this.ticks > 0) {
         --this.ticks;
      }

   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.player.getName(), this.type});
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Victim victim = (Victim)o;
         return Objects.equals(this.player.getName(), victim.player.getName()) && this.type == victim.type;
      } else {
         return false;
      }
   }

   public Player getPlayer() {
      return this.player;
   }

   public Victim.Type getType() {
      return this.type;
   }

   public int getTicks() {
      return this.ticks;
   }

   static enum Type {
      BURN(41),
      ACID(23),
      TELEPORT(10);

      private final int ticks;

      private Type(int ticks) {
         this.ticks = ticks;
      }

      int getTicks() {
         return this.ticks;
      }
   }
}
