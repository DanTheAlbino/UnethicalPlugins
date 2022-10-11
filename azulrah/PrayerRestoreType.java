package net.runelite.client.plugins.azulrah;

import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;

public enum PrayerRestoreType {
   PRAYER_POTION(143, 141, 139, 2434),
   SUPER_RESTORE(3030, 3028, 3026, 3024, 24605, 24603, 24601, 24598),
   RANGED(24644, 24641, 24638, 24635, 22470, 22467, 22464, 22461, 173, 171, 169, 2444),
   ANTIFIRE(2458, 2456, 2454, 2452, 22218, 22215, 22212, 22209),
   ANTIVENOM(5958, 5956, 5954, 5952, 12919, 12917, 12915, 12913),
   SANFEW_SERUM(10931, 10929, 10927, 10925),
   MAGIC(20724, 23754, 23751, 23748, 23745, 3046, 3044, 3042, 3040),
   COMBAT(12701, 12699, 12697, 12695, 9745, 9743, 9741, 9739);

   public int[] ItemIDs;

   private PrayerRestoreType(int... ids) {
      this.ItemIDs = ids;
   }

   public boolean containsId(int id) {
      return Arrays.stream(this.ItemIDs).anyMatch(x -> x == id);
   }

   public WidgetItem getItemFromInventory(Client client) {
      Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
      if (inventoryWidget == null) {
         return null;
      } else {
         for(WidgetItem item : inventoryWidget.getWidgetItems()) {
            if (Arrays.stream(this.ItemIDs).anyMatch(i -> i == item.getId())) {
               return item;
            }
         }

         return null;
      }
   }
}
