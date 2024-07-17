package me.blackout.orcrist.features.module.combat;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.misc.Maths;
import me.blackout.orcrist.utils.misc.PacketManager;
import me.blackout.orcrist.utils.player.CombatHelper;
import me.blackout.orcrist.utils.player.DamageCalculator;
import me.blackout.orcrist.utils.player.ItemHelper;
import me.blackout.orcrist.utils.world.BlockHelper;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.*;

public class BedBomb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutomation = settings.createGroup("Automations");
    private final SettingGroup sgInventory = settings.createGroup("Inventory");
    private final SettingGroup sgAutoCraft = settings.createGroup("Auto Craft");
    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<HandInteraction> interactHand = sgGeneral.add(new EnumSetting.Builder<HandInteraction>().name("Hand").description("Which hand to interact beds with.").defaultValue(HandInteraction.Mainhand).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("Delay to placing and breaking bed.").defaultValue(10).sliderRange(0, 20).build());
    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder().name("strict-direction").description("Places to the only direction you are facing at.").defaultValue(true).build());
    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder().name("predict-movement").description("Predicts movement of targets.").defaultValue(true).build());
    private final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder().name("ignore-terrain").description("Ignores terrain.").defaultValue(true).build());
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").description("Range to place/break beds.").defaultValue(5).sliderRange(1, 10).build());
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("Range to targeting targets.").defaultValue(5).sliderRange(1, 10).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("priority").description("Decides which target to prioritize.").defaultValue(SortPriority.LowestDistance).build());
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder().name("min-damage").description("Minimum damage to deal to target.").defaultValue(7).sliderRange(0, 36).build());
    private final Setting<Boolean> stabilizeCalculation = sgGeneral.add(new BoolSetting.Builder().name("stabilize-calculation").description("Stabilizes calculation.").defaultValue(true).build());
    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder().name("packet-place").description("Places bed using packet.").defaultValue(true).build());
    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder().name("air-place").description("Whether to place bed at air or not.").defaultValue(true).build());
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").description("Debugs every moment done by BedBomb.").defaultValue(false).build());

    // Automation
    private final Setting<AutomationMode> automationMode = sgAutomation.add(new EnumSetting.Builder<AutomationMode>().name("automation-mode").defaultValue(AutomationMode.Smart).build());
    private final Setting<Boolean> breakBurrow = sgAutomation.add(new BoolSetting.Builder().name("break-burrow").description("Breaks burrow of targets.").defaultValue(true).build());
    private final Setting<Boolean> antiTrap = sgAutomation.add(new BoolSetting.Builder().name("break-self-trap").description("Breaks self trap of targets.").defaultValue(true).build());
    private final Setting<Boolean> breakWeb = sgAutomation.add(new BoolSetting.Builder().name("break-web").description("Breaks web of targets.").defaultValue(true).build());
    private final Setting<Boolean> preventEscape = sgAutomation.add(new BoolSetting.Builder().name("prevent-escape").description("Prevents enemy from escaping.").defaultValue(true).build());
    private final Setting<Boolean> requireHole = sgAutomation.add(new BoolSetting.Builder().name("require-hole").description("Requires target to be in hole to perform any automation.").defaultValue(false).build());
    private final Setting<Boolean> disableOnNoBeds = sgAutomation.add(new BoolSetting.Builder().name("disable-on-no-beds").description("Disables BedBomb if there is no beds.").defaultValue(true).visible(() -> canBeVisible()).build());

    // Inventory
    private final Setting<Boolean> autoMove = sgInventory.add(new BoolSetting.Builder().name("auto-move").description("Automatically moves the item.").defaultValue(true).build());
    private final Setting<Integer> bedSlot = sgInventory.add(new IntSetting.Builder().name("bed-slot").description("Slot to move beds at.").defaultValue(5).sliderRange(1, 9).build());
    private final Setting<Boolean> moveTable = sgInventory.add(new BoolSetting.Builder().name("move-table").description("Moves crafting table as well.").defaultValue(true).build());
    private final Setting<Integer> tableSlot = sgInventory.add(new IntSetting.Builder().name("table-slot").description("Slot to move table at.").defaultValue(4).sliderRange(1, 9).build());
    private final Setting<Boolean> antiDesync = sgInventory.add(new BoolSetting.Builder().name("anti-desync").description("Prevents you from de-syncing your inventory.").defaultValue(true).build());

    // Auto Craft
    private final Setting<Boolean> craft = sgAutoCraft.add(new BoolSetting.Builder().name("craft").description("Automatically crafts beds.").defaultValue(true).build());
    private final Setting<Boolean> autoCraft = sgAutoCraft.add(new BoolSetting.Builder().name("auto-craft").description("Automatically places crafting table and crafts beds for you.").defaultValue(true).build());
    private final Setting<Double> radius = sgAutoCraft.add(new DoubleSetting.Builder().name("radius").description("Radius to which the auto craft places the crafting table.").defaultValue(4).sliderRange(1, 5).build());
    private final Setting<Integer> startAt = sgAutoCraft.add(new IntSetting.Builder().name("start-at").description("When to start crafting at.").defaultValue(1).sliderRange(0, 36).build());
    private final Setting<Boolean> requireTotem = sgAutoCraft.add(new BoolSetting.Builder().name("require-totem").description("Crafts only when you are holding a totem").defaultValue(true).build());
    private final Setting<Boolean> closeAfter = sgAutoCraft.add(new BoolSetting.Builder().name("close-after").description("Close the crafting GUI after filling.").defaultValue(true).build());
    private final Setting<Boolean> onlyHole = sgAutoCraft.add(new BoolSetting.Builder().name("only-hole").description("Only crafts when you are in hole.").defaultValue(true).build());
    private final Setting<Integer> emptySlotsNeeded = sgAutoCraft.add(new IntSetting.Builder().name("required-empty-slots").description("How many empty slots are required for activation.").defaultValue(5).min(1).build());
    private final Setting<Integer> stopAt = sgAutoCraft.add(new IntSetting.Builder().name("stop-at").description("When to stop crafting at.").defaultValue(6).sliderRange(0, 36).build());
    private final Setting<Boolean> craftInfo = sgAutoCraft.add(new BoolSetting.Builder().name("craft-info").description("Shows information about crafting.").defaultValue(true).build());

    // Safety
    private final Setting<Boolean> antiSuicide = sgSafety.add(new BoolSetting.Builder().name("anti-suicide").description("Prevents you taking damage from the beds placed.").defaultValue(true).build());
    private final Setting<Double> maxDamage = sgSafety.add(new DoubleSetting.Builder().name("max-damage").description("Maximum damage to deal to yourself.").defaultValue(7).sliderRange(0, 36).build());
    private final Setting<Boolean> pauseOnHealth = sgSafety.add(new BoolSetting.Builder().name("pause-on-health").defaultValue(true).build());
    private final Setting<Boolean> toggleOnHealth = sgSafety.add(new BoolSetting.Builder().name("toggle-on-health").description("Toggles if the threshold is reached.").defaultValue(false).build());
    private final Setting<Integer> threshold = sgSafety.add(new IntSetting.Builder().name("threshold").defaultValue(10).sliderRange(1, 36).visible(() -> pauseOnHealth.get() || toggleOnHealth.get()).build());

    // Pause
    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());
    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").defaultValue(true).build());
    private final Setting<Boolean> acPause = sgPause.add(new BoolSetting.Builder().name("pause-on-ac").defaultValue(true).build());
    private final Setting<Boolean> craftPause = sgPause.add(new BoolSetting.Builder().name("pause-on-craft").defaultValue(true).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay where bed are going to be placed at.").defaultValue(true).build());
    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("Which rendering mode to use to render the bed.").defaultValue(RenderMode.Accurate).build());
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Renders an swinging animation upon placing or breaking bed.").defaultValue(true).build());
    private final Setting<Boolean> renderAutomation = sgRender.add(new BoolSetting.Builder().name("render-automation").description("Renders automations.").defaultValue(true).build());
    private final Setting<Boolean> renderDamage = sgRender.add(new BoolSetting.Builder().name("render-damage").description("Renders damage on the render.").defaultValue(true).build());
    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder().name("damage-scale").description("How big the damage text should be.").defaultValue(1.25).min(1).sliderMax(4).visible(renderDamage :: get).build());
    private final Setting<Integer> renderTicks = sgRender.add(new IntSetting.Builder().name("render-ticks").description("How many ticks to keep the render up.").defaultValue(8).sliderRange(0, 20).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> automationSideColor = sgRender.add(new ColorSetting.Builder().name("automation-side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232, 5)).visible(renderAutomation :: get).build());
    private final Setting<SettingColor> automationLineColor = sgRender.add(new ColorSetting.Builder().name("automation-line-color").description("Line color for line mode.").defaultValue(new SettingColor(197, 137, 232)).visible(renderAutomation :: get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232, 5)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("Line color for line mode.").defaultValue(new SettingColor(197, 137, 232)).build());

    private int webTimer, automationTimer;
    private int placeTimer, breakTimer;
    private double damage;

    private BlockPos selfTrapPos;
    private boolean alerted, startedRefill = false;
    private boolean refilled = false;
    private long calc;

    private CardinalDirection direction;
    private PlayerEntity target;
    private Hand hand;

    private BlockPos placePos, breakPos;
    private BlockPos.Mutable damagePos = new BlockPos.Mutable();

    public BedBomb() {
        super(OrcristAddon.Combat, "bed-bomb", "Automatically places and breaks bed.");
    }

    @Override
    public void onActivate() {
        target = null;

        placePos = null;
        breakPos = null;

        direction = CardinalDirection.North;

        selfTrapPos = null;
        alerted = false;

        automationTimer = 0;
        placeTimer = 0;
        webTimer = 0;
        breakTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (TargetUtils.isBadTarget(target, targetRange.get())) target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());

        //renderBlocks.forEach(RenderBlock::tick);

        if (PlayerUtils.shouldPause(false, eatPause.get(), drinkPause.get())) return;
        if (Modules.get().get(AutoCrystal.class).isActive() && acPause.get()) return;
        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler && craftPause.get()) return;
        if (PlayerUtils.getTotalHealth() <= threshold.get() && pauseOnHealth.get()) return;
        if (mc.world.getDimension().bedWorks()) {
            info("Beds don't work here, disabling... (highlight)%s (default)", Formatting.LIGHT_PURPLE, Formatting.BOLD);
            toggle();
            return;
        }

        if (interactHand.get() == HandInteraction.Mainhand) hand = Hand.MAIN_HAND; else hand = Hand.OFF_HAND;

        if (autoMove.get()) {
            FindItemResult tables = InvUtils.find(Items.CRAFTING_TABLE);
            FindItemResult beds = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
            if (beds.found()) InvUtils.move().from(beds.slot()).to(bedSlot.get() - 1);
            if (moveTable.get() && tables.found()) InvUtils.move().from(tables.slot()).to(tableSlot.get() - 1);
        }

        if (disableOnNoBeds.get() && !craft.get()) {
            FindItemResult beds = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
            if (!beds.found()) {
                info("You've run out of beds, disabling...");
                toggle();
                return;
            }
        }

        if (target == null) {
            placePos = null;
            breakPos = null;
            return;
        }

        breakPos = getBreakPos();

        if (breakPos == null) {
            placePos = getPlacePos();
            breakPos = getBreakPos();
        }

        // Automations
        if (antiTrap.get()) {
            if (automationTimer >= delay.get()) {
                mineSelfTrap(); automationTimer = 0;
            } else  automationTimer++;
        }

        if (preventEscape.get() && BlockHelper.getBlock(target.getBlockPos().up(2)) != Blocks.OBSIDIAN && CombatHelper.isInHole(target, true) && !CombatHelper.isFaceTrapped(target, true)) {
            FindItemResult obby = InvUtils.find(Items.OBSIDIAN);
            if (obby.found()) BlockUtils.place(target.getBlockPos().up(2), obby, true, 50, true, true, true);
            if (BlockHelper.getBlock(target.getBlockPos().up(2)) != Blocks.OBSIDIAN) return;
        }

        if (breakBurrow.get()) {
            if (automationTimer >= delay.get()) {
                mineBurrow(); automationTimer = 0;
            } else automationTimer++;
        }

        if (breakWeb.get()) {
            if (automationTimer >= delay.get()) {
                mineWeb(); automationTimer = 0;
            } else automationTimer++;
        }

        // BedBombing
        if (CombatHelper.isInHole(target, true)) {

            if (breakTimer >= delay.get()) {
                breakBed(breakPos);
                breakTimer = 0;
            } else breakTimer++;

            placeBed(placePos, packetPlace.get());

        } else {

            if (placeTimer >= delay.get()) {
                placeBed(placePos, packetPlace.get());
                placeTimer = 0;
            } else placeTimer++;

            breakBed(breakPos);
        }

    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (craft.get()) {
            if (autoCraft.get() && isOutOfMaterial() && !alerted) {
                error("Cannot activate auto mode, no material left.");
                alerted = true;
            }
            if (autoCraft.get() && needsRefill() && canCraft() && !isOutOfMaterial() && !(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
                FindItemResult craftTable = InvUtils.findInHotbar(Items.CRAFTING_TABLE);
                if (!craftTable.found()) {
                    toggle();
                    error("No crafting tables found in hotbar!");
                    return;
                }
                if (debug.get()) info("Searching for nearby crafting tables");
                BlockPos tablePos;
                tablePos = findCraftingTable();
                if (tablePos == null) {
                    if (debug.get()) info("No crafting table nearby, placing table and returning.");
                    placeCraftingTable(craftTable);
                    return;
                }
                if (debug.get()) info("Located usable crafting table, opening and refilling");
                openCraftingTable(tablePos);
                if (craftInfo.get() && !startedRefill) {
                    info("Refilling...");
                    startedRefill = true;
                }
                refilled = true;
                return;
            }
            if (refilled && !needsRefill()) {
                if (craftInfo.get()) info("Refill complete.");
                refilled = false;
                startedRefill = false;
                if (debug.get()) info("Automatic finished.");
            }

            if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
                if (!canCraft()) {
                    if (debug.get()) info("Cancelling current refill because canRefill is false");
                    mc.player.closeHandledScreen();
                    if (antiDesync.get()) mc.player.getInventory().updateItems();
                    return;
                }

                if (isOutOfMaterial()) {
                    if (craftInfo.get() && !alerted) {
                        error("You are out of material!");
                        alerted = true;
                    }
                    mc.player.closeHandledScreen();
                    if (antiDesync.get()) mc.player.getInventory().updateItems();
                    return;
                }
                if (isInventoryFull()) {
                    if (closeAfter.get()) {
                        mc.player.closeHandledScreen();
                        if (antiDesync.get()) mc.player.getInventory().updateItems();
                    }
                    if (craftInfo.get() && !autoCraft.get()) info("Your inventory is full.");
                    return;
                }

                if (mc.player.currentScreenHandler instanceof CraftingScreenHandler CSH) {
                    List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getResultsForGroup(RecipeBookGroup.CRAFTING_MISC);
                    for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
                        for (RecipeEntry<?> recipe : recipeResultCollection.getRecipes(true)) {
                            if (recipe.value().getResult(mc.world.getRegistryManager()).getItem() instanceof BedItem) {
                                assert mc.interactionManager != null;
                                mc.interactionManager.clickRecipe(CSH.syncId, recipe, false);
                                click(CSH, 0, SlotActionType.QUICK_MOVE, 1);
                            }
                        }
                    }
                }
            }

            // Main crafter
            if (mc.player.currentScreenHandler instanceof CraftingScreenHandler CSH) {
                List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getResultsForGroup(RecipeBookGroup.CRAFTING_MISC);
                for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
                    for (RecipeEntry<?> recipe : recipeResultCollection.getRecipes(true)) {
                        if (recipe.value().getResult(mc.world.getRegistryManager()).getItem() instanceof BedItem) {
                            assert mc.interactionManager != null;
                            mc.interactionManager.clickRecipe(CSH.syncId, recipe, false);
                            click(CSH, 0, SlotActionType.QUICK_MOVE, 1);
                        }
                    }
                }
            }
        }
    }

    // Setting Position
    private BlockPos getPlacePos() {
        if (!InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) return null;

        calc = System.currentTimeMillis();
        if (isGoodTarget()) {
            for (int index = 0; index < 4; index++) {
                int i = index == 0 ? 1 : index == 1 ? 0 : 2;

                if (CombatHelper.isInHole(target, true)) {
                    for (CardinalDirection dir : CardinalDirection.values()) {
                        if (strictDirection.get()
                            && dir.toDirection() != mc.player.getHorizontalFacing()
                            && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;

                        BlockPos centerPos = target.getBlockPos().up(i);

                        double headSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos));
                        double offsetSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos.offset(dir.toDirection())));

                        damage = DamageCalculator.explosionDamage(target, new Vec3d(centerPos.getX(), centerPos.getY(), centerPos.getZ()), 10f, predictMovement.get());

                        if (!airPlace.get() && !BlockHelper.isSolid(centerPos.down().offset(dir.toDirection())))
                            return null;

                        if (mc.world.getBlockState(centerPos).isReplaceable()
                            && BlockUtils.canPlace(centerPos.offset(dir.toDirection()))
                            && DamageCalculator.explosionDamage(target, new Vec3d(centerPos.getX(), centerPos.getY(), centerPos.getZ()), 10f, predictMovement.get()) >= minDamage.get()
                            && PlayerUtils.distanceTo(centerPos.offset(dir.toDirection())) <= range.get()
                            && offsetSelfDamage < maxDamage.get()
                            && headSelfDamage < maxDamage.get()
                            && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - headSelfDamage > 0)
                            && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0)) {
                            if (debug.get()) info("Calculated last place pos in " + Maths.millisElapsed(calc));
                            return centerPos.offset((direction = dir).toDirection());
                        }
                    }
                } else {
                    for (CardinalDirection dir : CardinalDirection.values()) {
                        if (strictDirection.get()
                            && dir.toDirection() != mc.player.getHorizontalFacing()
                            && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;

                        List<BlockPos> Apos = BlockHelper.getSphere(mc.player.getBlockPos(), range.get(), range.get());

                        for (BlockPos MPos : Apos) {
                            BlockPos centerPos = MPos;

                            double headSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos));
                            double offsetSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos));

                            damage = DamageCalculator.explosionDamage(target, new Vec3d(centerPos.getX(), centerPos.getY(), centerPos.getZ()), 10f, predictMovement.get());

                            if (!airPlace.get() && !BlockHelper.isSolid(centerPos.down())) return null;

                            if (mc.world.getBlockState(centerPos).isReplaceable()
                                && BlockUtils.canPlace(centerPos)
                                && DamageCalculator.explosionDamage(target, new Vec3d(centerPos.getX(), centerPos.getY(), centerPos.getZ()), 10f, predictMovement.get()) >= minDamage.get()
                                && PlayerUtils.distanceTo(centerPos) <= range.get()
                                && offsetSelfDamage < maxDamage.get()
                                && headSelfDamage < maxDamage.get()
                                && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - headSelfDamage > 0)
                                && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0)) {
                                if (debug.get()) info("Calculated last place pos in " + Maths.millisElapsed(calc));
                                return centerPos;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isGoodTarget() {
        return !TargetUtils.isBadTarget(target, targetRange.get());
    }

    private BlockPos getBreakPos() {
        if (target == null) return null;
        double currentHP = PlayerUtils.getTotalHealth();

        for (BlockEntity be : Utils.blockEntities()) {
            if (be instanceof BedBlockEntity bed) {
                BlockPos bedPos = new BlockPos(bed.getPos());

                if (!(BlockHelper.getBlock(bedPos) instanceof BedBlock)) continue;

                if (PlayerUtils.distanceTo(bedPos) > targetRange.get()) continue;
                Vec3d vec = Utils.vec3d(bedPos);

                double selfDMG = DamageUtils.bedDamage(mc.player, vec);

                if (selfDMG > maxDamage.get()) continue;

                if (antiSuicide.get() && (currentHP - selfDMG) <= 0) continue;
                return bedPos;
            }
        }
        return null;
    }

    private boolean placeBed(BlockPos pos, boolean packetPlace) {
        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if (pos == null) return false;
        Vec3d hitPos = Vec3d.ofCenter(pos);

        double yaw = switch (direction) {
            case East -> 90;
            case South -> 180;
            case West -> -90;
            default -> 0;
        };

        Rotations.rotate(yaw, Rotations.getPitch(pos), () -> {
            if (packetPlace && !(BlockHelper.getBlock(pos) instanceof BedBlock)) {
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(hitPos, Direction.UP, pos, false), 0));
                mc.player.swingHand(hand);
                InvUtils.swap(bed.slot(), true);
            } else {
                BlockUtils.place(pos, bed, false, 0, swing.get(), true);
            }
            breakPos = pos;
        });

        return true;
    }

    private boolean breakBed(BlockPos pos) {
        if (pos == null) return false;
        if (BlockHelper.getBlock(pos) instanceof BedBlock) {
            if (PlayerUtils.distanceTo(pos) > range.get()) return false;
            if (interactHand.get() == HandInteraction.Mainhand) hand = Hand.MAIN_HAND; else hand = Hand.OFF_HAND;
            PacketManager.interact(pos, hand);
        }
        return true;
    }

    // Automations
    private void mineBurrow() {
        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() instanceof PickaxeItem);
        if (pickaxe.found() && CombatHelper.isBurrowed(target, requireHole.get())) {
            PacketManager.updateSlot(pickaxe.slot());
            PacketManager.sendPacketMine(target.getBlockPos(), swing.get());
            info("Mining " + target.getName().toString() + "'s Burrow!");
            return;
        }
    }

    private void mineSelfTrap() {
        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() instanceof PickaxeItem);
        //BlockPos SelfTrapPos = CombatHelper.getSelfTrapBlock(target, preventEscape.get());
        if (pickaxe.found() && shouldTrapMine() && !didTrapMine()) {
            BlockPos selfTrapPos = CombatHelper.getSelfTrapBlock(target, preventEscape.get());
            PacketManager.updateSlot(pickaxe.slot());
            PacketManager.sendPacketMine(selfTrapPos, swing.get());
            info("Mining " + target.getName().toString() + "'s Self Trap!");
        }
    }

    private void mineWeb() {
        FindItemResult sword = InvUtils.find(itemStack -> itemStack.getItem() instanceof SwordItem);
        BlockPos pos = target.getBlockPos();
        if (automationMode.get() == AutomationMode.Normal) {
            if (sword.found() && CombatHelper.isWebbed(target)) {
                PacketManager.updateSlot(sword.slot());
                PacketManager.sendPacketMine(target.getBlockPos().up(), swing.get());
                if (webTimer <= 0) {
                    info("Mining " + target.getName().toString() + "'s web!");
                    webTimer = 100;
                } else {
                    webTimer--;
                }
            }
        } else if (automationMode.get() == AutomationMode.Smart) {
            if (BlockHelper.getBlock(pos.up()) == Blocks.TRIPWIRE) {
                if (webTimer <= 0) { info("Mining " + target.getName().toString() + "'s String!"); webTimer = 100; } else webTimer--;
                BlockHelper.mine(pos.up(), true);
            }
            if (BlockHelper.getBlock(pos.up()) == Blocks.COBWEB && sword.found()) {
                if (webTimer <= 0) { info("Mining " + target.getName().toString() + "'s web!"); webTimer = 100; } else webTimer--;
                PacketManager.updateSlot(sword.slot());
                BlockHelper.mine(pos.up(), true);
            }
        }
    }

    private boolean shouldTrapMine() {
        return placePos == null && CombatHelper.getSelfTrapBlock(target, preventEscape.get()) != null;
    }

    private boolean didTrapMine() {
        if (CombatHelper.getSelfTrapBlock(target, preventEscape.get()) == null) return true;
        return BlockHelper.getBlock(selfTrapPos) == Blocks.AIR || !CombatHelper.isTrapBlock(selfTrapPos);
    }

    // Crafting utils
    private boolean canCraft() {
        if (requireTotem.get() && mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING || mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return true;
        if (onlyHole.get() && PlayerUtils.isInHole(true)) return true;
        if (!PlayerUtils.isMoving()) return true;
        if (getEmptySlots() < emptySlotsNeeded.get()) return false;
        if (!isInventoryFull()) return true;
        return false;
    }

    private Integer getEmptySlots() {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) emptySlots++;
        }
        return emptySlots;
    }
    private boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) return false;
        }
        return true;
    }

    private boolean isOutOfMaterial() {
        FindItemResult wool = InvUtils.find(itemStack -> ItemHelper.wools.contains(itemStack.getItem()));
        FindItemResult plank = InvUtils.find(itemStack -> ItemHelper.planks.contains(itemStack.getItem()));
        FindItemResult craftTable = InvUtils.find(Items.CRAFTING_TABLE);
        if (!craftTable.found()) return true;
        if (!wool.found() || !plank.found()) return true;
        return wool.count() < 3 || plank.count() < 3;
    }

    private void click(ScreenHandler container, int slot, SlotActionType action, int clickData) {
        assert mc.interactionManager != null;
        mc.interactionManager.clickSlot(container.syncId, slot, clickData, action, mc.player);
    }

    private void openCraftingTable(BlockPos tablePos) {
        Vec3d tableVec = new Vec3d(tablePos.getX(), tablePos.getY(), tablePos.getZ());
        BlockHitResult table = new BlockHitResult(tableVec, Direction.UP, tablePos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, table);
    }

    private void placeCraftingTable(FindItemResult craftTable) {
        List<BlockPos> nearbyBlocks = BlockHelper.getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) {
            if (BlockHelper.getBlock(block) == Blocks.AIR) {
                BlockUtils.place(block, craftTable, 0, true);
                break;
            }
        }
    }

    private boolean needsRefill() {
        FindItemResult beds = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if (beds.slot() <= startAt.get()) return true;
        if (beds.slot() >= stopAt.get()) return false;
        return false;
    }

    private BlockPos findCraftingTable() {
        List<BlockPos> nearbyBlocks = BlockHelper.getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) if (BlockHelper.getBlock(block) == Blocks.CRAFTING_TABLE) return block;
        return null;
    }

    private boolean canBeVisible() {
        return !craft.get();
    }

//    @EventHandler
//    private void onRender(Render3DEvent event) {
//        if (!render.get()) return;
//        renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
//        renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
//
//        if (renderAutomation.get() && target != null) {
//            if (selfTrapPos != null) event.renderer.box(selfTrapPos, automationSideColor.get(), automationLineColor.get(), shapeMode.get(), 0);
//            if (CombatHelper.isBurrowed(target, requireHole.get())) event.renderer.box(target.getBlockPos(), automationSideColor.get(), automationLineColor.get(), shapeMode.get(), 0);
//            if (CombatHelper.isWeb(target.getBlockPos().up())) event.renderer.box(target.getBlockPos().up(), automationSideColor.get(), automationLineColor.get(), shapeMode.get(), 0);
//        }
//    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!renderDamage.get() || damagePos == null) return;

        if (BlockHelper.getBlock(damagePos) instanceof BedBlock) {
            Vector3d vec3 = new Vector3d(damagePos.getX(), damagePos.getY(), damagePos.getZ());
            if (NametagUtils.to2D(vec3, damageTextScale.get())) {
                NametagUtils.begin(vec3);
                TextRenderer.get().begin(1, false, true);

                String text = String.format("%.1f", damage);
                double w = TextRenderer.get().getWidth(text) / 2;
                TextRenderer.get().render(text, -w, 0, lineColor.get(), true);

                TextRenderer.get().end();
                NametagUtils.end();
            }
        }
    }

    public enum HandInteraction {
        Mainhand, Offhand
    }

    public enum AutomationMode {
        Normal, Smart
    }

    public enum RenderMode {
        Normal, Accurate, Box
    }
}
