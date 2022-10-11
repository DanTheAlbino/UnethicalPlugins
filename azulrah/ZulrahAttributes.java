package net.runelite.client.plugins.azulrah;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.runelite.api.Prayer;

public final class ZulrahAttributes {
   @Nullable
   public StandLocation standLocation;
   @Nullable
   public StandLocation stallLocation;
   @Nullable
   private final Prayer prayer;
   private final int phaseTicks;

   public ZulrahAttributes(@Nonnull StandLocation standLocation, @Nullable StandLocation stallLocation, @Nullable Prayer prayer, int phaseTicks) {
      super();
      this.standLocation = standLocation;
      this.stallLocation = stallLocation;
      this.prayer = prayer;
      this.phaseTicks = phaseTicks;
   }

   @Nonnull
   public StandLocation getStandLocation() {
      return this.standLocation;
   }

   @Nullable
   public StandLocation getStallLocation() {
      return this.stallLocation;
   }

   @Nullable
   public Prayer getPrayer() {
      return this.prayer;
   }

   public int getPhaseTicks() {
      return this.phaseTicks;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ZulrahAttributes)) {
         return false;
      } else {
         ZulrahAttributes other = (ZulrahAttributes)o;
         Object this$standLocation = this.getStandLocation();
         Object other$standLocation = other.getStandLocation();
         if (this$standLocation == null) {
            if (other$standLocation != null) {
               return false;
            }
         } else if (!this$standLocation.equals(other$standLocation)) {
            return false;
         }

         Object this$stallLocation = this.getStallLocation();
         Object other$stallLocation = other.getStallLocation();
         if (this$stallLocation == null) {
            if (other$stallLocation != null) {
               return false;
            }
         } else if (!this$stallLocation.equals(other$stallLocation)) {
            return false;
         }

         this$stallLocation = this.getPrayer();
         other$stallLocation = other.getPrayer();
         if (this$stallLocation == null) {
            if (other$stallLocation != null) {
               return false;
            }
         } else if (!this$stallLocation.equals(other$stallLocation)) {
            return false;
         }

         return this.getPhaseTicks() == other.getPhaseTicks();
      }
   }

   public int hashCode() {
      byte PRIME = 59;
      int result = 1;
      Object $standLocation = this.getStandLocation();
      result = result * PRIME + $standLocation.hashCode();
      Object $stallLocation = this.getStallLocation();
      result = result * PRIME + ($stallLocation == null ? 43 : $stallLocation.hashCode());
      Object $prayer = this.getPrayer();
      result = result * PRIME + ($prayer == null ? 43 : $prayer.hashCode());
      return result * PRIME + this.getPhaseTicks();
   }

   public String toString() {
      StandLocation standLocation = this.getStandLocation();
      return "ZulrahAttributes(standLocation="
         + standLocation
         + ", stallLocation="
         + this.getStallLocation()
         + ", prayer="
         + this.getPrayer()
         + ", phaseTicks="
         + this.getPhaseTicks()
         + ")";
   }
}
