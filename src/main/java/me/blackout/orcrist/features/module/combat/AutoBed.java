//package me.blackout.orcrist.features.module.combat;
//
//import me.blackout.orcrist.OrcristAddon;
//import me.blackout.orcrist.utils.experimental.Mode;
//import me.blackout.orcrist.utils.experimental.Origin;
//import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
//import meteordevelopment.meteorclient.events.render.Render2DEvent;
//import meteordevelopment.meteorclient.events.render.Render3DEvent;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.renderer.ShapeMode;
//import meteordevelopment.meteorclient.renderer.text.TextRenderer;
//import meteordevelopment.meteorclient.settings.*;
//import meteordevelopment.meteorclient.systems.modules.Module;
//import meteordevelopment.meteorclient.systems.modules.Modules;
//import meteordevelopment.meteorclient.systems.modules.combat.SelfTrap;
//import meteordevelopment.meteorclient.systems.modules.combat.Surround;
//import meteordevelopment.meteorclient.systems.modules.movement.Blink;
//import meteordevelopment.meteorclient.utils.Utils;
//import meteordevelopment.meteorclient.utils.entity.EntityUtils;
//import meteordevelopment.meteorclient.utils.entity.SortPriority;
//import meteordevelopment.meteorclient.utils.player.FindItemResult;
//import meteordevelopment.meteorclient.utils.player.InvUtils;
//import meteordevelopment.meteorclient.utils.player.PlayerUtils;
//import meteordevelopment.meteorclient.utils.player.Rotations;
//import meteordevelopment.meteorclient.utils.render.NametagUtils;
//import meteordevelopment.meteorclient.utils.render.color.Color;
//import meteordevelopment.meteorclient.utils.render.color.SettingColor;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.block.*;
//import net.minecraft.block.entity.BedBlockEntity;
//import net.minecraft.block.entity.BlockEntity;
//import net.minecraft.client.gui.screen.ingame.CraftingScreen;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.item.BedItem;
//import net.minecraft.item.BlockItem;
//import net.minecraft.item.Item;
//import net.minecraft.item.Items;
//import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
//import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
//import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
//import net.minecraft.util.Hand;
//import net.minecraft.util.hit.BlockHitResult;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Direction;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.util.math.Vec3i;
//
//import java.util.*;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//public class AutoBed extends Module {
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//    private final SettingGroup sgPlace = settings.createGroup("Place");
//    private final SettingGroup sgBreak = settings.createGroup("Break");
//    private final SettingGroup sgRotations = settings.createGroup("Rotations");
//    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
//    private final SettingGroup sgPause = settings.createGroup("Pause");
//    private final SettingGroup sgMisc = settings.createGroup("Misc");
//    private final SettingGroup sgCraft = settings.createGroup("Auto Craft");
//    private final SettingGroup sgAuto = settings.createGroup("Automation");
//    private final SettingGroup sgExperimental = settings.createGroup("Experimental");
//    private final SettingGroup sgRender = settings.createGroup("Render");
//
//    // General
//    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").description("Range to target players.").defaultValue(5).sliderRange(0.0, 7).build());
//    private final Setting<Double> wallsRange = sgGeneral.add(new DoubleSetting.Builder().name("walls-range").description("IDK").defaultValue(4).sliderRange(1, 5).build());
//    private final Setting<Boolean> xymb = sgGeneral.add(new BoolSetting.Builder().name("xymb").defaultValue(false).build());
//    private final Setting<Origin> placeOrigin = sgGeneral.add(new EnumSetting.Builder<Origin>().name("place-origin").defaultValue(Origin.NCP).build());
//
//    // Place
//    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder().name("place").description("Places bed block.").build());
//    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder().name("place-delay").description("Delay to place beds.").defaultValue(1).sliderRange(0, 10).build());
//    private final Setting<Integer> holePlaceDelay = sgPlace.add(new IntSetting.Builder().name("hole-place-delay").description("IDK").defaultValue(1).sliderRange(0, 10).build());
//    private final Setting<Double> minPlaceDamage = sgPlace.add(new DoubleSetting.Builder().name("min-place-damage").description("Minimum amount of damage that can be done when placing.").defaultValue(6).sliderRange(1, 36).build());
//    private final Setting<Double> maxSelfPlaceDamage = sgPlace.add(new DoubleSetting.Builder().name("max-self-damage").description("Maximum amount of damage that can be done to you.").defaultValue(4).sliderRange(0, 36).build());
//    private final Setting<Boolean> noSuicidePlace = sgPlace.add(new BoolSetting.Builder().name("no-suicide-place").description("idk").defaultValue(false).build());
//
//    // Break
//    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder().name("place-delay").description("Delay to place beds.").defaultValue(1).sliderRange(0, 10).build());
//    private final Setting<Integer> holeBreakDelay = sgBreak.add(new IntSetting.Builder().name("hole-break-delay").description("IDK").defaultValue(1).sliderRange(0, 10).build());
//    private final Setting<Double> minBreakDamage = sgBreak.add(new DoubleSetting.Builder().name("min-break-damage").description("Minimum amount of damage that can be done when placing.").defaultValue(6).sliderRange(1, 36).build());
//    private final Setting<Double> maxSelfBreakDamage = sgBreak.add(new DoubleSetting.Builder().name("max-self-damage").description("Maximum amount of damage that can be done to you.").defaultValue(4).sliderRange(0, 36).build());
//    private final Setting<Boolean> noSuicideBreak = sgBreak.add(new BoolSetting.Builder().name("no-suicide-break").description("idk").defaultValue(false).build());
//
//    // Rotations
//    private final Setting<Boolean> strictDirection = sgRotations.add(new BoolSetting.Builder().name("strict-direction").defaultValue(false).build());
//    private final Setting<Boolean> rotateChecks = sgRotations.add(new BoolSetting.Builder().name("rotate-checks").defaultValue(true).build());
//
//    // Targetting
//    private final Setting<Double> targetRange = sgTargeting.add(new DoubleSetting.Builder().name("range").defaultValue(7).sliderRange(1, 17).build());
//    private final Setting<SortPriority> sortPriority = sgTargeting.add(new EnumSetting.Builder<SortPriority>().name("sort-priority").defaultValue(SortPriority.LowestHealth).build());
//    private final Setting<Boolean> onlyHoled = sgTargeting.add(new BoolSetting.Builder().name("only-holed").defaultValue(true).build());
//    private final Setting<Boolean> predict = sgTargeting.add(new BoolSetting.Builder().name("predict").defaultValue(true).build());
//    private final Setting<Boolean> antiFriendPop = sgTargeting.add(new BoolSetting.Builder().name("anti-friend-pop").defaultValue(true).build());
//    private final Setting<Boolean> smartDelay = sgTargeting.add(new BoolSetting.Builder().name("smart-delay").defaultValue(true).build());
//    private final Setting<Integer> hurtTimeThreshold = sgTargeting.add(new IntSetting.Builder().name("hurt-time-threshold").defaultValue(0).visible(smartDelay::get).build());
//    private final Setting<Boolean> netDamage = sgTargeting.add(new BoolSetting.Builder().name("net-damage").build());
//    private final Setting<Integer> maxTargets = sgTargeting.add(new IntSetting.Builder().name("max-targets").defaultValue(1).sliderRange(1, 5).build());
//
//    // Pause
//    private final Setting<Boolean> pauseWhileCrafting = sgPause.add(new BoolSetting.Builder().name("pause-on-craft").build());
//    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").build());
//    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").build());
//    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").build());
//
//    // Misc
//    private final Setting<Boolean> ignoreTerrain = sgMisc.add(new BoolSetting.Builder().name("ignore-terrain").build());
//    private final Setting<Integer> minBedAmount = sgMisc.add(new IntSetting.Builder().name("min-bed-amount").sliderRange(1, 9).build());
//    private final Setting<Boolean> holeTrap = sgMisc.add(new BoolSetting.Builder().name("toxic").build());
//    private final Setting<Boolean> antiVclip = sgMisc.add(new BoolSetting.Builder().name("ultra-toxic").build());
//    private final Setting<Boolean> autoSwitch = sgMisc.add(new BoolSetting.Builder().name("auto-switch").build());
//    private final Setting<Boolean> switchBack = sgMisc.add(new BoolSetting.Builder().name("switch-back").build());
//    private final Setting<Boolean> autoMove = sgMisc.add(new BoolSetting.Builder().name("auto-move").build());
//    private final Setting<Integer> autoMoveSlot = sgMisc.add(new IntSetting.Builder().name("auto-switch-slot").sliderRange(1, 9).build());
//
//    // Craft
//    private final Setting<Boolean> autoCraft = sgCraft.add(new BoolSetting.Builder().name("auto-craft").build());
//    private final Setting<Integer> craftDelay = sgCraft.add(new IntSetting.Builder().name("craft-delay").defaultValue(10).sliderRange(0, 20).build());
//    private final Setting<Boolean> placeTables = sgCraft.add(new BoolSetting.Builder().name("place-tables").build());
//    private final Setting<Double> minCraftHealth = sgCraft.add(new DoubleSetting.Builder().name("min-craft-health").defaultValue(10).sliderRange(0, 36).build());
//    private final Setting<Boolean> craftSafe = sgCraft.add(new BoolSetting.Builder().name("craft-safe").build());
//    private final Setting<Boolean> craftStill = sgCraft.add(new BoolSetting.Builder().name("craft-still").build());
//    private final Setting<Integer> refillThreshold = sgCraft.add(new IntSetting.Builder().name("refill-threshold").defaultValue(3).sliderRange(1, 10).build());
//    private final Setting<Integer> minBedCraft = sgCraft.add(new IntSetting.Builder().name("min-bed-craft").defaultValue(3).sliderRange(1, 10).build());
//
//    // Auto
//    private final Setting<Integer> minMineHealth = sgAuto.add(new IntSetting.Builder().name("min-mine-health").defaultValue(10).sliderRange(1, 36).build());
//    private final Setting<Mode> breakMode = sgAuto.add(new EnumSetting.Builder<Mode>().name("break-mode").defaultValue(Mode.PACKET).build());
//    private final Setting<Boolean> mineSafe = sgAuto.add(new BoolSetting.Builder().name("mine-safe").build());
//    private final Setting<Boolean> mineBurrow = sgAuto.add(new BoolSetting.Builder().name("mine-burrow").build());
//    private final Setting<Boolean> selfTrapMine = sgAuto.add(new BoolSetting.Builder().name("self-trap-mine").build());
//    private final Setting<Boolean> mineHead = sgAuto.add(new BoolSetting.Builder().name("mine-head").build());
//    private final Setting<Boolean> antiAntiBed = sgAuto.add(new BoolSetting.Builder().name("anti-anti-bed").build());
//
//    // Experimental
//    private final Setting<Boolean> oldMode = sgExperimental.add(new BoolSetting.Builder().name("old-mode").build());
//    private final Setting<Boolean> monkeyMode = sgExperimental.add(new BoolSetting.Builder().name("dependant-delays").build());
//    private final Setting<Boolean> noAirPlace = sgExperimental.add(new BoolSetting.Builder().name("no-air-place").build());
//    private final Setting<Boolean> debugText = sgExperimental.add(new BoolSetting.Builder().name("debug-text").build());
//
//    // Render
//    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders stuff.").defaultValue(true).build());
//    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder().name("swing-hand").description("Swing hand client side.").defaultValue(true).build());
//    private final Setting<Integer> maxBedRender = sgRender.add(new IntSetting.Builder().name("max-bed-render").defaultValue(1).sliderRange(1, 10).build());
//    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder().name("renderTime").defaultValue(10).sliderRange(1, 20).build());
//    private final Setting<Boolean> fade = sgRender.add(new BoolSetting.Builder().name("fade").build());
//    private final Setting<Boolean> shrink = sgRender.add(new BoolSetting.Builder().name("shrink").build());
//    private final Setting<Integer> beforeFadeDelay = sgRender.add(new IntSetting.Builder().name("before-fade-or-shrink-delay").defaultValue(5).visible(() -> render.get() && (fade.get() || shrink.get())).sliderRange(0, 10).build());
//    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
//    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232, 10)).build());
//    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232)).build());
//
//    private boolean hasWarnedNoCrafter;
//    private boolean hasWarnedNoMats;
//    private AutoBed.BedData dumbBreak;
//    private AutoBed.BedData dumbPlace;
//    private AutoBed.BedData lastMineTarget;
//    private BlockPos dumbCraft;
//    private PlayerEntity target;
//    private PlayerEntity trapHoldTarget;
//    private int placeDelayLeft;
//    private int breakDelayLeft;
//    private int craftDelayLeft;
//    private int mineTimeLeft;
//    private int prevSlot;
//    private final List<PlayerEntity> targets = new ArrayList();
//    private final List<PlayerEntity> friends = new ArrayList();
//    private Vec3d playerPos;
////    private final List<AutoBed.BedRenderBlock> renderBeds = new ArrayList<>();
////    private final List<RenderBlock> mineBlocks = new ArrayList<>();
//    private ExecutorService executor;
//
//
//    public AutoBed() {
//        super(OrcristAddon.Combat, "auto-bed", "Best Bed Bomb in da game.");
//    }
//
//    @EventHandler(priority = 200)
//    private void onPreTick(TickEvent.Pre event) {
//        if (mc.world.getDimension().bedWorks()) {
//            error("You are in the Overworld... disabling!", new Object[0]);
//            toggle();
//        } else {
//            playerPos = mc.player.getPos();
//            if (Modules.get().isActive(Blink.class)) {
//                playerPos = ((IBlink)Modules.get().get(Blink.class)).getOldPos();
//            }
//
//            PlayerUtils2.collectTargets(
//                targets,
//                friends,
//                targetRange.get(),
//                maxTargets.get(),
//                false,
//                onlyHoled.get(),
//                ignoreTerrain.get(),
//                (SortPriority) sortPriority.get()
//            );
//
//            if (targets.isEmpty()) {
//                target = null;
//            }
//
//            if (placeDelayLeft <= 0) {
//                if (place.get()
//                    && InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).count() >= Math.max(1, minBedAmount.get())
//                    && !shouldPause()
//                    && !targets.isEmpty()) {
//                    if (dumbPlace == null) {
//                        executor.submit(this::doPlaceThreaded);
//                    } else {
//                        placeBed(dumbPlace, true);
//                    }
//
//                    dumbPlace = null;
//                }
//            } else {
//                --placeDelayLeft;
//            }
//
//            if (craftDelayLeft > 0) {
//                --craftDelayLeft;
//            } else {
//                craftDelayLeft = craftDelay.get();
//                if (!cantCraft()) {
//                    if (debugText.get()) {
//                        info("Looking for crafting table.", new Object[0]);
//                    }
//
//                    BlockPos potential = null;
//                    int reach = (int)Math.ceil(range.get());
//
//                    for(int x = -reach; x <= reach; ++x) {
//                        for(int y = -reach; y <= reach; ++y) {
//                            for(int z = -reach; z <= reach; ++z) {
//                                BlockPos blockPos = mc.player.getBlockPos().add(x, y, z);
//                                if (inRange(blockPos) && !BlockUtils2.invalidPos(blockPos)) {
//                                    BlockState state = mc.world.getBlockState(blockPos);
//                                    if (state.getBlock() instanceof CraftingTableBlock) {
//                                        openTable(blockPos);
//                                        return;
//                                    }
//
//                                    if (state.isReplaceable()
//                                        && (!noAirPlace.get() && !oldMode.get() || !BlockUtils2.noSupport(blockPos))
//                                        && (
//                                        potential == null
//                                            || potential.getSquaredDistance(mc.player.getBlockPos()) < blockPos.getSquaredDistance(mc.player.getBlockPos())
//                                    )) {
//                                        potential = blockPos;
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    if (placeTables.get() && potential != null) {
//                        FindItemResult tables = InvUtils.findInHotbar(new Item[]{Items.CRAFTING_TABLE});
//                        if (!tables.found()) {
//                            return;
//                        }
//
//                        if (debugText.get()) {
//                            info("Placing crafting table", new Object[0]);
//                        }
//
//                        BlockUtils2.placeBlock(
//                            tables,
//                            potential,
//                            rotateChecks.get(),
//                            20,
//                            !noAirPlace.get() && !oldMode.get(),
//                            false,
//                            swingHand.get(),
//                            strictDirection.get()
//                        );
//                        dumbCraft = potential;
//                    }
//                }
//            }
//        }
//    }
//
//    @EventHandler(
//        priority = 200
//    )
//    private void onPostTick(TickEvent.Post event) {
//        if (target != null && target.isDead()) {
//            target = null;
//        }
//
//        if (breakDelayLeft <= 0) {
//            if (!pauseAll() && !targets.isEmpty()) {
//                if (dumbBreak == null) {
//                    executor.submit(this::doBreak);
//                } else {
//                    if (debugText.get()) {
//                        info("fast break: " + dumbBreak.placeDirection, new Object[0]);
//                    }
//
//                    if (rotateChecks.get()) {
//                        Rotations.rotate(
//                            Rotations.getYaw(dumbBreak.pos), Rotations.getPitch(dumbBreak.pos), 50, false, () -> breakBed(dumbBreak, true)
//                        );
//                    } else {
//                        breakBed(dumbBreak, true);
//                    }
//                }
//
//                dumbBreak = null;
//            }
//        } else {
//            --breakDelayLeft;
//        }
//
//        //RenderBlock.tick(mineBlocks);
//        //AutoBed.BedRenderBlock.bedTick(renderBeds);
//        if (dumbCraft != null && !cantCraft()) {
//            openTable(dumbCraft);
//            dumbCraft = null;
//        }
//
//        if (lastMineTarget != null && lastMineTarget.pos != null) {
//            if (!inRange(lastMineTarget.pos)
//                || !isGoodTarget(lastMineTarget.target)
//                || breakMode.get() != Mode.INSTANT && mc.world.getBlockState(lastMineTarget.pos).isAir()) {
//                if (lastMineTarget.placeDirection != null && placeDelayLeft <= 0) {
//                    dumbPlace = lastMineTarget;
//                }
//
//                if (prevSlot != -1 && PlayerUtils.getTotalHealth() >= (double)((Integer)minMineHealth.get()).intValue() && !pauseAll()) {
//                    InvUtils.swap(prevSlot, false);
//                }
//
//                if (debugText.get()) {
//                    info("Resetting lastMinePos", new Object[0]);
//                }
//
//                lastMineTarget = null;
//                mineTimeLeft = -1;
//                prevSlot = -1;
//            } else if (!pauseAll() && PlayerUtils.getTotalHealth() >= (double)((Integer)minMineHealth.get()).intValue()) {
//                doBreakSwitch(lastMineTarget.pos);
//            }
//        }
//
//        if (PlayerUtils.getTotalHealth() >= (double)((Integer)minMineHealth.get()).intValue()
//            && !shouldPause()
//            && (!mineSafe.get() || UtilsPlus.isSafe(mc.player))) {
//            if (lastMineTarget != null && (mineTimeLeft >= 0 || breakMode.get() == Mode.INSTANT) && mineTimeLeft >= -10) {
//                if (lastMineTarget.pos != null && PlayerUtils.getTotalHealth() >= (double)((Integer)minMineHealth.get()).intValue() && !pauseAll()) {
//                    BlockState state = mc.world.getBlockState(lastMineTarget.pos);
//                    if (!state.isReplaceable() && !(state.getBlock() instanceof BedBlock) && !state.isOf(Blocks.BEDROCK)) {
//                        --mineTimeLeft;
//                        float[] rotation = PlayerUtils.calculateAngle(
//                            new Vec3d(
//                                (double)lastMineTarget.pos.getX() + 0.5,
//                                (double)lastMineTarget.pos.getY() + 0.5,
//                                (double)lastMineTarget.pos.getZ() + 0.5
//                            )
//                        );
//                        switch((Mode)breakMode.get()) {
//                            case BYPASS:
//                                if (rotateChecks.get()) {
//                                    Rotations.rotate(
//                                        (double)rotation[0],
//                                        (double)rotation[1],
//                                        25,
//                                        () -> mc.interactionManager.updateBlockBreakingProgress(lastMineTarget.pos, BlockUtils2.getClosestDirection(lastMineTarget.pos, false))
//                                    );
//                                } else {
//                                    mc.interactionManager.updateBlockBreakingProgress(lastMineTarget.pos, BlockUtils2.getClosestDirection(lastMineTarget.pos, false));
//                                }
//                                break;
//                            case INSTANT:
//                                if (rotateChecks.get()) {
//                                    Rotations.rotate(
//                                        (double)rotation[0],
//                                        (double)rotation[1],
//                                        25,
//                                        () -> mc
//                                            .getNetworkHandler()
//                                            .sendPacket(
//                                                new PlayerActionC2SPacket(
//                                                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, lastMineTarget.pos, BlockUtils2.getClosestDirection(lastMineTarget.pos, false)
//                                                )
//                                            )
//                                    );
//                                } else {
//                                    mc
//                                        .getNetworkHandler()
//                                        .sendPacket(
//                                            new PlayerActionC2SPacket(
//                                                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, lastMineTarget.pos, BlockUtils2.getClosestDirection(lastMineTarget.pos, false)
//                                            )
//                                        );
//                                }
//
//                                if (mineTimeLeft < 0) {
//                                    mineTimeLeft = 0;
//                                }
//
//                                if (lastMineTarget.placeDirection != null && placeDelayLeft <= 0) {
//                                    dumbPlace = lastMineTarget;
//                                }
//
//                                //RenderBlock.addRenderBlock(mineBlocks, lastMineTarget.pos, 1);
//                        }
//
//                        if (breakMode.get() != Mode.PACKET) {
//                            RandUtils.swing(swing.get());
//                        }
//                    }
//                }
//            } else if (InvUtils.findInHotbar(
//                    itemStack -> itemStack.getItem().equals(Items.DIAMOND_PICKAXE) || itemStack.getItem().equals(Items.NETHERITE_PICKAXE)
//                )
//                .found()) {
//                for(PlayerEntity target : targets) {
//                    if (mineBurrow.get() && UtilsPlus.isBurrowed(target)) {
//                        BlockState state = mc.world.getBlockState(target.getBlockPos());
//                        if (inRange(target.getBlockPos()) && !(state.getBlock() instanceof BedBlock) && !state.isOf(Blocks.BEDROCK)) {
//                            mineTimeLeft = CityUtils.getSpeed(state, target.getBlockPos());
//                            lastMineTarget = new AutoBed.BedData(target.getBlockPos(), null, 10.0F, target);
//                            doBreakSwitch(lastMineTarget.pos);
//                            mineBlock(target.getBlockPos());
//                            //RenderBlock.addRenderBlock(mineBlocks, target.getBlockPos(), mineTimeLeft + 1);
//                            if (debugText.get()) {
//                                info("Burrow mining " + target.getName().getString());
//                            }
//                            break;
//                        }
//                    }
//
//                    if (antiAntiBed.get() && !UtilsPlus.isTrapped(target) && !xymb.get()) {
//                        BlockState state = mc.world.getBlockState(target.getBlockPos().up());
//                        if (inRange(target.getBlockPos().up())
//                            && !state.isReplaceable()
//                            && !(state.getBlock() instanceof BedBlock)
//                            && !state.isOf(Blocks.BEDROCK)) {
//                            mineTimeLeft = CityUtils.getSpeed(state, target.getBlockPos().up());
//                            lastMineTarget = new AutoBed.BedData(target.getBlockPos().up(), null, 10.0F, target);
//                            doBreakSwitch(lastMineTarget.pos);
//                            mineBlock(target.getBlockPos().up());
//                            //RenderBlock.addRenderBlock(mineBlocks, target.getBlockPos().up(), mineTimeLeft + 1);
//                            if (debugText.get()) {
//                                info("Obstruction mining " + target.getName().getString());
//                            }
//                            break;
//                        }
//                    }
//
//                    Surround surround = Modules.get().get(Surround.class);
//                    SelfTrap selftrap = Modules.get().get(SelfTrap.class);
//                    if (selfTrapMine.get()
//                        && !xymb.get()
//                        && (
//                        UtilsPlus.isTrapped(target)
//                            || breakMode.get() == Mode.PACKET
//                            && mc.world.getBlockState(target.getBlockPos().up()).getBlock() instanceof BedBlock
//                    )) {
//                        for(Vec3i city : CityUtils.CITY_WITHOUT_BURROW) {
//                            BlockState state = mc.world.getBlockState(target.getBlockPos().add(city).up());
//                            if ((!surround.isActive() || !surround.getPlacePositions(false).contains(new BlockPos(city)))
//                                && (!selftrap.isActive() || !surround.getPlacePositions(false).contains(new BlockPos(city)))
//                                && !state.isReplaceable()
//                                && !(state.getBlock() instanceof BedBlock)
//                                && !state.isOf(Blocks.BEDROCK)
//                                && inRange(target.getBlockPos().add(city).up())) {
//                                mineTimeLeft = CityUtils.getSpeed(state, target.getBlockPos().add(city).up());
//                                lastMineTarget = new AutoBed.BedData(
//                                    target.getBlockPos().add(city).up(),
//                                    RandUtils.direction(city).getOpposite(),
//                                    getBedDamage(Vec3d.ofCenter(target.getBlockPos().up())),
//                                    target
//                                );
//                                doBreakSwitch(lastMineTarget.pos);
//                                mineBlock(target.getBlockPos().add(city).up());
//                                //RenderBlock.addRenderBlock(mineBlocks, target.getBlockPos().add(city).up(), mineTimeLeft + 1);
//                                if (debugText.get()) {
//                                    info("Self trap mining " + target.getName().getString());
//                                }
//
//                                return;
//                            }
//                        }
//
//                        if (mineHead.get()
//                            && (!surround.isActive() || !surround.getPlacePositions(false).contains(target.getBlockPos().up(2)))
//                            && (!selftrap.isActive() || !surround.getPlacePositions(false).contains(target.getBlockPos().up(2)))) {
//                            BlockState state = mc.world.getBlockState(target.getBlockPos().up(2));
//                            if (!state.isReplaceable()
//                                && !(state.getBlock() instanceof BedBlock)
//                                && !state.isOf(Blocks.BEDROCK)
//                                && inRange(target.getBlockPos().up(2))) {
//                                mineTimeLeft = CityUtils.getSpeed(state, target.getBlockPos().up(2));
//                                lastMineTarget = new AutoBed.BedData(target.getBlockPos().up(2), null, 10.0F, target);
//                                doBreakSwitch(lastMineTarget.pos);
//                                mineBlock(target.getBlockPos().up(2));
//                                //RenderBlock.addRenderBlock(mineBlocks, target.getBlockPos().up(2), mineTimeLeft + 1);
//                                if (debugText.get()) {
//                                    info("Self trap head mining " + target.getName().getString());
//                                }
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private void mineBlock(BlockPos pos) {
//        float[] rotation = PlayerUtils.calculateAngle(
//            new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5)
//        );
//        switch((Mode)breakMode.get()) {
//            case BYPASS:
//                if (rotateChecks.get()) {
//                    Rotations.rotate(
//                        (double)rotation[0], (double)rotation[1], 25, () -> mc.interactionManager.updateBlockBreakingProgress(pos, BlockUtils2.getClosestDirection(pos, false))
//                    );
//                } else {
//                    mc.interactionManager.updateBlockBreakingProgress(pos, BlockUtils2.getClosestDirection(pos, false));
//                }
//                break;
//            case INSTANT:
//                if (rotateChecks.get()) {
//                    Rotations.rotate(
//                        (double)rotation[0],
//                        (double)rotation[1],
//                        25,
//                        () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, BlockUtils2.getClosestDirection(pos, false)))
//                    );
//                } else {
//                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, BlockUtils2.getClosestDirection(pos, false)));
//                }
//                break;
//            default:
//                UtilsPlus.mine(pos, swing.get(), rotateChecks.get());
//        }
//
//        RandUtils.swing(swing.get());
//    }
//
//    private void doPlaceThreaded() {
//        if (placeDelayLeft <= 0) {
//            label254:
//            for(BlockEntity blockEntity : Utils.blockEntities()) {
//                if (blockEntity instanceof BedBlockEntity && !mc.world.getBlockState(blockEntity.getPos()).isAir()) {
//                    Vec3d footPos = new Vec3d(
//                        (double)blockEntity.getPos().getX() + 0.5,
//                        (double)blockEntity.getPos().getY() + 0.5,
//                        (double)blockEntity.getPos().getZ() + 0.5
//                    );
//                    Vec3d headPos = footPos.add(
//                        (double)BedBlock.getDirection(mc.world, blockEntity.getPos()).getOffsetX(),
//                        0.0,
//                        (double)BedBlock.getDirection(mc.world, blockEntity.getPos()).getOffsetZ()
//                    );
//                    if ((inRange(footPos) || inRange(headPos))
//                        && !BedBlock.getBedPart(mc.world.getBlockState(blockEntity.getPos())).equals(BedBloc.FIRST)
//                        && !((double)DamageCalcUtils.explosionDamage(target, headPos, predict.get(), false, false, 5) < minBreakDamage.get())
//                        && !(
//                        (double)DamageCalcUtils.explosionDamage(target, headPos, ignoreTerrain.get(), predict.get(), true, 5)
//                            < minBreakDamage.get()
//                    )) {
//                        float multiSelfDmg = DamageCalcUtils.explosionDamage(mc.player, headPos, false, ignoreTerrain.get(), true, 5);
//                        if (!((double)multiSelfDmg > maxSelfBreakDamage.get())
//                            && (!noSuicideBreak.get() || !(PlayerUtils.getTotalHealth() - (double)multiSelfDmg < 0.5))) {
//                            if (!((Type)antiFriendPop.get()).breakTrue()) {
//                                return;
//                            }
//
//                            for(PlayerEntity friend : friends) {
//                                if (!((double)DamageCalcUtils.explosionDamage(friend, headPos, predict.get(), false, false, 5) < maxSelfBreakDamage.get())) {
//                                    double friendDmg = (double)DamageCalcUtils.explosionDamage(friend, headPos, predict.get(), ignoreTerrain.get(), true, 5);
//                                    if (friendDmg > maxSelfBreakDamage.get()
//                                        || noSuicideBreak.get() && (double) EntityUtils.getTotalHealth(friend) - friendDmg < 0.5) {
//                                        continue label254;
//                                    }
//                                }
//                            }
//
//                            return;
//                        }
//                    }
//                }
//            }
//
//            if (trapHoldTarget != null
//                && (
//                trapHoldTarget.isDead()
//                    || (double)trapHoldTarget.distanceTo(mc.player) > range.get() + 1.0
//                    || trapHoldTarget.distanceTo(mc.player) > (float)((Integer) targetRange.get()).intValue()
//                    || !UtilsPlus.isSurrounded(trapHoldTarget, true, false)
//            )) {
//                trapHoldTarget = null;
//            }
//
//            BedData bestBed = new BedData();
//            List<PlayerEntity> getPooped = new ArrayList();
//            int range = (int)Math.ceil(range.get() + 1.0);
//            Vec3d origin = (placeOrigin.get()).getOrigin(playerPos);
//            dumbBreak = null;
//
//            for(int x = -range; x <= range; ++x) {
//                for(double y = (double)(-range); y <= (double)range; ++y) {
//                    label188:
//                    for(int z = -range; z <= range; ++z) {
//                        BlockPos blockPos = new BlockPos((int) (origin.x + x), (int) (origin.y + y), (int) (origin.z + z));
//                        if (!BlockUtils2.invalidPos(blockPos)) {
//                            Vec3d currentPosVec = Vec3d.ofCenter(blockPos);
//                            if ((!xymb.get() || mc.world.canPlace(Blocks.PURPLE_BED.getDefaultState(), blockPos, ShapeContext.absent()))
//                                && (!oldMode.get() || mc.world.getBlockState(blockPos.down()).isSolid())) {
//                                Direction placeDirection = getPlaceDirection(blockPos);
//                                if (placeDirection != null) {
//                                    Vec3d offsetCurrentPosVec = Vec3d.ofCenter(blockPos.offset(placeDirection.getOpposite()));
//                                    double selfDmg = (double)DamageCalcUtils.explosionDamage(
//                                        mc.player, currentPosVec, false, ignoreTerrain.get(), true, 5
//                                    );
//                                    if (!(selfDmg > maxSelfPlaceDamage.get()) && (!noSuicidePlace.get() || !(PlayerUtils.getTotalHealth() - selfDmg < 0.5))) {
//                                        if (((Type)antiFriendPop.get()).placeTrue()) {
//                                            for(PlayerEntity friend : friends) {
//                                                if (!(
//                                                    (double)DamageCalcUtils.explosionDamage(friend, currentPosVec, predict.get(), false, false, 5)
//                                                        < maxSelfPlaceDamage.get()
//                                                )) {
//                                                    double friendDmg = (double)DamageCalcUtils.explosionDamage(
//                                                        friend, currentPosVec, predict.get(), ignoreTerrain.get(), true, 5
//                                                    );
//                                                    if (friendDmg > maxSelfPlaceDamage.get()
//                                                        || noSuicidePlace.get() && (double)EntityUtils.getTotalHealth(friend) - friendDmg < 0.5) {
//                                                        continue label188;
//                                                    }
//                                                }
//                                            }
//                                        }
//
//                                        float totalDamage = 0.0F;
//                                        boolean enoughSingleDamage = false;
//                                        List<PlayerEntity> poopsHere = new ArrayList();
//
//                                        for(PlayerEntity target : targets) {
//                                            if (!smartDelay.get() || target.hurtTime <= hurtTimeThreshold.get()) {
//                                                if (!netDamage.get()) {
//                                                    float simpleDmg = DamageCalcUtils.explosionDamage(target, currentPosVec, predict.get(), false, false, 5);
//                                                    if ((double)simpleDmg < minPlaceDamage.get() || simpleDmg < bestBed.damage) {
//                                                        continue;
//                                                    }
//                                                }
//
//                                                float damage = DamageCalcUtils.explosionDamage(
//                                                    target, currentPosVec, predict.get(), ignoreTerrain.get(), true, 5
//                                                );
//                                                if (netDamage.get()) {
//                                                    totalDamage += damage;
//                                                } else if (damage < bestBed.damage) {
//                                                    continue;
//                                                }
//
//                                                if (!((double)damage < minPlaceDamage.get())) {
//                                                    if ((double)damage > (double)EntityUtils.getTotalHealth(target) + 0.5) {
//                                                        poopsHere.add(target);
//                                                    }
//
//                                                    if (trapHoldTarget == null || !UtilsPlus.isSurrounded(target, true, true) || target.equals(trapHoldTarget)) {
//                                                        enoughSingleDamage = true;
//                                                        if (!netDamage.get()) {
//                                                            bestBed.set(new BlockPos((int) offsetCurrentPosVec.x, (int) offsetCurrentPosVec.y, (int) offsetCurrentPosVec.z), placeDirection, damage, target);
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                        }
//
//                                        if (netDamage.get() && enoughSingleDamage && getPooped.size() <= poopsHere.size() && !(bestBed.damage > totalDamage)) {
//                                            getPooped.clear();
//                                            getPooped.addAll(poopsHere);
//                                            bestBed.set(
//                                                new BlockPos(offsetCurrentPosVec),
//                                                placeDirection,
//                                                totalDamage,
//                                                trapHoldTarget == null ? (PlayerEntity)targets.get(0) : trapHoldTarget
//                                            );
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (bestBed.pos != null) {
//                target = bestBed.target;
//                if (debugText.get()) {
//                    info("normal place: " + bestBed.placeDirection, new Object[0]);
//                }
//
//                placeBed(bestBed, false);
//            }
//        }
//    }
//
//    private void doBreak() {
//        if (breakDelayLeft <= 0) {
//            dumbPlace = null;
//            BedData bestBed = new BedData();
//
//            label138:
//            for(BlockEntity blockEntity : Utils.blockEntities()) {
//                if (blockEntity instanceof BedBlockEntity
//                    && !mc.world.getBlockState(blockEntity.getPos()).isAir()
//                    && !BedBlock.getBedPart(mc.world.getBlockState(blockEntity.getPos())).equals(class_4733.FIRST)) {
//                    Vec3d footPos = new Vec3d(
//                        (double)blockEntity.getPos().getX() + 0.5,
//                        (double)blockEntity.getPos().getY() + 0.5,
//                        (double)blockEntity.getPos().getZ() + 0.5
//                    );
//                    Vec3d headPos = footPos.add(
//                        (double)BedBlock.getDirection(mc.world, blockEntity.getPos()).getOffsetX(),
//                        0.0,
//                        (double)BedBlock.getDirection(mc.world, blockEntity.getPos()).getOffsetZ()
//                    );
//                    boolean headInRange = inRange(headPos);
//                    boolean feetInRange = inRange(footPos);
//                    if (headInRange || feetInRange) {
//                        double selfDmg = (double)DamageCalcUtils.explosionDamage(mc.player, headPos, predict.get(), ignoreTerrain.get(), true, 5);
//                        if (!(selfDmg > maxSelfBreakDamage.get()) && (!noSuicideBreak.get() || !(PlayerUtils.getTotalHealth() - selfDmg < 0.5))) {
//                            if (((Type)antiFriendPop.get()).breakTrue()) {
//                                for(PlayerEntity friend : friends) {
//                                    if (!((double)DamageCalcUtils.explosionDamage(friend, headPos, predict.get(), false, false, 5) < maxSelfBreakDamage.get())
//                                    )
//                                    {
//                                        double friendDmg = (double)DamageCalcUtils.explosionDamage(
//                                            friend, headPos, predict.get(), ignoreTerrain.get(), true, 5
//                                        );
//                                        if (friendDmg > maxSelfBreakDamage.get()
//                                            || noSuicideBreak.get() && (double)EntityUtils.getTotalHealth(friend) - friendDmg < 0.5) {
//                                            continue label138;
//                                        }
//                                    }
//                                }
//                            }
//
//                            float totalDamage = 0.0F;
//                            boolean enoughSingleDamage = false;
//
//                            for(PlayerEntity target : targets) {
//                                if (!smartDelay.get() || target.hurtTime <= hurtTimeThreshold.get()) {
//                                    if (!netDamage.get()) {
//                                        float simpleDmg = DamageCalcUtils.explosionDamage(target, headPos, predict.get(), false, false, 5);
//                                        if ((double)simpleDmg < minBreakDamage.get() || simpleDmg < bestBed.damage) {
//                                            continue;
//                                        }
//                                    }
//
//                                    float damage = DamageCalcUtils.explosionDamage(target, headPos, predict.get(), ignoreTerrain.get(), true, 5);
//                                    if (netDamage.get()) {
//                                        totalDamage += damage;
//                                    } else if (damage < bestBed.damage) {
//                                        continue;
//                                    }
//
//                                    if (!((double)damage < minBreakDamage.get())) {
//                                        enoughSingleDamage = true;
//                                        if (!netDamage.get()) {
//                                            bestBed.set(
//                                                new BlockPos(footOrHead(footPos, headPos)),
//                                                BedBlock.getDirection(mc.world, blockEntity.getPos()),
//                                                damage,
//                                                target
//                                            );
//                                        }
//                                    }
//                                }
//                            }
//
//                            if (netDamage.get() && enoughSingleDamage && !(totalDamage < bestBed.damage)) {
//                                bestBed.set(
//                                    new BlockPos(footOrHead(footPos, headPos)),
//                                    BedBlock.getDirection(mc.world, blockEntity.getPos()),
//                                    totalDamage,
//                                    trapHoldTarget == null ? (PlayerEntity)targets.get(0) : trapHoldTarget
//                                );
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (bestBed.pos != null) {
//                target = bestBed.target;
//                if (rotateChecks.get()) {
//                    Rotations.rotate(Rotations.getYaw(bestBed.pos), Rotations.getPitch(bestBed.pos), 50, false, () -> breakBed(bestBed, false));
//                } else {
//                    breakBed(bestBed, false);
//                }
//            }
//        }
//    }
//
//    private Vec3d footOrHead(Vec3d feet, Vec3d head) {
//        return inRange(feet) ? feet : head;
//    }
//
//    private void openTable(BlockPos blockPos) {
//        if (debugText.get()) info("Attempting to open crafting table at " + blockPos + ".");
//
//        boolean wasSneaking = mc.player.isSneaking();
//        if (wasSneaking) {
//            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
//        }
//
//        if (rotateChecks.get()) {
//            RandUtils.rotate(
//                blockPos,
//                () -> mc
//                    .player
//                    .networkHandler
//                    .sendPacket(
//                        new PlayerInteractBlockC2SPacket(
//                            Hand.MAIN_HAND,
//                            new BlockHitResult(Vec3d.ofCenter(blockPos), BlockUtils2.getClosestDirection(blockPos, false), blockPos, true),
//                            0
//                        )
//                    )
//            );
//        } else {
//            mc
//                .player
//                .networkHandler
//                .sendPacket(
//                    new PlayerInteractBlockC2SPacket(
//                        Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(blockPos), BlockUtils2.getClosestDirection(blockPos, false), blockPos, true), 0
//                    )
//                );
//        }
//
//        RandUtils.swing(swingHand.get());
//        if (wasSneaking) {
//            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, class_2849.PRESS_SHIFT_KEY));
//        }
//    }
//
//    private void doPlace(FindItemResult result, BlockPos pos) {
//        boolean airPlace = !noAirPlace.get() && !oldMode.get();
//        BlockPos neighbor = pos;
//        Direction placeSide = BlockUtils2.getClosestDirection(pos, !airPlace);
//        if (!airPlace) {
//            if (oldMode.get()) {
//                neighbor = pos.down();
//                placeSide = Direction.UP;
//            } else {
//                neighbor = pos.offset(placeSide);
//                placeSide = placeSide.getOpposite();
//            }
//        }
//
//        if (result.getHand() == null) {
//            InvUtils.swap(result.slot(), switchBack.get());
//        }
//
//        Hand hand = RandUtils.hand(result);
//        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), placeSide, neighbor, false);
//        boolean sneak = !mc.player.isSneaking() && BlockUtils2.clickableBlock(mc.world.getBlockState(hit.getBlockPos()), hit, hand);
//        if (sneak) {
//            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
//        }
//
//        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, hit, 0));
//        RandUtils.swing(swingHand.get(), hand);
//        if (sneak) {
//            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
//        }
//
//        if (switchBack.get()) {
//            InvUtils.swapBack();
//        }
//    }
//
//    private void placeBed(AutoBed.BedData bedData, boolean fastPlace) {
//        FindItemResult result = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
//        if (result.found()) {
//            if (result.isMain() && autoMove.get()) {
//                doAutoMove();
//            }
//
//            if (!result.isHotbar() && !result.isOffhand()) {
//                result = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
//            }
//
//            if (result.found() && (result.getHand() != null || autoSwitch.get())) {
//                //AutoBed.BedRenderBlock.addBedRender(renderBeds, bedData, renderTime.get(), maxRenderBeds.get());
//                FindItemResult finalRes = result;
//                if (rotateChecks.get()) {
//                    Vec3d vec = Vec3d.ofCenter(bedData.pos).add(Vec3d.ofCenter(bedData.placeDirection.getVector()).multiply(0.5));
//                    Rotations.rotate(Rotations.getYaw(vec), Rotations.getPitch(bedData.pos), 50, () -> doPlace(finalRes, bedData.pos));
//                } else {
//                    Rotations.rotate(
//                        (double)UtilsPlus.yawFromDir(bedData.placeDirection), Rotations.getPitch(bedData.pos), 50, () -> doPlace(finalRes, bedData.pos)
//                    );
//                }
//
//                if (UtilsPlus.isSurrounded(bedData.target, true, true) && UtilsPlus.isSelfTrapBlock(bedData.target, bedData.pos)) {
//                    if (holeTrap.get()) {
//                        FindItemResult obby = InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN});
//                        if (inRange(bedData.target.getBlockPos().up(2))) {
//                            BlockUtils2.placeBlock(
//                                obby,
//                                bedData.target.getBlockPos().up(2),
//                                rotateChecks.get(),
//                                20,
//                                !oldMode.get() && !noAirPlace.get(),
//                                false,
//                                swingHand.get(),
//                                strictDirection.get()
//                            );
//                        }
//
//                        if (antiVclip.get()) {
//                            if (inRange(bedData.target.getBlockPos().up(4))) {
//                                BlockUtils2.placeBlock(
//                                    obby,
//                                    bedData.target.getBlockPos().up(4),
//                                    rotateChecks.get(),
//                                    15,
//                                    !oldMode.get() && !noAirPlace.get(),
//                                    false,
//                                    swingHand.get(),
//                                    strictDirection.get()
//                                );
//                            }
//
//                            if (inRange(bedData.target.getBlockPos().up(5))) {
//                                BlockUtils2.placeBlock(
//                                    obby,
//                                    bedData.target.getBlockPos().up(5),
//                                    rotateChecks.get(),
//                                    15,
//                                    !oldMode.get() && !noAirPlace.get(),
//                                    false,
//                                    swingHand.get(),
//                                    strictDirection.get()
//                                );
//                            }
//                        }
//                    }
//
//                    placeDelayLeft = holePlaceDelay.get();
//                    if (monkeyMode.get()) {
//                        breakDelayLeft = holeBreakDelay.get();
//                    }
//
//                    if (holeBreakDelay.get() > 0) {
//                        trapHoldTarget = bedData.target;
//                    }
//                } else {
//                    placeDelayLeft = placeDelay.get();
//                    if (monkeyMode.get()) {
//                        breakDelayLeft = breakDelay.get();
//                    }
//                }
//
//                if (!fastPlace && bedData.target.isAlive() && targets.contains(bedData.target) && bedData.damage != 0.0F) {
//                    dumbBreak = bedData;
//                }
//            }
//        }
//    }
//
//    private void breakBed(AutoBed.BedData data, boolean fastBreak) {
//        boolean wasSneaking = mc.player.isSneaking();
//        if (wasSneaking) {
//            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
//        }
//
//        Hand hand = mc.player.getMainHandStack().getItem() instanceof BlockItem ? Hand.OFF_HAND : Hand.MAIN_HAND;
//        mc.player
//            .networkHandler
//            .sendPacket(
//                new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(Vec3d.ofCenter(data.pos), BlockUtils2.getClosestDirection(data.pos, false), data.pos, false), 0)
//            );
//        RandUtils.swing(swingHand.get(), hand);
//        if (wasSneaking) {
//            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
//        }
//
//        if (UtilsPlus.isSurrounded(data.target, true, false) && UtilsPlus.isSelfTrapBlock(data.target, data.pos)) {
//            breakDelayLeft = holeBreakDelay.get();
//            if (monkeyMode.get()) {
//                placeDelayLeft = holePlaceDelay.get();
//            }
//        } else {
//            breakDelayLeft = breakDelay.get();
//            if (monkeyMode.get()) {
//                placeDelayLeft = placeDelay.get();
//            }
//        }
//
//        if (!fastBreak && data.target.isAlive() && targets.contains(data.target) && data.placeDirection != null) {
//            dumbPlace = data;
//        }
//    }
//
//    private boolean cantCraft() {
//        if (!autoCraft.get()) {
//            return true;
//        } else if (mc.currentScreen != null) {
//            return true;
//        } else if (pauseAll()) {
//            return true;
//        } else if (PlayerUtils.getTotalHealth() < (double)((Integer)minCraftHealth.get()).intValue()) {
//            return true;
//        } else if (craftSafe.get() && !UtilsPlus.isSafe(mc.player)) {
//            return true;
//        } else if (craftStill.get() && UtilsPlus.smartVelocity(mc.player).length() != 0.0) {
//            return true;
//        } else {
//            AutoCrafter crafter = (AutoCrafter)Modules.get().get(AutoCrafter.class);
//            FindItemResult beds = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
//            if (!beds.found() || beds.count() <= refillThreshold.get() && beds.count() < crafter..get()) {
//                int emptySlots = RandUtils.countEmptySlots();
//                if (emptySlots < minBedCraftAmount.get() || emptySlots == 0) {
//                    return true;
//                } else if (crafter.maxBeds.get() - beds.count() < minBedCraftAmount.get()) {
//                    return true;
//                } else if (!crafter.isActive()) {
//                    if (!hasWarnedNoCrafter) {
//                        error("Enable Auto Crafter!", new Object[0]);
//                        hasWarnedNoCrafter = true;
//                    }
//
//                    return true;
//                } else {
//                    hasWarnedNoCrafter = false;
//                    if (crafter.noBedMaterials()) {
//                        if (!hasWarnedNoMats) {
//                            error("Not enough materials!", new Object[0]);
//                            hasWarnedNoMats = true;
//                        }
//
//                        return true;
//                    } else {
//                        hasWarnedNoMats = false;
//                        return false;
//                    }
//                }
//            } else {
//                return true;
//            }
//        }
//    }
//
//    private float getBedDamage(Vec3d bedPos) {
//        float damage = 0.0F;
//
//        for(PlayerEntity target : targets) {
//            float dmg = DamageCalcUtils.explosionDamage(target, bedPos, predict.get(), ignoreTerrain.get(), true, 5);
//            if (netDamage.get()) {
//                damage += dmg;
//            } else if (dmg > damage) {
//                damage = dmg;
//            }
//        }
//
//        return damage;
//    }
//
//    @EventHandler
//    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
//        lastMineTarget = null;
//    }
//
//    private Direction getPlaceDirection(BlockPos blockPos) {
//        Direction returnDir = null;
//        double distance = range.get();
//        Vec3d origin = ((Origin)placeOrigin.get()).getOrigin(playerPos);
//        Iterator var6 = distance.HORIZONTAL.iterator();
//
//        while(true) {
//            Direction direction;
//            BlockPos offsetBlockPos;
//            Vec3d offsetPosVec;
//            boolean foundOne;
//            label51:
//            do {
//                if (!var6.hasNext()) {
//                    return returnDir;
//                }
//
//                direction = (Direction)var6.next();
//                offsetBlockPos = blockPos.offset(direction);
//                offsetPosVec = Vec3d.ofCenter(offsetBlockPos);
//                if (!rotateChecks.get()) {
//                    break;
//                }
//
//                foundOne = false;
//                Vec3d hitVec = offsetPosVec;
//
//                for(Direction direction1 : Arrays.asList(Direction.NORTH, Direction.SOUTH)) {
//                    hitVec = hitVec.add(Vec3d.ofCenter(direction1.getVector()).multiply(0.5));
//
//                    for(Direction direction2 : Arrays.asList(Direction.WEST, Direction.EAST)) {
//                        hitVec = hitVec.add(Vec3d.ofCenter(direction2.getVector()).multiply(0.5));
//                        double offsetYaw = Rotations.getYaw(hitVec);
//                        if (Direction.fromRotation(offsetYaw).equals(direction.getOpposite())) {
//                            foundOne = true;
//                            continue label51;
//                        }
//                    }
//                }
//            } while(!foundOne);
//
//            if (inRange(offsetPosVec)) {
//                double distanceCurrent = offsetPosVec.distanceTo(origin);
//                if (!(distanceCurrent > distance)
//                    && mc.world.canPlace(Blocks.PURPLE_BED.getDefaultState(), offsetBlockPos, ShapeContext.absent())) {
//                    BlockState offState = mc.world.getBlockState(offsetBlockPos);
//                    if (offState.getMaterial().isReplaceable()
//                        && (
//                        !oldMode.get()
//                            || offState.isAir() && mc.world.getBlockState(offsetBlockPos.down()).isSolidBlock(mc.world, blockPos)
//                    )
//                        && (!noAirplace.get() || !BlockUtils2.noSupport(offsetBlockPos))) {
//                        distance = distanceCurrent;
//                        returnDir = direction.getOpposite();
//                    }
//                }
//            }
//        }
//    }
//
//    private void doBreakSwitch(BlockPos pos) {
//        if (breakMode.get() == Mode.PACKET) {
//            if (mineTimeLeft == -1 && UtilsPlus.isTrapped(lastMineTarget.target)) {
//                int slot = InvUtils.findFastestTool(mc.world.getBlockState(pos)).slot();
//                if (slot != -1) {
//                    prevSlot = mc.player.getInventory().selectedSlot;
//                    InvUtils.swap(slot, false);
//                }
//            }
//        } else {
//            int slot = InvUtils.findFastestTool(mc.world.getBlockState(pos)).slot();
//            if (slot != -1) {
//                prevSlot = mc.player.getInventory().selectedSlot;
//                InvUtils.swap(slot, false);
//            }
//        }
//    }
//
//    private boolean isGoodTarget(PlayerEntity target) {
//        return target != null
//            && target.isAlive()
//            && target.isInRange(mc.player, targetRange.get())
//            && (!onlyHoled.get() || UtilsPlus.isSurrounded(target, true, false));
//    }
//
//    private boolean inRange(BlockPos blockPos) {
//        return inRange(Vec3d.ofCenter(blockPos));
//    }
//
//    private boolean inRange(Vec3d pos) {
//        if (pos == null) {
//            return false;
//        } else {
//            double distance = (placeOrigin.get()).getOrigin(playerPos).distanceTo(pos);
//            if (UtilsPlus.cantSee(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), strictDirection.get())) {
//                return distance <= wallsRange.get();
//            } else {
//                return distance <= range.get();
//            }
//        }
//    }
//
//    private void doAutoMove() {
//        if (!InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem).found()) {
//            int slot = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).slot();
//            InvUtils.move().from(slot).toHotbar(autoMoveSlot.get() - 1);
//        }
//    }
//
//    private boolean shouldPause() {
//        if (pauseAll()) {
//            return true;
//        } else {
//            FindItemResult beds = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
//            return beds.count() < minBedAmount.get() || beds.count() <= 0;
//        }
//    }
//
//    private boolean pauseAll() {
//        return pauseWhileCrafting.get() && mc.currentScreen instanceof CraftingScreen
//            ? true
//            : PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get());
//    }
//
//    public boolean active() {
//        return !shouldPause() && place.get() && !targets.isEmpty() && isActive();
//    }
//
//    public void onActivate() {
//        placeDelayLeft = 0;
//        breakDelayLeft = 0;
//        craftDelayLeft = 0;
//        mineTimeLeft = -1;
//        prevSlot = -1;
//        hasWarnedNoMats = false;
//        hasWarnedNoCrafter = false;
//        target = null;
//        dumbBreak = null;
//        dumbPlace = null;
//        trapHoldTarget = null;
//        lastMineTarget = null;
//        dumbCraft = null;
//        targets.clear();
//        friends.clear();
//        //renderBeds.clear();
//        //mineBlocks.clear();
//        executor = Executors.newSingleThreadExecutor();
//    }
//
//    public void onDeactivate() {
//        target = null;
//    }
//
////    @EventHandler
////    private void onRender(Render3DEvent event) {
////        if (render.get()) {
////            try {
////                for(AutoBed.BedRenderBlock block : renderBeds) {
////                    if (gradient.get()) {
////                        RenderUtils2.renderBed(
////                            event.renderer,
////                            (Color)sideColor.get(),
////                            (Color)sideColor2.get(),
////                            (Color)lineColor.get(),
////                            (Color)lineColor2.get(),
////                            (ShapeMode)shapeMode.get(),
////                            fade.get(),
////                            block.ticks,
////                            renderTime.get(),
////                            beforeFadeDelay.get(),
////                            block.placeDirection,
////                            block.pos,
////                            shrink.get()
////                        );
////                    } else {
////                        RenderUtils2.renderBed(
////                            event.renderer,
////                            (Color)sideColor.get(),
////                            (Color)sideColor.get(),
////                            (Color)lineColor.get(),
////                            (Color)lineColor.get(),
////                            (ShapeMode)shapeMode.get(),
////                            fade.get(),
////                            block.ticks,
////                            renderTime.get(),
////                            beforeFadeDelay.get(),
////                            block.placeDirection,
////                            block.pos,
////                            shrink.get()
////                        );
////                    }
////                }
////
////                if (renderMine.get()) {
////                    for(RenderBlock renderBlock : mineBlocks) {
////                        renderBlock.render(
////                            event,
////                            (SettingColor)mineSideColor.get(),
////                            (SettingColor)mineLineColor.get(),
////                            (ShapeMode)shapeMode.get(),
////                            mineFade.get()
////                        );
////                    }
////                }
////            } catch (ConcurrentModificationException var4) {
////                if (debugText.get()) {
////                    warning("cme", new Object[0]);
////                }
////            }
////        }
////    }
////
////    @EventHandler
////    private void onRender2D(Render2DEvent event) {
////        if (render.get() && renderDamage.get() && !renderBeds.isEmpty()) {
////            int preA = ((SettingColor)damageColor.get()).a;
////
////            try {
////                for(AutoBed.BedRenderBlock block : renderBeds) {
////                    Vec3 pos = new Vec3(
////                        (double)block.pos.getX() + (double)block.placeDirection.getOffsetX() * 0.5 + 0.5,
////                        (double)block.pos.getY() + 0.3,
////                        (double)block.pos.getZ() + (double)block.placeDirection.getOffsetZ() * 0.5 + 0.5
////                    );
////                    if (NametagUtils.to2D(pos, damageScale.get())) {
////                        NametagUtils.begin(pos);
////                        TextRenderer.get().begin(1.0, false, true);
////                        String damageText = String.valueOf(TextUtils.round(block.damage, roundDamage.get()));
////                        double w = TextRenderer.get().getWidth(damageText) * 0.5;
////                        if (fade.get()) {
////                            SettingColor var10000 = (SettingColor)damageColor.get();
////                            var10000.a = (int)((float)var10000.a * (block.ticks / (float)((Integer)renderTime.get()).intValue()));
////                        }
////
////                        TextRenderer.get().render(damageText, -w, 0.0, (Color)damageColor.get(), true);
////                        TextRenderer.get().end();
////                        NametagUtils.end();
////                        ((SettingColor)damageColor.get()).a = preA;
////                    }
////                }
////            } catch (Exception var9) {
////            }
////        }
////    }
//
////    @Override
////    public PlayerEntity getTarget() {
////        return target == null ? null : target;
////    }
//
//    public String getInfoString() {
//        return target == null ? null : target.getName().getString();
//    }
//
//
//    private static class BedData {
//        public BlockPos pos;
//        public Direction placeDirection;
//        public float damage;
//        public PlayerEntity target;
//
//        public BedData() {
//        }
//
//        public BedData(BlockPos pos, Direction placeDirection, float damage, PlayerEntity target) {
//            pos = pos;
//            placeDirection = placeDirection;
//            damage = damage;
//            target = target;
//        }
//
//        public void set(BlockPos pos, Direction placeDirection, float damage, PlayerEntity target) {
//            pos = pos;
//            placeDirection = placeDirection;
//            damage = damage;
//            target = target;
//        }
//    }
//}
