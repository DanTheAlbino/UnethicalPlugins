package net.runelite.client.plugins.azulrah;

import com.google.common.base.Preconditions;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.HeadIcon;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.api.Skill;
import net.runelite.api.TileItem;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.autils.AUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import org.pf4j.Extension;

@Extension
@PluginDependency(AUtils.class)
@PluginDescriptor(
   name = "AZulrah",
   description = "Anarchise' Auto Zulrah",
   tags = {"anarchise", "zulrah", "aplugins"},
   enabledByDefault = false
)
public class AZulrahPlugin extends Plugin {
   int jjj = 91911;
   private int nextRestoreVal = 0;
   @Inject
   private Client client;
   private static final String MESSAGE_STUN = "The Alchemical Hydra temporarily stuns you.";
   @Inject
   private AZulrahConfig configph;
   @Inject
   private ClientThread clientThread;
   private Rectangle bounds;
   private int timeout;
   boolean FirstWalk = true;
   private final Map<LocalPoint, Integer> projectilesMap = new HashMap();
   private final Map<GameObject, Integer> toxicCloudsMap = new HashMap();
   private int lastAttackTick = -1;
   LocalPoint standPos;
   private WorldPoint lastLocation = new WorldPoint(0, 0, 0);
   private final List<WorldPoint> obstacles = new ArrayList();
   private final Map<LocalPoint, Projectile> poisonProjectiles = new HashMap();
   @Nullable
   private NPC nm;
   @Inject
   private ItemManager itemManager;
   @Inject
   private AUtils utils;
   private boolean inFight;
   private boolean cursed;
   private Prayer prayerToClick;
   private Random r = new Random();
   List<TileItem> loot = new ArrayList();
   List<String> lootableItems = new ArrayList();
   List<String> withdrawList = new ArrayList();
   String[] list;
   String[] Loot;
   private Prayer prayer;
   @Inject
   private KeyManager keyManager;
   @Inject
   private InfoBoxManager infoBoxManager;
   @Inject
   private OverlayManager overlayManager;
   ZulrahAttributes zulrahAttributes;
   ZulrahData zulrahData;
   ZulrahPhase zulrahPhase;
   @Inject
   AZulrahOverlay zulrahOverlay;
   Instant botTimer;
   boolean noBomb = true;
   boolean noBomb2 = true;
   private NPC zulrahNpc = null;
   public AZulrahState state;
   private NPC zulrah = null;
   private int stage = 0;
   private int phaseTicks = -1;
   private int attackTicks = -1;
   private int acidFreePathLength = 3;
   private int totalTicks = 0;
   private RotationType currentRotation = null;
   private List<RotationType> potentialRotations = new ArrayList();
   private static boolean flipStandLocation = false;
   private static boolean flipPhasePrayer = false;
   private static boolean zulrahReset = false;
   private final Collection<NPC> snakelings = new ArrayList();
   private boolean holdingSnakelingHotkey = false;
   private Counter zulrahTotalTicksInfoBox;
   public static final BufferedImage[] ZULRAH_IMAGES = new BufferedImage[3];
   private final BiConsumer<RotationType, RotationType> phaseTicksHandler = (current, potential) -> {
      if (zulrahReset) {
         this.phaseTicks = 38;
      } else {
         ZulrahPhase p = current != null ? this.getCurrentPhase(current) : this.getCurrentPhase(potential);
         Preconditions.checkNotNull(p, "Attempted to set phase ticks but current Zulrah phase was somehow null. Stage: " + this.stage);
         this.phaseTicks = p.getAttributes().getPhaseTicks();
      }

   };
   private static final List<Integer> regions = Arrays.asList(7513, 7514, 7769, 7770);
   private static final List<Integer> regionz = Arrays.asList(9007, 9008);
   Player player;
   WorldArea ZULRAH_BOAT = new WorldArea(new WorldPoint(2192, 3045, 0), new WorldPoint(2221, 3068, 0));
   WorldArea ZULRAH_ISLAND = new WorldArea(new WorldPoint(2145, 3065, 0), new WorldPoint(2156, 3076, 0));
   WorldArea ZULRAH_ISLAND2 = new WorldArea(new WorldPoint(2158, 3066, 0), new WorldPoint(2193, 3086, 0));
   WorldPoint ZULRAHPOINT = new WorldPoint(2178, 3068, 0);
   WorldArea ZULRAH_ISLAND3 = new WorldArea(new WorldPoint(2172, 3063, 0), new WorldPoint(2182, 3075, 0));
   WorldPoint ZULRAHPOINT2 = new WorldPoint(2195, 3059, 0);
   WorldArea ZULRAH_ISLAND4 = new WorldArea(new WorldPoint(2157, 3068, 0), new WorldPoint(2166, 3078, 0));
   WorldArea EDGEVILLE_BANK = new WorldArea(new WorldPoint(3082, 3485, 0), new WorldPoint(3100, 3502, 0));
   LocalPoint standPos1;
   ZulrahData data;
   private boolean banked = false;
   public boolean startTeaks = false;
   @Inject
   ConfigManager configManager;
   public Prayer currentPrayer;
   boolean alreadyBanked = false;
   int[] ItemIDs;
   int lll = 999990;

   @Provides
   AZulrahConfig provideConfig(ConfigManager configManager) {
      return (AZulrahConfig)configManager.getConfig(AZulrahConfig.class);
   }

   public AZulrahPlugin() {
      super();
      this.inFight = false;
   }

   @Subscribe
   private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
      if (configButtonClicked.getGroup().equalsIgnoreCase("azulrah")) {
         if (configButtonClicked.getKey().equals("startButton")) {
            if (!this.startTeaks) {
               this.startTeaks = true;
               this.overlayManager.add(this.zulrahOverlay);
               this.botTimer = Instant.now();
            } else {
               this.reset();
            }
         }

      }
   }

   protected void startUp() throws Exception {
      this.reset();
   }

   private void reset() {
      this.loot.clear();
      this.lootableItems.clear();
      this.withdrawList.clear();
      this.Loot = this.configph.lootNames().toLowerCase().split("\\s*,\\s*");
      if (!this.configph.lootNames().isBlank()) {
         this.lootableItems.addAll(Arrays.asList(this.Loot));
      }

      this.banked = false;
      this.startTeaks = false;
      this.zulrahNpc = null;
      this.stage = 0;
      this.phaseTicks = -1;
      this.attackTicks = -1;
      this.totalTicks = 0;
      this.currentRotation = null;
      this.potentialRotations.clear();
      this.projectilesMap.clear();
      this.toxicCloudsMap.clear();
      flipStandLocation = false;
      flipPhasePrayer = false;
      zulrahReset = false;
      this.holdingSnakelingHotkey = false;
      this.lastAttackTick = -1;
      this.inFight = false;
      this.prayerToClick = null;
      this.alreadyBanked = false;
      this.zulrah = null;
      this.state = null;
      this.botTimer = null;
      this.overlayManager.remove(this.zulrahOverlay);
   }

   protected void shutDown() throws Exception {
      this.reset();
   }

   private void resetZul() {
      this.zulrahNpc = null;
      this.stage = 0;
      this.phaseTicks = -1;
      this.attackTicks = -1;
      this.totalTicks = 0;
      this.currentRotation = null;
      this.potentialRotations.clear();
      this.projectilesMap.clear();
      this.toxicCloudsMap.clear();
      flipStandLocation = false;
      flipPhasePrayer = false;
      zulrahReset = false;
      this.holdingSnakelingHotkey = false;
      this.lastAttackTick = -1;
      this.inFight = false;
      this.prayerToClick = null;
      this.zulrah = null;
      this.banked = false;
   }

   private static boolean isInPOH(Client client) {
      return Arrays.stream(client.getMapRegions()).anyMatch(regions::contains);
   }

   private static boolean isInZulrah(Client client) {
      return Arrays.stream(client.getMapRegions()).anyMatch(regionz::contains);
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

   public AZulrahState getState() {
      if (this.timeout > 0) {
         return AZulrahState.TIMEOUT;
      } else {
         return this.utils.isBankOpen() ? this.getBankState() : this.getStates();
      }
   }

   private AZulrahState getStates() {
      NPC bs = this.utils.findNearestNpc(new int[]{2042, 2043, 2044});
      if (this.player.getWorldArea().intersectsWith(this.EDGEVILLE_BANK) && !this.banked) {
         this.loot.clear();
         return AZulrahState.FIND_BANK;
      } else if (this.player.getWorldArea().intersectsWith(this.EDGEVILLE_BANK) && this.banked && !this.utils.isBankOpen()) {
         this.banked = false;
         this.loot.clear();
         return AZulrahState.FIND_BANK;
      } else if (this.player.getWorldArea().intersectsWith(this.ZULRAH_BOAT) && this.utils.findNearestGameObject(new int[]{10068}) != null) {
         return AZulrahState.USE_BOAT;
      } else {
         if (this.player.getWorldArea().intersectsWith(this.ZULRAH_ISLAND)) {
            this.alreadyBanked = false;
            this.utils.useGroundObject(10663, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), this.sleepDelay());
         }

         if (this.player.getWorldArea().intersectsWith(this.ZULRAH_ISLAND3)) {
            return AZulrahState.WALK_FOURTH;
         } else if (this.player.getWorldArea().intersectsWith(this.ZULRAH_ISLAND4)) {
            this.alreadyBanked = false;
            return AZulrahState.WALK_THIRD;
         } else if (!this.loot.isEmpty() && !this.utils.inventoryFull() && isInZulrah(this.client)) {
            return AZulrahState.LOOT_ITEMS;
         } else if (this.utils.inventoryContains(this.configph.foodID())
            && this.utils.inventoryFull()
            && !this.loot.isEmpty()
            && !isInPOH(this.client)
            && isInZulrah(this.client)) {
            return AZulrahState.EAT_FOOD;
         } else if (this.utils.inventoryContains(12934) && this.loot.isEmpty() && !isInPOH(this.client) && isInZulrah(this.client)) {
            Widget inventory = this.client.getWidget(WidgetInfo.INVENTORY);

            for(WidgetItem item : inventory.getWidgetItems()) {
               if ("Group 3".equalsIgnoreCase(this.getTag(item.getId()))) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "Wield",
                                 "<col=ff9040>" + item.getId(),
                                 item.getId(),
                                 MenuAction.ITEM_SECOND_OPTION.getId(),
                                 item.getIndex(),
                                 WidgetInfo.INVENTORY.getId()
                              )
                     );
               }
            }

            return AZulrahState.TELE_TAB;
         } else if (this.getRestoreItem() == null && isInZulrah(this.client)) {
            Widget inventory = this.client.getWidget(WidgetInfo.INVENTORY);

            for(WidgetItem item : inventory.getWidgetItems()) {
               if ("Group 3".equalsIgnoreCase(this.getTag(item.getId()))) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "Wield",
                                 "<col=ff9040>" + item.getId(),
                                 item.getId(),
                                 MenuAction.ITEM_SECOND_OPTION.getId(),
                                 item.getIndex(),
                                 WidgetInfo.INVENTORY.getId()
                              )
                     );
               }
            }

            return AZulrahState.TELE_TAB;
         } else if (!this.utils.inventoryContains(this.configph.foodID()) && this.client.getBoostedSkillLevel(Skill.HITPOINTS) < 50 && isInZulrah(this.client)
            )
          {
            Widget inventory = this.client.getWidget(WidgetInfo.INVENTORY);

            for(WidgetItem item : inventory.getWidgetItems()) {
               if ("Group 3".equalsIgnoreCase(this.getTag(item.getId()))) {
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "Wield",
                                 "<col=ff9040>" + item.getId(),
                                 item.getId(),
                                 MenuAction.ITEM_SECOND_OPTION.getId(),
                                 item.getIndex(),
                                 WidgetInfo.INVENTORY.getId()
                              )
                     );
               }
            }

            return AZulrahState.TELE_TAB;
         } else {
            if (this.client.getVar(Varbits.PRAYER_PROTECT_FROM_MAGIC) != 0 && isInPOH(this.client)) {
               this.activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);
            }

            if (this.client.getVar(Varbits.PRAYER_PROTECT_FROM_MISSILES) != 0 && isInPOH(this.client)) {
               this.activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);
            }

            if (this.client.getVar(Varbits.PRAYER_EAGLE_EYE) != 0 && isInPOH(this.client)) {
               this.activatePrayer(WidgetInfo.PRAYER_EAGLE_EYE);
            }

            if (this.client.getVar(Varbits.PRAYER_MYSTIC_MIGHT) != 0 && isInPOH(this.client)) {
               this.activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
            }

            if (this.client.getVar(Varbits.PRAYER_AUGURY) != 0 && isInPOH(this.client)) {
               this.activatePrayer(WidgetInfo.PRAYER_AUGURY);
            }

            if (this.client.getVar(Varbits.PRAYER_RIGOUR) != 0 && isInPOH(this.client)) {
               this.activatePrayer(WidgetInfo.PRAYER_RIGOUR);
            }

            if (this.client.getVar(Prayer.RIGOUR.getVarbit()) == 0
               && this.configph.Rigour()
               && isInZulrah(this.client)
               && this.configph.RangedOnly()
               && !this.configph.MageOnly()) {
               this.activatePrayer(WidgetInfo.PRAYER_RIGOUR);
            }

            if (this.client.getVar(Prayer.EAGLE_EYE.getVarbit()) == 0
               && !this.configph.Rigour()
               && isInZulrah(this.client)
               && this.configph.RangedOnly()
               && !this.configph.MageOnly()) {
               this.activatePrayer(WidgetInfo.PRAYER_EAGLE_EYE);
            }

            if (this.client.getVar(Prayer.AUGURY.getVarbit()) == 0
               && this.configph.Augury()
               && isInZulrah(this.client)
               && !this.configph.RangedOnly()
               && this.configph.MageOnly()) {
               this.activatePrayer(WidgetInfo.PRAYER_AUGURY);
            }

            if (this.client.getVar(Prayer.MYSTIC_MIGHT.getVarbit()) == 0
               && !this.configph.Augury()
               && isInZulrah(this.client)
               && !this.configph.RangedOnly()
               && this.configph.MageOnly()) {
               this.activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
            }

            if (isInPOH(this.client)
               && this.client.getBoostedSkillLevel(Skill.PRAYER) < this.client.getRealSkillLevel(Skill.PRAYER)
               && this.configph.usePOHPool()) {
               return AZulrahState.DRINK_POOL;
            } else if (isInPOH(this.client) && !this.alreadyBanked && this.configph.fairyRings()) {
               return AZulrahState.TELE_EDGE;
            } else if (isInPOH(this.client) && this.alreadyBanked && this.configph.fairyRings()) {
               return AZulrahState.FAIRY_RING;
            } else if (isInPOH(this.client) && !this.configph.fairyRings()) {
               return AZulrahState.TELE_EDGE;
            } else if (!this.configph.MageOnly()
               && this.client.getBoostedSkillLevel(Skill.RANGED) <= this.client.getRealSkillLevel(Skill.RANGED)
               && isInZulrah(this.client)) {
               return AZulrahState.DRINK_RANGE;
            } else if (!this.configph.RangedOnly()
               && !this.configph.nomagepots()
               && this.client.getBoostedSkillLevel(Skill.MAGIC) <= this.client.getRealSkillLevel(Skill.MAGIC)
               && isInZulrah(this.client)) {
               return AZulrahState.DRINK_MAGIC;
            } else if (this.configph.antivenomplus() && this.client.getVar(VarPlayer.IS_POISONED) > 0 && isInZulrah(this.client)) {
               return AZulrahState.DRINK_ANTIVENOM;
            } else if (this.client.getLocalPlayer().getLocalLocation() != this.standPos && isInZulrah(this.client)) {
               return AZulrahState.WALK_SAFE;
            } else {
               return this.client.getLocalPlayer().getInteracting() != bs && isInZulrah(this.client) ? AZulrahState.ATTACK_ZULRAH : AZulrahState.TIMEOUT;
            }
         }
      }
   }

   private AZulrahState getBankState() {
      if (!this.banked) {
         this.utils.depositAll();
         this.banked = true;
         return AZulrahState.DEPOSIT_ITEMS;
      } else if (!this.configph.fairyRings() && !this.utils.inventoryContains(12938)) {
         return AZulrahState.WITHDRAW_TELES;
      } else {
         if (this.configph.fairyRings() && !this.utils.inventoryContains(772)) {
            this.utils.withdrawItem(772);
         }

         if (!this.utils.inventoryContains(8013)) {
            return AZulrahState.WITHDRAW_HOUSE;
         } else if (!this.configph.RangedOnly()
            && !this.configph.MageOnly()
            && this.configph.mageID1() != 0
            && !this.utils.inventoryContains(this.configph.mageID1())) {
            return AZulrahState.WITHDRAW_GEAR;
         } else if (!this.configph.RangedOnly()
            && !this.configph.MageOnly()
            && this.configph.mageID2() != 0
            && !this.utils.inventoryContains(this.configph.mageID2())) {
            return AZulrahState.WITHDRAW_GEAR;
         } else if (!this.configph.RangedOnly()
            && !this.configph.MageOnly()
            && this.configph.mageID3() != 0
            && !this.utils.inventoryContains(this.configph.mageID3())) {
            return AZulrahState.WITHDRAW_GEAR;
         } else if (!this.configph.RangedOnly()
            && !this.configph.MageOnly()
            && this.configph.mageID4() != 0
            && !this.utils.inventoryContains(this.configph.mageID4())) {
            return AZulrahState.WITHDRAW_GEAR;
         } else if (!this.configph.RangedOnly()
            && !this.configph.MageOnly()
            && this.configph.mageID5() != 0
            && !this.utils.inventoryContains(this.configph.mageID5())) {
            return AZulrahState.WITHDRAW_GEAR;
         } else if (!this.configph.RangedOnly()
            && !this.configph.MageOnly()
            && this.configph.mageID6() != 0
            && !this.utils.inventoryContains(this.configph.mageID6())) {
            return AZulrahState.WITHDRAW_GEAR;
         } else if (!this.configph.RangedOnly()
            && !this.configph.MageOnly()
            && this.configph.mageID7() != 0
            && !this.utils.inventoryContains(this.configph.mageID7())) {
            return AZulrahState.WITHDRAW_GEAR;
         } else if (!this.configph.RangedOnly()
            && !this.configph.MageOnly()
            && this.configph.mageID8() != 0
            && !this.utils.inventoryContains(this.configph.mageID8())) {
            return AZulrahState.WITHDRAW_GEAR;
         } else if (!this.configph.RangedOnly() && !this.configph.nomagepots() && this.configph.imbuedheart() && !this.utils.inventoryContains(20724)) {
            return AZulrahState.WITHDRAW_MAGIC;
         } else if (!this.configph.RangedOnly()
            && !this.configph.nomagepots()
            && this.configph.supers()
            && !this.configph.imbuedheart()
            && !this.utils.inventoryContains(23745)) {
            return AZulrahState.WITHDRAW_MAGIC;
         } else if (!this.configph.RangedOnly()
            && !this.configph.nomagepots()
            && !this.configph.supers()
            && !this.configph.imbuedheart()
            && !this.utils.inventoryContains(3040)) {
            return AZulrahState.WITHDRAW_MAGIC;
         } else if (!this.configph.MageOnly() && this.configph.supers() && !this.utils.inventoryContains(24635)) {
            return AZulrahState.WITHDRAW_RANGED;
         } else if (!this.configph.MageOnly() && !this.configph.supers() && !this.utils.inventoryContains(2444)) {
            return AZulrahState.WITHDRAW_RANGED;
         } else if (!this.utils.inventoryContains(12913) && this.configph.antivenomplus() && !this.configph.serphelm() && !this.configph.superantipoison()) {
            return AZulrahState.WITHDRAW_VENOM;
         } else if (!this.utils.inventoryContains(5952) && !this.configph.antivenomplus() && !this.configph.serphelm() && !this.configph.superantipoison()) {
            return AZulrahState.WITHDRAW_VENOM;
         } else if (!this.utils.inventoryContains(2448) && !this.configph.antivenomplus() && !this.configph.serphelm() && this.configph.superantipoison()) {
            return AZulrahState.WITHDRAW_VENOM;
         } else if (!this.configph.useRestores() && !this.utils.inventoryContains(2434)) {
            return AZulrahState.WITHDRAW_RESTORES;
         } else if (this.configph.useRestores() && !this.utils.inventoryContains(3024)) {
            return AZulrahState.WITHDRAW_RESTORES;
         } else if (!this.utils.inventoryContains(this.configph.foodID())) {
            return AZulrahState.WITHDRAW_FOOD1;
         } else if (!this.utils.inventoryContains(this.configph.foodID2())) {
            return AZulrahState.WITHDRAW_FOOD2;
         } else if (this.player.getWorldArea().intersectsWith(this.EDGEVILLE_BANK) && this.utils.inventoryContains(this.configph.foodID2()) && this.banked) {
            this.alreadyBanked = true;
            return AZulrahState.WALK_SECOND;
         } else {
            return AZulrahState.TIMEOUT;
         }
      }
   }

   @Subscribe
   private void onGameTick(GameTick event) throws IOException {
      if (this.startTeaks) {
         if (this.client.getWidget(219, 1) != null) {
            if (this.player.getWorldArea().intersectsWith(this.ZULRAH_BOAT)
               && !this.utils.isMoving()
               && this.client.getWidget(219, 1).getChildren()[0].getText().equals("Return to Zulrah's shrine?")) {
               this.clientThread.invoke(() -> this.client.invokeMenuAction("", "", 0, 30, 1, 14352385));
            }

            if (this.player.getWorldArea().intersectsWith(this.ZULRAH_BOAT)
               && !this.utils.isMoving()
               && this.client.getWidget(219, 1).getText().equals("The priestess rows you to Zulrah's shrine, then hurriedly paddles away.")) {
               this.clientThread.invoke(() -> this.client.invokeMenuAction("", "", 0, MenuAction.WIDGET_TYPE_6.getId(), -1, 15007746));
            }
         }

         if (this.getZulrahNpc() != null) {
            if (this.client.getLocalPlayer().getInteracting() != this.zulrahNpc && isInZulrah(this.client) && !this.utils.isMoving()) {
               this.utils.attackNPCDirect(this.zulrahNpc);
            }

            ++this.totalTicks;
            if (this.attackTicks >= 0) {
               --this.attackTicks;
            }

            if (this.phaseTicks >= 0) {
               --this.phaseTicks;
            }

            if (this.projectilesMap.size() > 0) {
               this.projectilesMap.values().removeIf(v -> v <= 0);
               this.projectilesMap.replaceAll((k, v) -> v - 1);
            }

            if (this.toxicCloudsMap.size() > 0) {
               this.toxicCloudsMap.values().removeIf(v -> v <= 0);
               this.toxicCloudsMap.replaceAll((k, v) -> v - 1);
            }
         }

         for(ZulrahData data : this.getZulrahData()) {
            if (data.getCurrentPhase().isPresent()) {
               this.standPos = ((StandLocation)data.getCurrentDynamicStandLocation().get()).toLocalPoint();
               if (data.getCurrentPhasePrayer().isPresent()) {
                  this.currentPrayer = (Prayer)data.getCurrentPhasePrayer().get();
                  if (this.currentPrayer != null && this.client.getVar(this.currentPrayer.getVarbit()) == 0) {
                     this.activatePrayer(this.currentPrayer.getWidgetInfo());
                  }
               }
            }
         }

         this.player = this.client.getLocalPlayer();
         if (this.client != null && this.player != null) {
            this.state = this.getState();
            switch(this.state) {
               case TIMEOUT:
                  this.utils.handleRun(30, 20);
                  --this.timeout;
                  break;
               case CONTINUE2:
                  this.clientThread.invoke(() -> this.client.invokeMenuAction("", "", 0, MenuAction.WIDGET_TYPE_6.getId(), -1, 15007746));
                  break;
               case FAIRY_RING:
                  if (!this.utils.isItemEquipped(Collections.singleton(772))) {
                     this.clientThread
                        .invoke(
                           () -> this.client
                                 .invokeMenuAction(
                                    "Wield",
                                    "<col=ff9040>772",
                                    772,
                                    MenuAction.ITEM_SECOND_OPTION.getId(),
                                    this.utils.getInventoryWidgetItem(772).getIndex(),
                                    WidgetInfo.INVENTORY.getId()
                                 )
                        );
                  }

                  GameObject ring = this.utils.findNearestGameObject(new int[]{29228});
                  this.utils.useGameObjectDirect(ring, this.sleepDelay(), MenuAction.GAME_OBJECT_THIRD_OPTION.getId());
                  break;
               case WALK_FOURTH:
                  this.utils.walk(this.ZULRAHPOINT2);
                  this.timeout = this.tickDelay();
                  break;
               case WALK_THIRD:
                  Widget inventory = this.client.getWidget(WidgetInfo.INVENTORY);
                  if (this.configph.RangedOnly()) {
                     for(WidgetItem item : inventory.getWidgetItems()) {
                        if ("Group 2".equalsIgnoreCase(this.getTag(item.getId()))) {
                           this.clientThread
                              .invoke(
                                 () -> this.client
                                       .invokeMenuAction(
                                          "Wield",
                                          "<col=ff9040>" + item.getId(),
                                          item.getId(),
                                          MenuAction.ITEM_SECOND_OPTION.getId(),
                                          item.getIndex(),
                                          WidgetInfo.INVENTORY.getId()
                                       )
                              );
                        }
                     }
                  }

                  if (this.configph.MageOnly()) {
                     for(WidgetItem item : inventory.getWidgetItems()) {
                        if ("Group 3".equalsIgnoreCase(this.getTag(item.getId()))) {
                           this.clientThread
                              .invoke(
                                 () -> this.client
                                       .invokeMenuAction(
                                          "Wield",
                                          "<col=ff9040>" + item.getId(),
                                          item.getId(),
                                          MenuAction.ITEM_SECOND_OPTION.getId(),
                                          item.getIndex(),
                                          WidgetInfo.INVENTORY.getId()
                                       )
                              );
                        }
                     }
                  }

                  if (!this.configph.RangedOnly()) {
                     for(WidgetItem item : inventory.getWidgetItems()) {
                        if ("Group 3".equalsIgnoreCase(this.getTag(item.getId()))) {
                           this.clientThread
                              .invoke(
                                 () -> this.client
                                       .invokeMenuAction(
                                          "Wield",
                                          "<col=ff9040>" + item.getId(),
                                          item.getId(),
                                          MenuAction.ITEM_SECOND_OPTION.getId(),
                                          item.getIndex(),
                                          WidgetInfo.INVENTORY.getId()
                                       )
                              );
                        }
                     }
                  }

                  this.utils.walk(this.ZULRAHPOINT);
                  this.timeout = this.tickDelay();
                  break;
               case CONTINUE:
                  this.clientThread.invoke(() -> this.client.invokeMenuAction("", "", 0, MenuAction.WIDGET_TYPE_6.getId(), 1, 14352385));
                  break;
               case WALK_SAFE:
                  this.utils.walk(this.standPos);
                  this.timeout = 2;
                  break;
               case WITHDRAW_VENOM:
                  if (this.configph.antivenomplus() && !this.configph.superantipoison()) {
                     this.utils.withdrawItem(12913);
                  }

                  if (!this.configph.antivenomplus() && !this.configph.superantipoison()) {
                     this.utils.withdrawItem(5952);
                  }

                  if (this.configph.superantipoison()) {
                     this.utils.withdrawItem(2448);
                  }

                  this.timeout = this.tickDelay();
                  break;
               case ATTACK_ZULRAH:
                  this.utils.attackNPCDirect(this.zulrahNpc);
                  this.timeout = this.tickDelay();
                  break;
               case DEACTIVATE_PRAY:
                  this.clientThread.invoke(() -> this.client.invokeMenuAction("Deactivate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
                  this.timeout = this.tickDelay();
                  break;
               case ACTIVATE_PRAY:
                  this.clientThread.invoke(() -> this.client.invokeMenuAction("Activate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
                  break;
               case WALK_FIRST:
                  this.clientThread
                     .invoke(
                        () -> this.client
                              .invokeMenuAction(
                                 "",
                                 "",
                                 12938,
                                 MenuAction.ITEM_FIRST_OPTION.getId(),
                                 this.utils.getInventoryWidgetItem(12938).getIndex(),
                                 WidgetInfo.INVENTORY.getId()
                              )
                     );
                  this.timeout = this.tickDelay();
                  this.resetZul();
                  break;
               case TELE_TAB:
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
               case WALK_SECOND:
                  this.resetZul();
                  if (this.configph.fairyRings()) {
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
                  }

                  if (!this.configph.fairyRings()) {
                     this.clientThread
                        .invoke(
                           () -> this.client
                                 .invokeMenuAction(
                                    "",
                                    "",
                                    12938,
                                    MenuAction.ITEM_FIRST_OPTION.getId(),
                                    this.utils.getInventoryWidgetItem(12938).getIndex(),
                                    WidgetInfo.INVENTORY.getId()
                                 )
                        );
                  }

                  this.timeout = this.tickDelay();
                  break;
               case TELE_EDGE:
                  this.resetZul();
                  this.utils.useDecorativeObject(13523, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), this.sleepDelay());
                  this.timeout = this.tickDelay();
                  break;
               case DRINK_POOL:
                  this.resetZul();
                  GameObject Pool = this.utils.findNearestGameObject(new int[]{29240, 29241});
                  this.utils.useGameObjectDirect(Pool, this.sleepDelay(), MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
                  this.timeout = this.tickDelay();
                  break;
               case WITHDRAW_GEAR:
                  if (!this.utils.inventoryContains(this.configph.mageID1())) {
                     this.utils.withdrawItem(this.configph.mageID1());
                  }

                  if (!this.utils.inventoryContains(this.configph.mageID2())) {
                     this.utils.withdrawItem(this.configph.mageID2());
                  }

                  if (!this.utils.inventoryContains(this.configph.mageID3())) {
                     this.utils.withdrawItem(this.configph.mageID3());
                  }

                  if (!this.utils.inventoryContains(this.configph.mageID4())) {
                     this.utils.withdrawItem(this.configph.mageID4());
                  }

                  if (!this.utils.inventoryContains(this.configph.mageID5())) {
                     this.utils.withdrawItem(this.configph.mageID5());
                  }

                  if (!this.utils.inventoryContains(this.configph.mageID6())) {
                     this.utils.withdrawItem(this.configph.mageID6());
                  }

                  if (!this.utils.inventoryContains(this.configph.mageID7())) {
                     this.utils.withdrawItem(this.configph.mageID7());
                  }

                  if (!this.utils.inventoryContains(this.configph.mageID8())) {
                     this.utils.withdrawItem(this.configph.mageID8());
                  }

                  this.timeout = this.tickDelay();
                  break;
               case WITHDRAW_RANGED:
                  if (!this.configph.supers()) {
                     this.utils.withdrawItem(2444);
                  }

                  if (this.configph.supers()) {
                     this.utils.withdrawItem(24635);
                  }

                  this.timeout = this.tickDelay();
                  break;
               case WITHDRAW_MAGIC:
                  if (!this.configph.supers() && !this.configph.imbuedheart()) {
                     this.utils.withdrawItem(3040);
                  }

                  if (this.configph.supers() && !this.configph.imbuedheart()) {
                     this.utils.withdrawItem(23745);
                  }

                  if (this.configph.imbuedheart()) {
                     this.utils.withdrawItem(20724);
                  }

                  this.timeout = this.tickDelay();
                  break;
               case WITHDRAW_RESTORES:
                  if (!this.configph.useRestores()) {
                     this.utils.withdrawItemAmount(2434, this.configph.praypotAmount());
                  }

                  if (this.configph.useRestores()) {
                     this.utils.withdrawItemAmount(3024, this.configph.praypotAmount());
                  }

                  this.timeout = this.tickDelay();
                  break;
               case WITHDRAW_TELES:
                  this.utils.withdrawItem(12938);
                  this.timeout = this.tickDelay();
                  break;
               case WITHDRAW_HOUSE:
                  this.utils.withdrawItemAmount(8013, 5);
                  this.timeout = this.tickDelay();
                  break;
               case WITHDRAW_FOOD1:
                  this.utils.withdrawItemAmount(this.configph.foodID(), this.configph.foodAmount());
                  this.timeout = this.tickDelay();
                  break;
               case WITHDRAW_FOOD2:
                  this.utils.withdrawItemAmount(this.configph.foodID2(), this.configph.foodAmount2());
                  this.timeout = this.tickDelay();
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
               case DRINK_MAGIC:
                  WidgetItem Cpot = this.GetMagicItem();
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
               case USE_BOAT:
                  GameObject boat = this.utils.findNearestGameObject(new int[]{10068});
                  this.utils.useGameObjectDirect(boat, 100L, MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
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
            }
         }

      }
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
   public void onAnimationChanged(AnimationChanged event) {
      if (event.getActor() instanceof NPC) {
         NPC npc = (NPC)event.getActor();
         if (npc.getName() == null || npc.getName().equalsIgnoreCase("zulrah")) {
            switch(npc.getAnimation()) {
               case 5069:
                  this.attackTicks = 4;
                  if (this.currentRotation != null
                     && this.getCurrentPhase(this.currentRotation).getZulrahNpc().isJad()
                     && this.zulrahNpc.getInteracting() == this.client.getLocalPlayer()) {
                     flipPhasePrayer = !flipPhasePrayer;
                  }
                  break;
               case 5071:
                  this.zulrahNpc = npc;
                  this.potentialRotations = RotationType.findPotentialRotations(npc, this.stage);
                  this.phaseTicksHandler.accept(this.currentRotation, (RotationType)this.potentialRotations.get(0));
                  break;
               case 5072:
                  if (zulrahReset) {
                     zulrahReset = false;
                  }

                  if (this.currentRotation != null && this.isLastPhase(this.currentRotation)) {
                     this.stage = -1;
                     this.currentRotation = null;
                     this.potentialRotations.clear();
                     this.snakelings.clear();
                     flipStandLocation = false;
                     flipPhasePrayer = false;
                     zulrahReset = true;
                  }
                  break;
               case 5073:
                  ++this.stage;
                  if (this.currentRotation == null) {
                     this.potentialRotations = RotationType.findPotentialRotations(npc, this.stage);
                     this.currentRotation = this.potentialRotations.size() == 1 ? (RotationType)this.potentialRotations.get(0) : null;
                  }

                  this.phaseTicksHandler.accept(this.currentRotation, (RotationType)this.potentialRotations.get(0));
                  break;
               case 5804:
                  this.resetZul();
                  break;
               case 5806:
               case 5807:
                  this.attackTicks = 8;
                  flipStandLocation = !flipStandLocation;
            }

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
      WidgetItem item = this.utils.getInventoryWidgetItem(this.configph.foodID());
      return item != null ? item : item;
   }

   public WidgetItem GetRangedItem() {
      WidgetItem item = PrayerRestoreType.RANGED.getItemFromInventory(this.client);
      return item != null ? item : item;
   }

   public WidgetItem GetMagicItem() {
      WidgetItem item = PrayerRestoreType.MAGIC.getItemFromInventory(this.client);
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

   @Subscribe
   private void onProjectileMoved(ProjectileMoved event) {
      if (this.zulrahNpc != null) {
         Projectile p = event.getProjectile();
         switch(p.getId()) {
            case 1045:
            case 1047:
               this.projectilesMap.put(event.getPosition(), p.getRemainingCycles() / 30);
         }
      }
   }

   public String getTag(int itemId) {
      String tag = this.configManager.getConfiguration("inventorytags", "item_" + itemId);
      return tag != null && !tag.isEmpty() ? tag : "";
   }

   @Subscribe
   private void onNpcChanged(NpcChanged event) {
      int npcId = event.getNpc().getId();
      if (npcId == 2043 && this.client.getVar(Prayer.PROTECT_FROM_MISSILES.getVarbit()) != 0) {
         this.activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);
      }

      if (npcId == 2043 && this.client.getVar(Prayer.PROTECT_FROM_MAGIC.getVarbit()) != 0) {
         this.activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);
      }

      if (npcId == 2042 && this.client.getVar(Prayer.PROTECT_FROM_MISSILES.getVarbit()) == 0) {
         this.activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);
      } else if (npcId == 2044 && this.client.getVar(Prayer.PROTECT_FROM_MAGIC.getVarbit()) == 0) {
         this.activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);
      }

      if (event.getNpc().getName().equalsIgnoreCase("zulrah")) {
         this.zulrahNpc = event.getNpc();
      }

      if (npcId == 2042 && !this.configph.RangedOnly()) {
         Widget inventory = this.client.getWidget(WidgetInfo.INVENTORY);
         if (inventory == null) {
            return;
         }

         for(WidgetItem item : inventory.getWidgetItems()) {
            if ("Group 3".equalsIgnoreCase(this.getTag(item.getId()))) {
               this.clientThread
                  .invoke(
                     () -> this.client
                           .invokeMenuAction(
                              "Wield",
                              "<col=ff9040>" + item.getId(),
                              item.getId(),
                              MenuAction.ITEM_SECOND_OPTION.getId(),
                              item.getIndex(),
                              WidgetInfo.INVENTORY.getId()
                           )
                  );
            }
         }

         if (this.client.getVar(Prayer.AUGURY.getVarbit()) == 0 && this.configph.Augury()) {
            this.activatePrayer(WidgetInfo.PRAYER_AUGURY);
         }

         if (this.client.getVar(Prayer.MYSTIC_MIGHT.getVarbit()) == 0 && !this.configph.Augury()) {
            this.activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
         }
      } else if (npcId == 2043 && !this.configph.RangedOnly()) {
         Widget inventory = this.client.getWidget(WidgetInfo.INVENTORY);
         if (inventory == null) {
            return;
         }

         for(WidgetItem item : inventory.getWidgetItems()) {
            if ("Group 3".equalsIgnoreCase(this.getTag(item.getId()))) {
               this.clientThread
                  .invoke(
                     () -> this.client
                           .invokeMenuAction(
                              "Wield",
                              "<col=ff9040>" + item.getId(),
                              item.getId(),
                              MenuAction.ITEM_SECOND_OPTION.getId(),
                              item.getIndex(),
                              WidgetInfo.INVENTORY.getId()
                           )
                  );
            }
         }

         if (this.client.getVar(Prayer.AUGURY.getVarbit()) == 0 && this.configph.Augury()) {
            this.activatePrayer(WidgetInfo.PRAYER_AUGURY);
         }

         if (this.client.getVar(Prayer.MYSTIC_MIGHT.getVarbit()) == 0 && !this.configph.Augury()) {
            this.activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
         }
      } else if (npcId == 2044 && !this.configph.MageOnly()) {
         Widget inventory = this.client.getWidget(WidgetInfo.INVENTORY);
         if (inventory == null) {
            return;
         }

         for(WidgetItem item : inventory.getWidgetItems()) {
            if ("Group 2".equalsIgnoreCase(this.getTag(item.getId()))) {
               this.clientThread
                  .invoke(
                     () -> this.client
                           .invokeMenuAction(
                              "Wield",
                              "<col=ff9040>" + item.getId(),
                              item.getId(),
                              MenuAction.ITEM_SECOND_OPTION.getId(),
                              item.getIndex(),
                              WidgetInfo.INVENTORY.getId()
                           )
                  );
            }
         }

         if (this.client.getVar(Prayer.RIGOUR.getVarbit()) == 0 && this.configph.Rigour()) {
            this.activatePrayer(WidgetInfo.PRAYER_RIGOUR);
         }

         if (this.client.getVar(Prayer.RIGOUR.getVarbit()) == 0 && !this.configph.Rigour()) {
            this.activatePrayer(WidgetInfo.PRAYER_EAGLE_EYE);
         }
      }

   }

   @Subscribe
   private void onNpcSpawned(NpcSpawned event) {
      NPC npc = event.getNpc();
      int npcId = event.getNpc().getId();
      if (npcId == 2042 && this.client.getLocalPlayer().getOverheadIcon() != HeadIcon.RANGED) {
         this.zulrah = npc;
         this.zulrahNpc = npc;
         this.activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);
         if (this.client.getVar(Prayer.AUGURY.getVarbit()) == 0 && this.configph.Augury() && !this.configph.RangedOnly()) {
            this.activatePrayer(WidgetInfo.PRAYER_AUGURY);
         }

         if (this.client.getVar(Prayer.MYSTIC_MIGHT.getVarbit()) == 0 && !this.configph.Augury() && !this.configph.RangedOnly()) {
            this.activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
         }
      } else if (npcId == 2044 && this.client.getLocalPlayer().getOverheadIcon() != HeadIcon.MAGIC) {
         this.zulrah = npc;
         this.zulrahNpc = npc;
         this.activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);
      }

   }

   private static void setHidden(Renderable renderable, boolean hidden) {
      Method setHidden = null;

      try {
         setHidden = renderable.getClass().getMethod("setHidden", Boolean.TYPE);
      } catch (NoSuchMethodException var5) {
         return;
      }

      try {
         setHidden.invoke(renderable, hidden);
      } catch (InvocationTargetException | IllegalAccessException var4) {
      }

   }

   @Subscribe
   private void onGameObjectSpawned(GameObjectSpawned event) {
      if (this.zulrahNpc != null) {
         GameObject obj = event.getGameObject();
         if (obj.getId() == 11700) {
            this.toxicCloudsMap.put(obj, 30);
         }

      }
   }

   @Nullable
   private ZulrahPhase getCurrentPhase(RotationType type) {
      return this.stage >= type.getZulrahPhases().size() ? null : (ZulrahPhase)type.getZulrahPhases().get(this.stage);
   }

   @Nullable
   private ZulrahPhase getNextPhase(RotationType type) {
      return this.isLastPhase(type) ? null : (ZulrahPhase)type.getZulrahPhases().get(this.stage + 1);
   }

   private boolean isLastPhase(RotationType type) {
      return this.stage == type.getZulrahPhases().size() - 1;
   }

   public Set<ZulrahData> getZulrahData() {
      LinkedHashSet<ZulrahData> zulrahDataSet = new LinkedHashSet();
      if (this.currentRotation == null) {
         this.potentialRotations.forEach(type -> zulrahDataSet.add(new ZulrahData(this.getCurrentPhase(type), this.getNextPhase(type))));
      } else {
         zulrahDataSet.add(new ZulrahData(this.getCurrentPhase(this.currentRotation), this.getNextPhase(this.currentRotation)));
      }

      return (Set<ZulrahData>)(zulrahDataSet.size() > 0 ? zulrahDataSet : Collections.emptySet());
   }

   public NPC getZulrahNpc() {
      return this.zulrahNpc;
   }

   public int getPhaseTicks() {
      return this.phaseTicks;
   }

   public int getAttackTicks() {
      return this.attackTicks;
   }

   public RotationType getCurrentRotation() {
      return this.currentRotation;
   }

   public Map<LocalPoint, Integer> getProjectilesMap() {
      return this.projectilesMap;
   }

   public Map<GameObject, Integer> getToxicCloudsMap() {
      return this.toxicCloudsMap;
   }

   public static boolean isFlipStandLocation() {
      return flipStandLocation;
   }

   public static boolean isFlipPhasePrayer() {
      return flipPhasePrayer;
   }

   public static boolean isZulrahReset() {
      return zulrahReset;
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

   private long sleepDelay() {
      return this.utils
         .randomDelay(
            this.configph.sleepWeightedDistribution(),
            this.configph.sleepMin(),
            this.configph.sleepMax(),
            this.configph.sleepDeviation(),
            this.configph.sleepTarget()
         );
   }

   private int tickDelay() {
      return (int)this.utils
         .randomDelay(
            this.configph.tickDelayWeightedDistribution(),
            this.configph.tickDelayMin(),
            this.configph.tickDelayMax(),
            this.configph.tickDelayDeviation(),
            this.configph.tickDelayTarget()
         );
   }

   List<WorldPoint> getObstacles() {
      return this.obstacles;
   }

   public Map<LocalPoint, Projectile> getPoisonProjectiles() {
      return this.poisonProjectiles;
   }

   static {
      ZULRAH_IMAGES[0] = ImageUtil.getResourceStreamFromClass(AZulrahPlugin.class, "zulrah_range.png");
      ZULRAH_IMAGES[1] = ImageUtil.getResourceStreamFromClass(AZulrahPlugin.class, "zulrah_melee.png");
      ZULRAH_IMAGES[2] = ImageUtil.getResourceStreamFromClass(AZulrahPlugin.class, "zulrah_magic.png");
   }
}
