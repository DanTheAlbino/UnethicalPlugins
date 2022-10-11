package net.runelite.client.plugins.avorkath;

import com.google.inject.Provides;
import java.awt.Rectangle;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.TileItem;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.ProjectileSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.autils.AUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.Extension;

@Extension
@PluginDependency(AUtils.class)
@PluginDescriptor(
   name = "AVorkath",
   description = "Anarchise' Auto Vorkath.",
   tags = {"vorkath", "anarchise", "aplugins"},
   enabledByDefault = false
)
public class AVorkathPlugin extends Plugin {
   @Inject
   private Client client;
   @Inject
   private AVorkathConfig configvk;
   @Inject
   private ClientThread clientThread;
   @Inject
   private AUtils utils;
   @Inject
   private KeyManager keyManager;
   @Inject
   private InfoBoxManager infoBoxManager;
   @Inject
   private OverlayManager overlayManager;
   @Inject
   private AVorkathOverlay overlayvk;
   private Rectangle bounds;
   private int timeout;
   private NPC vorkath;
   private List<WorldPoint> acidSpots = new ArrayList();
   boolean FirstWalk = true;
   private List<WorldPoint> acidFreePath = new ArrayList();
   private int lastAcidSpotsSize = 0;
   private final Set<Integer> DIAMOND_SET = Set.of(21946, 9243);
   WorldArea EDGEVILLE_BANK = new WorldArea(new WorldPoint(3082, 3485, 0), new WorldPoint(3100, 3502, 0));
   WorldArea RELEKKA_POH = new WorldArea(new WorldPoint(2664, 3625, 0), new WorldPoint(2678, 3638, 0));
   WorldArea RELEKKA_TOWN = new WorldArea(new WorldPoint(2635, 3668, 0), new WorldPoint(2652, 3684, 0));
   WorldArea VORKATH = new WorldArea(new WorldPoint(2262, 4032, 0), new WorldPoint(2286, 4053, 0));
   WorldArea VORKATH2 = new WorldArea(new WorldPoint(2259, 4053, 0), new WorldPoint(2290, 4083, 0));
   private final Set<Integer> RUBY_SET = Set.of(21944, 9242);
   AVorkathState state;
   LocalPoint beforeLoc;
   Player player;
   MenuEntry targetMenu;
   LocalPoint dodgeRight;
   LocalPoint dodgeLeft;
   Instant botTimer;
   private boolean inFight;
   private Prayer prayerToClick;
   private Random r = new Random();
   List<String> lootableItems = new ArrayList();
   List<TileItem> loot = new ArrayList();
   private Prayer prayer;
   boolean startTeaks = false;
   boolean killedvorkath = false;
   boolean noBomb = true;
   boolean noBomb2 = true;
   private NPC zulrahNpc = null;
   private int stage = 0;
   private int phaseTicks = -1;
   private int attackTicks = -1;
   private int acidFreePathLength = 3;
   private int totalTicks = 0;
   boolean banked = false;
   String[] values;
   @Inject
   ConfigManager configManager;
   int[] ItemIDs;
   private boolean attacked = false;
   private int AcidTickCount = 0;
   private static final List<Integer> regions = Arrays.asList(7513, 7514, 7769, 7770);

   @Provides
   AVorkathConfig provideConfig(ConfigManager configManager) {
      return (AVorkathConfig)configManager.getConfig(AVorkathConfig.class);
   }

   public AVorkathPlugin() {
      super();
      this.inFight = false;
   }

   private void reset() {
      this.loot.clear();
      this.lootableItems.clear();
      this.values = this.configvk.lootNames().toLowerCase().split("\\s*,\\s*");
      if (!this.configvk.lootNames().isBlank()) {
         this.lootableItems.addAll(Arrays.asList(this.values));
      }

      this.overlayManager.remove(this.overlayvk);
      this.startTeaks = false;
      this.zulrahNpc = null;
      this.stage = 0;
      this.phaseTicks = -1;
      this.state = null;
      this.killedvorkath = false;
      this.timeout = 0;
      this.attackTicks = -1;
      this.totalTicks = 0;
      this.inFight = false;
      this.prayerToClick = null;
      this.noBomb = true;
      this.noBomb2 = true;
      this.dodgeRight = null;
      this.dodgeLeft = null;
      this.banked = false;
      this.botTimer = null;
   }

   @Subscribe
   private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
      if (configButtonClicked.getGroup().equalsIgnoreCase("avork")) {
         if (configButtonClicked.getKey().equals("startButton")) {
            if (!this.startTeaks) {
               this.startTeaks = true;
               this.overlayManager.add(this.overlayvk);
               this.loot.clear();
               this.lootableItems.clear();
               this.values = this.configvk.lootNames().toLowerCase().split("\\s*,\\s*");
               if (!this.configvk.lootNames().isBlank()) {
                  this.lootableItems.addAll(Arrays.asList(this.values));
               }

               this.noBomb = true;
               this.noBomb2 = true;
               this.banked = false;
               this.botTimer = Instant.now();
            } else {
               this.reset();
            }
         }

      }
   }

   private long sleepDelay() {
      return this.utils
         .randomDelay(
            this.configvk.sleepWeightedDistribution(),
            this.configvk.sleepMin(),
            this.configvk.sleepMax(),
            this.configvk.sleepDeviation(),
            this.configvk.sleepTarget()
         );
   }

   private int tickDelay() {
      return (int)this.utils
         .randomDelay(
            this.configvk.tickDelayWeightedDistribution(),
            this.configvk.tickDelayMin(),
            this.configvk.tickDelayMax(),
            this.configvk.tickDelayDeviation(),
            this.configvk.tickDelayTarget()
         );
   }

   protected void startUp() throws Exception {
      this.reset();
   }

   protected void shutDown() throws Exception {
      this.reset();
   }

   private void openBank() {
      GameObject bankTarget = this.utils.findNearestBankNoDepositBoxes();
      if (bankTarget != null) {
         this.clientThread
            .invoke(
               () -> this.client
                     .invokeMenuAction(
                        "",
                        "",
                        bankTarget.getId(),
                        this.utils.getBankMenuOpcode(bankTarget.getId()),
                        bankTarget.getSceneMinLocation().getX(),
                        bankTarget.getSceneMinLocation().getY()
                     )
            );
      }

   }

   private void lootItem(List<TileItem> itemList) {
      TileItem lootItem = this.getNearestTileItem(itemList);
      if (lootItem != null) {
         this.clientThread
            .invoke(
               () -> this.client
                     .invokeMenuAction(
                        "",
                        "",
                        lootItem.getId(),
                        MenuAction.GROUND_ITEM_THIRD_OPTION.getId(),
                        lootItem.getTile().getSceneLocation().getX(),
                        lootItem.getTile().getSceneLocation().getY()
                     )
            );
      }

   }

   private TileItem getNearestTileItem(List<TileItem> tileItems) {
      TileItem closestTileItem = (TileItem)tileItems.get(0);
      int closestDistance = closestTileItem.getTile().getWorldLocation().distanceTo(this.player.getWorldLocation());

      for(TileItem tileItem : tileItems) {
         int currentDistance = tileItem.getTile().getWorldLocation().distanceTo(this.player.getWorldLocation());
         if (currentDistance < closestDistance) {
            closestTileItem = tileItem;
            closestDistance = currentDistance;
         }
      }

      return closestTileItem;
   }

   public WidgetItem getRestoreItem() {
      WidgetItem item = PrayerRestoreType.PRAYER_POTION.getItemFromInventory(this.client);
      if (item != null) {
         return item;
      } else {
         item = PrayerRestoreType.SANFEW_SERUM.getItemFromInventory(this.client);
         return item != null ? item : PrayerRestoreType.SUPER_RESTORE.getItemFromInventory(this.client);
      }
   }

   public WidgetItem GetFoodItem() {
      WidgetItem item = this.utils.getInventoryWidgetItem(this.configvk.foodID());
      return item != null ? item : item;
   }

   public WidgetItem GetRangedItem() {
      WidgetItem item = PrayerRestoreType.RANGED.getItemFromInventory(this.client);
      return item != null ? item : item;
   }

   public WidgetItem GetCombatItem() {
      WidgetItem item = PrayerRestoreType.COMBAT.getItemFromInventory(this.client);
      return item != null ? item : item;
   }

   public WidgetItem GetAntifireItem() {
      WidgetItem item = PrayerRestoreType.ANTIFIRE.getItemFromInventory(this.client);
      return item != null ? item : item;
   }

   public WidgetItem GetAntiVenomItem() {
      WidgetItem item = PrayerRestoreType.ANTIVENOM.getItemFromInventory(this.client);
      return item != null ? item : item;
   }

   public AVorkathState getState() {
      if (this.timeout > 0) {
         return AVorkathState.TIMEOUT;
      } else {
         return this.utils.isBankOpen() ? this.getBankState() : this.getStates();
      }
   }

   @Nullable
   NPC vorkathAwake() {
      return this.utils.findNearestNpc(new int[]{8059});
   }

   private boolean isInVorkath() {
      return ArrayUtils.contains(this.client.getMapRegions(), 9023);
   }

   private AVorkathState getStates() {
      if (!this.noBomb2 && !this.isInVorkath()) {
         this.noBomb2 = true;
         this.attacked = false;
      }

      if (!this.noBomb && !this.isInVorkath()) {
         this.noBomb = true;
         this.attacked = false;
      }

      if (this.utils.findNearestNpc(new int[]{8059}) != null && this.isInVorkath()) {
         this.acidFreePath.clear();
         this.acidSpots.clear();
         this.noBomb = true;
         this.noBomb2 = true;
         this.attacked = false;
      }

      if (this.banked && this.client.getLocalPlayer().getWorldArea().intersectsWith(this.EDGEVILLE_BANK) && !this.utils.isBankOpen()) {
         this.banked = false;
      }

      if (isInPOH(this.client)
         && this.utils.inventoryContains(this.RUBY_SET)
         && !this.utils.isItemEquipped(this.RUBY_SET)
         && this.configvk.useRanged()
         && !this.configvk.useBlowpipe()) {
         return AVorkathState.EQUIP_RUBIES;
      } else if (!this.isInVorkath()
         && this.utils.inventoryContains(this.RUBY_SET)
         && !this.utils.isItemEquipped(this.RUBY_SET)
         && this.configvk.useRanged()
         && !this.configvk.useBlowpipe()) {
         return AVorkathState.EQUIP_RUBIES;
      } else if (this.isInVorkath()
         && this.calculateHealth(this.vorkath, 750) > 265
         && this.calculateHealth(this.vorkath, 750) <= 750
         && this.utils.inventoryContains(this.RUBY_SET)
         && !this.utils.isItemEquipped(this.RUBY_SET)
         && this.acidSpots.isEmpty()
         && this.configvk.useRanged()
         && !this.configvk.useBlowpipe()) {
         return AVorkathState.EQUIP_RUBIES;
      } else if (this.isInVorkath()
         && this.calculateHealth(this.vorkath, 750) < 265
         && this.utils.inventoryContains(this.DIAMOND_SET)
         && !this.utils.isItemEquipped(this.DIAMOND_SET)
         && this.acidSpots.isEmpty()
         && this.configvk.useRanged()
         && !this.configvk.useBlowpipe()) {
         return AVorkathState.EQUIP_DIAMONDS;
      } else if (this.client.getVar(Varbits.QUICK_PRAYER) == 1 && !this.isInVorkath()) {
         return AVorkathState.DEACTIVATE_PRAY;
      } else if (this.player.getWorldArea().intersectsWith(this.EDGEVILLE_BANK) && this.banked) {
         return AVorkathState.WALK_FIRST;
      } else if (this.player.getWorldArea().intersectsWith(this.EDGEVILLE_BANK) && !this.banked) {
         return AVorkathState.FIND_BANK;
      } else {
         if (this.player.getLocalLocation() == new LocalPoint(5824, 7872) && this.isInVorkath()) {
            this.utils.walk(new LocalPoint(6080, 7872));
         }

         if (isInPOH(this.client)
            && this.client.getBoostedSkillLevel(Skill.PRAYER) < this.client.getRealSkillLevel(Skill.PRAYER)
            && this.configvk.usePOHpool()) {
            return AVorkathState.DRINK_POOL;
         } else {
            if (this.utils.isItemEquipped(Collections.singleton(this.configvk.specWeapon()))
               && this.client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) < this.configvk.specThreshold() * 10) {
               WidgetItem weapon = this.utils.getInventoryWidgetItem(this.configvk.normalWeapon());
               WidgetItem offhand = this.utils.getInventoryWidgetItem(this.configvk.normalOffhand());
               if (weapon != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction("", "", weapon.getId(), MenuAction.ITEM_SECOND_OPTION.getId(), weapon.getIndex(), WidgetInfo.INVENTORY.getId())
                     );
               }

               if (offhand != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "", "", offhand.getId(), MenuAction.ITEM_SECOND_OPTION.getId(), offhand.getIndex(), WidgetInfo.INVENTORY.getId()
                              )
                     );
               }
            }

            if (this.utils.isItemEquipped(Collections.singleton(this.configvk.specWeapon())) && !this.isInVorkath()) {
               WidgetItem weapon = this.utils.getInventoryWidgetItem(this.configvk.normalWeapon());
               WidgetItem offhand = this.utils.getInventoryWidgetItem(this.configvk.normalOffhand());
               if (weapon != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction("", "", weapon.getId(), MenuAction.ITEM_SECOND_OPTION.getId(), weapon.getIndex(), WidgetInfo.INVENTORY.getId())
                     );
               }

               if (offhand != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "", "", offhand.getId(), MenuAction.ITEM_SECOND_OPTION.getId(), offhand.getIndex(), WidgetInfo.INVENTORY.getId()
                              )
                     );
               }
            }

            if (!this.loot.isEmpty() && !this.utils.inventoryFull() && this.isInVorkath()) {
               return AVorkathState.LOOT_ITEMS;
            } else if (this.utils.inventoryContains(22124)
               && this.loot.isEmpty()
               && !isInPOH(this.client)
               && this.isInVorkath()
               && !this.configvk.onlytelenofood()) {
               return AVorkathState.WALK_SECOND;
            } else if (!this.utils.inventoryContains(this.configvk.foodID())
               && this.client.getBoostedSkillLevel(Skill.HITPOINTS) <= this.configvk.healthTP()
               && this.loot.isEmpty()
               && !isInPOH(this.client)
               && this.isInVorkath()) {
               return AVorkathState.WALK_SECOND;
            } else if (this.utils.inventoryContains(this.configvk.foodID())
               && this.utils.inventoryFull()
               && !this.loot.isEmpty()
               && !isInPOH(this.client)
               && this.isInVorkath()) {
               return AVorkathState.EAT_FOOD;
            } else if (this.getRestoreItem() == null && this.client.getBoostedSkillLevel(Skill.PRAYER) <= this.configvk.prayTP() && this.isInVorkath()) {
               return AVorkathState.WALK_SECOND;
            } else if (isInPOH(this.client)) {
               return AVorkathState.TELE_EDGE;
            } else if (this.player.getWorldArea().intersectsWith(this.RELEKKA_POH)) {
               return AVorkathState.WALK_THIRD;
            } else if (this.player.getWorldArea().intersectsWith(this.RELEKKA_TOWN)) {
               return AVorkathState.USE_BOAT;
            } else if (this.player.getWorldArea().intersectsWith(this.VORKATH)) {
               return AVorkathState.JUMP_OBSTACLE;
            } else if (!this.acidSpots.isEmpty() && this.isInVorkath()) {
               return AVorkathState.ACID_WALK;
            } else if (!this.noBomb && this.isInVorkath()) {
               return AVorkathState.HANDLE_BOMB;
            } else if (!this.noBomb2 && this.isInVorkath()) {
               return AVorkathState.HANDLE_ICE;
            } else if (this.configvk.antivenomplus() && this.client.getVar(VarPlayer.IS_POISONED) > 0 && this.isInVorkath()) {
               return AVorkathState.DRINK_ANTIVENOM;
            } else if (this.client.getBoostedSkillLevel(Skill.RANGED) <= this.configvk.potThreshold() && this.isInVorkath() && this.configvk.useRanged()) {
               return AVorkathState.DRINK_RANGE;
            } else if (this.client.getBoostedSkillLevel(Skill.STRENGTH) <= this.configvk.potThreshold() && this.isInVorkath() && !this.configvk.useRanged()) {
               return AVorkathState.DRINK_COMBAT;
            } else if (this.configvk.superantifire() && this.client.getVarbitValue(6101) == 0 && this.isInVorkath()) {
               return AVorkathState.DRINK_ANTIFIRE;
            } else if (!this.configvk.superantifire() && this.client.getVarbitValue(3981) == 0 && this.isInVorkath()) {
               return AVorkathState.DRINK_ANTIFIRE;
            } else if (this.client.getVar(Varbits.QUICK_PRAYER) == 0 && this.isInVorkath() && this.acidSpots.isEmpty() && this.noBomb2 && this.noBomb) {
               return AVorkathState.ACTIVATE_PRAY;
            } else if ((this.client.getVar(Varbits.QUICK_PRAYER) == 0 || !this.isInVorkath() || this.acidSpots.isEmpty()) && this.noBomb2 && this.noBomb) {
               if (this.utils.findNearestNpc(new int[]{8059}) != null
                  && this.isInVorkath()
                  && this.loot.isEmpty()
                  && this.utils.inventoryItemContainsAmount(this.configvk.foodID(), 3, false, false)) {
                  return AVorkathState.WAKE_VORKATH;
               } else if (this.utils.findNearestNpc(new int[]{8059}) != null
                  && this.isInVorkath()
                  && this.loot.isEmpty()
                  && !this.utils.inventoryItemContainsAmount(this.configvk.foodID(), 3, false, false)) {
                  return AVorkathState.WALK_SECOND;
               } else if (this.player.getWorldLocation().distanceTo(this.vorkath.getWorldArea()) <= 1 && this.configvk.useRanged()) {
                  return AVorkathState.MOVE_AWAY;
               } else if (!this.utils.isItemEquipped(Collections.singleton(this.configvk.specWeapon()))
                  && this.utils.inventoryFull()
                  && this.utils.inventoryContains(this.configvk.foodID())
                  && this.configvk.normalOffhand() != 0
                  && this.calculateHealth(this.vorkath, 750) >= this.configvk.specHP()
                  && this.client.getVar(VarPlayer.SPECIAL_ATTACK_ENABLED) == 0
                  && this.client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) >= this.configvk.specThreshold() * 10
                  && this.configvk.useSpec()
                  && this.noBomb
                  && this.noBomb2
                  && this.utils.findNearestNpc(new int[]{8061}) != null
                  && this.acidSpots.isEmpty()
                  && this.vorkath != null
                  && this.isInVorkath()) {
                  return AVorkathState.EAT_FOOD;
               } else if (!this.utils.isItemEquipped(Collections.singleton(this.configvk.specWeapon()))
                  && this.calculateHealth(this.vorkath, 750) >= this.configvk.specHP()
                  && this.client.getVar(VarPlayer.SPECIAL_ATTACK_ENABLED) == 0
                  && this.client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) >= this.configvk.specThreshold() * 10
                  && this.configvk.useSpec()
                  && this.noBomb
                  && this.noBomb2
                  && this.utils.findNearestNpc(new int[]{8061}) != null
                  && this.acidSpots.isEmpty()
                  && this.vorkath != null
                  && this.isInVorkath()) {
                  return AVorkathState.EQUIP_SPEC;
               } else if (this.utils.isItemEquipped(Collections.singleton(this.configvk.specWeapon()))
                  && this.calculateHealth(this.vorkath, 750) >= this.configvk.specHP()
                  && this.client.getVar(VarPlayer.SPECIAL_ATTACK_ENABLED) == 0
                  && this.client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) >= this.configvk.specThreshold() * 10
                  && this.configvk.useSpec()
                  && this.noBomb
                  && this.noBomb2
                  && this.utils.findNearestNpc(new int[]{8061}) != null
                  && this.acidSpots.isEmpty()
                  && this.vorkath != null
                  && this.isInVorkath()) {
                  return AVorkathState.SPECIAL_ATTACK;
               } else {
                  return this.noBomb
                        && this.noBomb2
                        && this.utils.findNearestNpc(new int[]{8061}) != null
                        && this.acidSpots.isEmpty()
                        && this.vorkath != null
                        && this.client.getLocalPlayer().getInteracting() != this.vorkath
                        && this.isInVorkath()
                     ? AVorkathState.ATTACK_VORKATH
                     : AVorkathState.TIMEOUT;
               }
            } else {
               return AVorkathState.DEACTIVATE_PRAY;
            }
         }
      }
   }

   private AVorkathState getBankState() {
      if (!this.banked) {
         this.utils.depositAll();
         this.banked = true;
         return AVorkathState.DEPOSIT_ITEMS;
      } else {
         if (this.configvk.useSpec() && !this.utils.inventoryContains(this.configvk.specWeapon())) {
            this.utils.withdrawItem(this.configvk.specWeapon());
         }

         if (!this.utils.inventoryContains(8013)) {
            return AVorkathState.WITHDRAW_TELES;
         } else if (!this.utils.inventoryContains(12791)) {
            return AVorkathState.WITHDRAW_POUCH;
         } else if (!this.utils.inventoryContains(2444) && this.configvk.useRanged() && !this.configvk.supers()) {
            return AVorkathState.WITHDRAW_RANGED;
         } else if (!this.utils.inventoryContains(22461) && this.configvk.useRanged() && this.configvk.supers()) {
            return AVorkathState.WITHDRAW_RANGED;
         } else if (!this.utils.inventoryContains(12695) && !this.configvk.useRanged()) {
            return AVorkathState.WITHDRAW_COMBAT;
         } else if (this.configvk.superantifire() && !this.utils.inventoryContains(22209)) {
            return AVorkathState.WITHDRAW_ANTIFIRE;
         } else if (!this.configvk.superantifire() && !this.utils.inventoryContains(2452)) {
            return AVorkathState.WITHDRAW_ANTIFIRE;
         } else if (this.configvk.antivenomplus() && !this.utils.inventoryContains(12913)) {
            return AVorkathState.WITHDRAW_VENOM;
         } else if (!this.configvk.antivenomplus() && !this.utils.inventoryContains(5952)) {
            return AVorkathState.WITHDRAW_VENOM;
         } else if (!this.utils.inventoryContains(3024) && !this.utils.inventoryContains(2434)) {
            return AVorkathState.WITHDRAW_RESTORES;
         } else if (!this.utils.inventoryContains(this.DIAMOND_SET) && this.configvk.useRanged() && !this.configvk.useBlowpipe()) {
            return AVorkathState.WITHDRAW_BOLTS;
         } else if (!this.utils.inventoryContains(this.configvk.foodID())) {
            return AVorkathState.WITHDRAW_FOOD1;
         } else if (!this.utils.inventoryContains(this.configvk.foodID2())) {
            return AVorkathState.WITHDRAW_FOOD2;
         } else {
            return this.player.getWorldArea().intersectsWith(this.EDGEVILLE_BANK) && this.utils.inventoryContains(this.configvk.foodID2()) && this.banked
               ? AVorkathState.WALK_FIRST
               : AVorkathState.TIMEOUT;
         }
      }
   }

   @Subscribe
   private void onGameTick(GameTick event) throws ClassNotFoundException, SQLException {
      this.player = this.client.getLocalPlayer();
      if (this.client != null && this.player != null) {
         if (!this.startTeaks) {
            return;
         }

         this.state = this.getState();
         this.beforeLoc = this.player.getLocalLocation();
         this.utils.setMenuEntry(null);
         switch(this.state) {
            case TIMEOUT:
               this.utils.handleRun(30, 20);
               --this.timeout;
               break;
            case SPECIAL_ATTACK:
               if (this.utils.isItemEquipped(Collections.singleton(this.configvk.specWeapon()))) {
                  this.clientThread
                     .invoke(() -> this.client.invokeMenuAction("Use <col=00ff00>Special Attack</col>", "", 1, MenuAction.CC_OP.getId(), -1, 38862884));
               }
               break;
            case MOVE_AWAY:
               this.utils
                  .walk(
                     new WorldPoint(
                        this.player.getWorldLocation().getX(), this.player.getWorldLocation().getY() - 3, this.player.getWorldLocation().getPlane()
                     )
                  );
               break;
            case TELE_EDGE:
               this.utils.useDecorativeObject(13523, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), this.sleepDelay());
               this.timeout = this.tickDelay();
               break;
            case EQUIP_SPEC:
               WidgetItem weapon = this.utils.getInventoryWidgetItem(this.configvk.specWeapon());
               if (weapon != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction("", "", weapon.getId(), MenuAction.ITEM_SECOND_OPTION.getId(), weapon.getIndex(), WidgetInfo.INVENTORY.getId())
                     );
               }
               break;
            case EQUIP_RUBIES:
               WidgetItem boltz = this.utils.getInventoryWidgetItem(this.RUBY_SET);
               if (boltz != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction("", "", boltz.getId(), MenuAction.ITEM_SECOND_OPTION.getId(), boltz.getIndex(), WidgetInfo.INVENTORY.getId())
                     );
               }
               break;
            case DRINK_POOL:
               GameObject Pool = this.utils.findNearestGameObject(new int[]{29240, 29241});
               this.utils.useGameObjectDirect(Pool, this.sleepDelay(), MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
               this.timeout = this.tickDelay();
               break;
            case EQUIP_DIAMONDS:
               WidgetItem bolts = this.utils.getInventoryWidgetItem(this.DIAMOND_SET);
               if (bolts != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction("", "", bolts.getId(), MenuAction.ITEM_SECOND_OPTION.getId(), bolts.getIndex(), WidgetInfo.INVENTORY.getId())
                     );
               }
               break;
            case ACID_WALK:
               this.calculateAcidFreePath();
               ++this.AcidTickCount;
               if (this.acidFreePathLength >= 3) {
                  if (this.FirstWalk) {
                     this.utils.walk((WorldPoint)this.acidFreePath.get(1));
                     this.FirstWalk = false;
                  }

                  if (!this.FirstWalk) {
                     this.utils.walk((WorldPoint)this.acidFreePath.get(this.acidFreePath.size() - 1));
                     this.FirstWalk = true;
                  }
               }
               break;
            case ATTACK_VORKATH:
               if (this.isInVorkath()) {
                  this.utils.attackNPCDirect(this.vorkath);
               }
               break;
            case HANDLE_ICE:
               NPC npc = this.utils.findNearestNpc(new String[]{"Zombified Spawn"});
               this.clientThread.invoke(() -> this.client.invokeMenuAction("", "", 0, MenuAction.WIDGET_TYPE_2.getId(), -1, 14286876));
               this.timeout = 1;
               this.clientThread
                  .invoke(
                     () -> this.client
                           .invokeMenuAction(
                              "", "", npc.getIndex(), MenuAction.SPELL_CAST_ON_NPC.getId(), npc.getLocalLocation().getX(), npc.getLocalLocation().getY()
                           )
                  );
               break;
            case HANDLE_BOMB:
               WorldPoint loc = this.client.getLocalPlayer().getWorldLocation();
               LocalPoint localLoc = LocalPoint.fromWorld(this.client, loc);
               this.dodgeRight = new LocalPoint(localLoc.getX() + 256, localLoc.getY());
               this.dodgeLeft = new LocalPoint(localLoc.getX() - 256, localLoc.getY());
               if (localLoc.distanceTo(this.dodgeLeft) <= 1) {
                  this.noBomb = true;
                  this.noBomb2 = true;
               }

               if (localLoc.distanceTo(this.dodgeRight) <= 1) {
                  this.noBomb = true;
                  this.noBomb2 = true;
               }

               if (localLoc.getX() < 6208) {
                  this.utils.walk(this.dodgeRight);
                  this.timeout = this.tickDelay();
                  this.noBomb = true;
                  this.noBomb2 = true;
               } else {
                  this.utils.walk(this.dodgeLeft);
                  this.timeout = this.tickDelay();
                  this.noBomb = true;
                  this.noBomb2 = true;
               }
               break;
            case DEACTIVATE_PRAY:
               this.clientThread.invoke(() -> this.client.invokeMenuAction("Deactivate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
               break;
            case ACTIVATE_PRAY:
               this.clientThread.invoke(() -> this.client.invokeMenuAction("Activate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
               break;
            case WITHDRAW_COMBAT:
               if (!this.configvk.supers()) {
                  this.utils.withdrawItem(9739);
               } else {
                  this.utils.withdrawItem(12695);
               }

               this.timeout = 4;
               break;
            case WITHDRAW_RANGED:
               if (!this.configvk.supers()) {
                  this.utils.withdrawItem(2444);
               } else {
                  this.utils.withdrawItem(22461);
               }

               this.timeout = 4;
               break;
            case WAKE_VORKATH:
               this.clientThread
                  .invoke(
                     () -> this.client
                           .invokeMenuAction("", "", this.utils.findNearestNpc(new String[]{"Vorkath"}).getIndex(), MenuAction.NPC_FIRST_OPTION.getId(), 0, 0)
                  );
               this.timeout = this.tickDelay();
               break;
            case CLOSE_BANK:
               this.utils.closeBank();
               this.timeout = this.tickDelay();
               break;
            case WITHDRAW_VENOM:
               if (this.configvk.antivenomplus()) {
                  this.utils.withdrawItemAmount(12913, this.configvk.antipoisonamount());
               }

               if (!this.configvk.antivenomplus()) {
                  this.utils.withdrawItemAmount(5952, this.configvk.antipoisonamount());
               }

               this.timeout = 4;
               break;
            case WITHDRAW_ANTIFIRE:
               if (this.configvk.superantifire()) {
                  this.utils.withdrawItem(22209);
               }

               if (!this.configvk.superantifire()) {
                  this.utils.withdrawItem(2452);
               }

               this.timeout = 4;
               break;
            case WITHDRAW_POUCH:
               this.utils.withdrawItem(12791);
               this.timeout = this.tickDelay();
               break;
            case WITHDRAW_RESTORES:
               if (this.configvk.useRestores()) {
                  this.utils.withdrawItemAmount(3024, this.configvk.praypotAmount());
               } else {
                  this.utils.withdrawItemAmount(2434, this.configvk.praypotAmount());
               }

               this.timeout = 4;
               break;
            case WITHDRAW_TELES:
               this.utils.withdrawItemAmount(8013, 10);
               this.timeout = 4;
               break;
            case WITHDRAW_BOLTS:
               if (this.utils.bankContains(21946, 1)) {
                  this.utils.withdrawAllItem(21946);
               }

               if (!this.utils.bankContains(21946, 1) && this.utils.bankContains(9243, 1)) {
                  this.utils.withdrawAllItem(9243);
               }

               this.timeout = 4;
               break;
            case WITHDRAW_FOOD1:
               this.utils.withdrawItemAmount(this.configvk.foodID(), this.configvk.foodAmount());
               this.timeout = 4;
               break;
            case WITHDRAW_FOOD2:
               this.utils.withdrawItemAmount(this.configvk.foodID2(), this.configvk.foodAmount2());
               this.timeout = 4;
               break;
            case MOVING:
               this.utils.handleRun(30, 20);
               this.timeout = this.tickDelay();
               break;
            case DRINK_ANTIVENOM:
               WidgetItem ven = this.GetAntiVenomItem();
               if (ven != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "Drink",
                                 "<col=ff9040>Potion",
                                 ven.getId(),
                                 MenuAction.ITEM_FIRST_OPTION.getId(),
                                 ven.getIndex(),
                                 WidgetInfo.INVENTORY.getId()
                              )
                     );
               }
               break;
            case DRINK_COMBAT:
               WidgetItem Cpot = this.GetCombatItem();
               if (Cpot != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "Drink",
                                 "<col=ff9040>Potion",
                                 Cpot.getId(),
                                 MenuAction.ITEM_FIRST_OPTION.getId(),
                                 Cpot.getIndex(),
                                 WidgetInfo.INVENTORY.getId()
                              )
                     );
               }
               break;
            case EAT_FOOD:
               WidgetItem food = this.GetFoodItem();
               if (food != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction("", "", food.getId(), MenuAction.ITEM_FIRST_OPTION.getId(), food.getIndex(), WidgetInfo.INVENTORY.getId())
                     );
               }
               break;
            case DRINK_RANGE:
               WidgetItem Rpot = this.GetRangedItem();
               if (Rpot != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "Drink",
                                 "<col=ff9040>Potion",
                                 Rpot.getId(),
                                 MenuAction.ITEM_FIRST_OPTION.getId(),
                                 Rpot.getIndex(),
                                 WidgetInfo.INVENTORY.getId()
                              )
                     );
               }
               break;
            case DRINK_ANTIFIRE:
               WidgetItem overload = this.GetAntifireItem();
               if (overload != null) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "Drink",
                                 "<col=ff9040>Potion",
                                 overload.getId(),
                                 MenuAction.ITEM_FIRST_OPTION.getId(),
                                 overload.getIndex(),
                                 WidgetInfo.INVENTORY.getId()
                              )
                     );
               }
               break;
            case WALK_FIRST:
               this.clientThread
                  .invoke(
                     () -> this.client
                           .invokeMenuAction(
                              "",
                              "",
                              8013,
                              MenuAction.ITEM_THIRD_OPTION.getId(),
                              this.utils.getInventoryWidgetItem(8013).getIndex(),
                              WidgetInfo.INVENTORY.getId()
                           )
                  );
               this.banked = false;
               this.timeout = this.tickDelay();
               break;
            case WALK_SECOND:
               this.clientThread
                  .invoke(
                     () -> this.client
                           .invokeMenuAction(
                              "",
                              "",
                              8013,
                              MenuAction.ITEM_FIRST_OPTION.getId(),
                              this.utils.getInventoryWidgetItem(8013).getIndex(),
                              WidgetInfo.INVENTORY.getId()
                           )
                  );
               this.timeout = this.tickDelay();
               break;
            case WALK_THIRD:
               this.utils.walk(new WorldPoint(2643, 3676, 0));
               this.timeout = this.tickDelay();
               break;
            case USE_BOAT:
               GameObject boat = this.utils.findNearestGameObject(new int[]{29917});
               this.utils.useGameObjectDirect(boat, this.sleepDelay(), MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
               this.timeout = this.tickDelay();
               break;
            case FIND_BANK:
               this.openBank();
               this.timeout = this.tickDelay();
               break;
            case DEPOSIT_ITEMS:
               this.timeout = this.tickDelay();
               break;
            case WITHDRAW_ITEMS:
               this.timeout = this.tickDelay();
               break;
            case LOOT_ITEMS:
               this.lootItem(this.loot);
               break;
            case JUMP_OBSTACLE:
               this.utils.useGameObject(31990, 3, this.sleepDelay());
               this.timeout = this.tickDelay();
         }
      }

   }

   @Subscribe
   private void onItemSpawned(ItemSpawned event) {
      TileItem item = event.getItem();
      String itemName = this.client.getItemDefinition(item.getId()).getName().toLowerCase();
      if (this.lootableItems.stream().anyMatch(itemName.toLowerCase()::contains) && item.getId() != 1751) {
         this.loot.add(item);
      }

   }

   @Subscribe
   private void onItemDespawned(ItemDespawned event) {
      this.loot.remove(event.getItem());
   }

   public int getBoostAmount(WidgetItem restoreItem, int prayerLevel) {
      if (PrayerRestoreType.PRAYER_POTION.containsId(restoreItem.getId())) {
         return 7 + (int)Math.floor((double)prayerLevel * 0.25);
      } else if (PrayerRestoreType.SANFEW_SERUM.containsId(restoreItem.getId())) {
         return 4 + (int)Math.floor((double)prayerLevel * 0.0);
      } else {
         return PrayerRestoreType.SUPER_RESTORE.containsId(restoreItem.getId()) ? 8 + (int)Math.floor((double)prayerLevel * 0.25) : 0;
      }
   }

   private int calculateHealth(NPC target, Integer maxHealth) {
      if (target != null && target.getName() != null) {
         int healthScale = target.getHealthScale();
         int healthRatio = target.getHealthRatio();
         return healthRatio >= 0 && healthScale > 0 && maxHealth != null ? (int)((float)(maxHealth * healthRatio / healthScale) + 0.5F) : -1;
      } else {
         return -1;
      }
   }

   @Subscribe
   private void onClientTick(ClientTick event) {
      if (this.acidSpots.size() != this.lastAcidSpotsSize) {
         if (this.acidSpots.size() == 0) {
            this.acidFreePath.clear();
         } else {
            this.calculateAcidFreePath();
         }

         this.lastAcidSpotsSize = this.acidSpots.size();
      }

   }

   @Subscribe
   public void onAnimationChanged(AnimationChanged event) {
      if (this.vorkath != null) {
         Actor actor = event.getActor();
         if (actor.getAnimation() == 7950 && actor.getName().contains("Vorkath")) {
            Widget widget = this.client.getWidget(10485775);
            if (widget != null) {
               this.bounds = widget.getBounds();
            }
         }

         if (actor.getAnimation() == 7949 && actor.getName().contains("Vorkath") && this.client.getVar(Varbits.QUICK_PRAYER) == 1) {
            this.clientThread.invoke(() -> this.client.invokeMenuAction("Deactivate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
         }
      }

   }

   @Subscribe
   private void onProjectileSpawned(ProjectileSpawned event) {
      if (this.client.getGameState() == GameState.LOGGED_IN) {
         Projectile projectile = event.getProjectile();
         WorldPoint loc = this.client.getLocalPlayer().getWorldLocation();
         LocalPoint localLoc = LocalPoint.fromWorld(this.client, loc);
         if (projectile.getId() == 1481) {
            this.noBomb = false;
         }

         if (projectile.getId() == 395) {
            this.noBomb2 = false;
            if (this.client.getLocalPlayer().getInteracting() != null) {
               this.utils.walk(localLoc);
            }
         }
      }

   }

   @Subscribe
   private void onProjectileMoved(ProjectileMoved event) {
      Projectile proj = event.getProjectile();
      LocalPoint loc = event.getPosition();
      WorldPoint location = WorldPoint.fromLocal(this.client, loc);
      LocalPoint playerLocation = this.client.getLocalPlayer().getLocalLocation();
      WorldPoint loc1 = this.client.getLocalPlayer().getWorldLocation();
      LocalPoint localLoc = LocalPoint.fromWorld(this.client, loc1);
      if (proj.getId() == 1483) {
         this.addAcidSpot(WorldPoint.fromLocal(this.client, loc));
      }

      if (proj.getId() == 395) {
         this.noBomb2 = false;
         if (this.client.getLocalPlayer().getInteracting() != null) {
            this.utils.walk(localLoc);
         }
      }

      if (proj.getId() == 1481) {
         this.noBomb = false;
      }

   }

   public String getTag(int itemId) {
      String tag = this.configManager.getConfiguration("inventorytags", "item_" + itemId);
      return tag != null && !tag.isEmpty() ? tag : "";
   }

   @Subscribe
   private void onNpcSpawned(NpcSpawned event) {
      NPC npc = event.getNpc();
      if (npc.getName() != null) {
         if (npc.getName().equals("Vorkath")) {
            this.vorkath = event.getNpc();
         }

         if (npc.getName().equals("Zombified Spawn")) {
            this.noBomb2 = false;
         }

      }
   }

   @Subscribe
   private void onNpcDespawned(NpcDespawned event) {
      NPC npc = event.getNpc();
      if (npc.getName() != null) {
         Widget widget = this.client.getWidget(10485775);
         if (widget != null) {
            this.bounds = widget.getBounds();
         }

         if (npc.getName().equals("Vorkath")) {
            this.vorkath = null;
            if (this.client.getVar(Varbits.QUICK_PRAYER) == 1) {
               this.clientThread.invoke(() -> this.client.invokeMenuAction("Deactivate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
            }
         }

      }
   }

   @Subscribe
   private void onGameStateChanged(GameStateChanged event) {
      this.loot.clear();
      GameState gamestate = event.getGameState();
      if (gamestate == GameState.LOADING && this.inFight) {
         this.reset();
      }

   }

   public static boolean isInPOH(Client client) {
      return Arrays.stream(client.getMapRegions()).anyMatch(regions::contains);
   }

   @Subscribe
   private void onChatMessage(ChatMessage event) {
      if (event.getType() == ChatMessageType.GAMEMESSAGE) {
         Widget widget = this.client.getWidget(10485775);
         if (widget != null) {
            this.bounds = widget.getBounds();
         }

         String prayerMessage = "Your prayers have been disabled!";
         String poisonMessage = "You have been poisoned by venom!";
         String poisonMessageNV = "You have been poisoned!";
         String frozenMessage = "You have been frozen!";
         String spawnExplode = "The spawn violently explodes, unfreezing you as it does so.";
         String unfrozenMessage = "You become unfrozen as you kill the spawn.";
         if (event.getMessage().equals(prayerMessage) || event.getMessage().contains(prayerMessage)) {
            this.clientThread.invoke(() -> this.client.invokeMenuAction("Activate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
         }

         if (event.getMessage().equals(frozenMessage)) {
            this.noBomb = false;
            this.noBomb2 = false;
         }

         if (event.getMessage().equals(poisonMessage)) {
            WidgetItem pot = this.GetAntiVenomItem();
            if (pot != null) {
               this.clientThread
                  .invoke(
                     () -> this.client
                           .invokeMenuAction(
                              "Drink", "<col=ff9040>Potion", pot.getId(), MenuAction.ITEM_FIRST_OPTION.getId(), pot.getIndex(), WidgetInfo.INVENTORY.getId()
                           )
                  );
            }
         }

         if (event.getMessage().equals(poisonMessageNV)) {
            WidgetItem pot = this.GetAntiVenomItem();
            if (pot != null && this.configvk.antivenomplus()) {
               this.clientThread
                  .invoke(
                     () -> this.client
                           .invokeMenuAction(
                              "Drink", "<col=ff9040>Potion", pot.getId(), MenuAction.ITEM_FIRST_OPTION.getId(), pot.getIndex(), WidgetInfo.INVENTORY.getId()
                           )
                  );
            }
         }

         if (event.getMessage().equals(spawnExplode) || event.getMessage().equals(unfrozenMessage)) {
            this.noBomb = true;
            this.noBomb2 = true;
            if (this.isInVorkath()) {
               this.utils.attackNPCDirect(this.vorkath);
            }
         }

      }
   }

   private void addAcidSpot(WorldPoint acidSpotLocation) {
      if (!this.acidSpots.contains(acidSpotLocation)) {
         this.acidSpots.add(acidSpotLocation);
      }

   }

   private void calculateAcidFreePath() {
      this.acidFreePath.clear();
      if (this.vorkath != null) {
         int[][][] directions = new int[][][]{{{0, 1}, {0, -1}}, {{1, 0}, {-1, 0}}};
         List<WorldPoint> bestPath = new ArrayList();
         double bestClicksRequired = 99.0;
         WorldPoint playerLoc = this.client.getLocalPlayer().getWorldLocation();
         WorldPoint vorkLoc = this.vorkath.getWorldLocation();
         int maxX = vorkLoc.getX() + 14;
         int minX = vorkLoc.getX() - 8;
         int maxY = vorkLoc.getY() - 1;
         int minY = vorkLoc.getY() - 8;

         for(int x = -1; x < 2; ++x) {
            for(int y = -1; y < 2; ++y) {
               WorldPoint baseLocation = new WorldPoint(playerLoc.getX() + x, playerLoc.getY() + y, playerLoc.getPlane());
               if (!this.acidSpots.contains(baseLocation) && baseLocation.getY() >= minY && baseLocation.getY() <= maxY) {
                  for(int d = 0; d < directions.length; ++d) {
                     double currentClicksRequired = (double)(Math.abs(x) + Math.abs(y));
                     if (currentClicksRequired < 2.0) {
                        currentClicksRequired += (double)(Math.abs(y * directions[d][0][0]) + Math.abs(x * directions[d][0][1]));
                     }

                     if (d == 0) {
                        currentClicksRequired += 0.5;
                     }

                     List<WorldPoint> currentPath = new ArrayList();
                     currentPath.add(baseLocation);

                     for(int i = 1; i < 25; ++i) {
                        WorldPoint testingLocation = new WorldPoint(
                           baseLocation.getX() + i * directions[d][0][0], baseLocation.getY() + i * directions[d][0][1], baseLocation.getPlane()
                        );
                        if (this.acidSpots.contains(testingLocation)
                           || testingLocation.getY() < minY
                           || testingLocation.getY() > maxY
                           || testingLocation.getX() < minX
                           || testingLocation.getX() > maxX) {
                           break;
                        }

                        currentPath.add(testingLocation);
                     }

                     for(int i = 1; i < 25; ++i) {
                        WorldPoint testingLocation = new WorldPoint(
                           baseLocation.getX() + i * directions[d][1][0], baseLocation.getY() + i * directions[d][1][1], baseLocation.getPlane()
                        );
                        if (this.acidSpots.contains(testingLocation)
                           || testingLocation.getY() < minY
                           || testingLocation.getY() > maxY
                           || testingLocation.getX() < minX
                           || testingLocation.getX() > maxX) {
                           break;
                        }

                        currentPath.add(testingLocation);
                     }

                     if (currentPath.size() >= this.acidFreePathLength && currentClicksRequired < bestClicksRequired
                        || currentClicksRequired == bestClicksRequired && currentPath.size() > bestPath.size()) {
                        bestPath = currentPath;
                        bestClicksRequired = currentClicksRequired;
                     }
                  }
               }
            }
         }

         if (bestClicksRequired != 99.0) {
            this.acidFreePath = bestPath;
         }

      }
   }

   @Subscribe
   private void onGameObjectSpawned(GameObjectSpawned event) {
      GameObject obj = event.getGameObject();
      if (obj.getId() == 30032 || obj.getId() == 32000) {
         this.addAcidSpot(obj.getWorldLocation());
      }

   }

   @Subscribe
   public void onGameObjectDespawned(GameObjectDespawned event) {
      GameObject obj = event.getGameObject();
      if (obj.getId() == 30032 || obj.getId() == 32000) {
         this.acidSpots.remove(obj.getWorldLocation());
      }

   }

   public void activatePrayer(WidgetInfo widgetInfo) {
      Widget prayer_widget = this.client.getWidget(widgetInfo);
      if (prayer_widget != null) {
         if (this.client.getBoostedSkillLevel(Skill.PRAYER) > 0) {
            this.clientThread
               .invoke(
                  () -> this.client
                        .invokeMenuAction("Activate", prayer_widget.getName(), 1, MenuAction.CC_OP.getId(), prayer_widget.getItemId(), prayer_widget.getId())
               );
         }
      }
   }
}
