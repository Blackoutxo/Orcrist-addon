package me.blackout.orcrist.features.module.combat;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.CombatHelper;
import me.blackout.orcrist.utils.world.BlockHelper;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class CevBreaker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTrap = settings.createGroup("Trap");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("mode").description("Determines how to mine obsidian blocks.").defaultValue(Mode.Vanilla).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("How many ticks to wait between block placements.").defaultValue(4).min(0).sliderRange(0, 10).build());
    private final Setting<Integer> crystalDelay = sgGeneral.add(new IntSetting.Builder().name("break-delay").description("How many ticks to wait before breaking a crystal.").defaultValue(4).min(0).sliderRange(0, 10).build());
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").description("The radius players can be in to be targeted.").defaultValue(5).sliderMin(0).sliderMax(10).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to select the player to target.").defaultValue(SortPriority.LowestDistance).build());
    private final Setting<Boolean> NORM = sgGeneral.add(new BoolSetting.Builder().name("NORM").build());
    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder().name("anti-suicide").description("Will not place and break crystals if they will kill you.").defaultValue(true).build());
    private final Setting<Double> maxDamage = sgGeneral.add(new DoubleSetting.Builder().name("max-damage").description("Maximum damage crystals can deal to yourself.").defaultValue(8).min(0).max(36).sliderMax(36).visible(antiSuicide::get).build());
    private final Setting<Boolean> silentSwitch = sgGeneral.add(new BoolSetting.Builder().name("silent-switch").description("Changes slots only clientside.").defaultValue(false).build());
    private final Setting<Boolean> interfering = sgGeneral.add(new BoolSetting.Builder().name("break-interfering").description("Breaks blocks that prevent the target from taking damage.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Sends rotation packets to the server when placing.").defaultValue(true).build());

    // Trap
    private final Setting<Boolean> trap = sgTrap.add(new BoolSetting.Builder().name("trap").description("Traps the target before cev-ing them.").defaultValue(false).build());
    private final Setting<Boolean> instaTrap = sgTrap.add(new BoolSetting.Builder().name("insta-trap").description("Instaplaces the trap-blocks around the target.").defaultValue(false).build());
    private final Setting<TrapMode> trapMode = sgTrap.add(new EnumSetting.Builder<TrapMode>().name("trap-mode").description("Which blocks to place around the target.").defaultValue(TrapMode.Face).build());

    // Pause
    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses cev breaker when eating.").defaultValue(true).build());
    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses cev breaker when drinking.").defaultValue(true).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the obsidian will be placed.").defaultValue(true).build());
    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder().name("render-swing").description("Renders a hand swing animation.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(render::get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The color of the sides of the blocks being rendered.").defaultValue(new SettingColor(197, 137, 232, 45)).visible(render::get).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The color of the lines of the blocks being rendered.").defaultValue(new SettingColor(197, 137, 232, 255)).visible(render::get).build());

    private final List<BlockPos> placePositions = new ArrayList<>();

    private BlockPos obbypos;

    private boolean broken;

    private int ctimer;
    private int timer;

    public CevBreaker() {
        super(OrcristAddon.Combat,"cev-breaker", "Places and explodes crystals on top of the target.");
    }

    @Override
    public void onActivate() {

        obbypos = null;

        ctimer = crystalDelay.get() + 1;
        timer = delay.get() + 1;

        broken = false;

        placePositions.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());

        obbypos = null;

        FindItemResult obby = InvUtils.findInHotbar(Items.OBSIDIAN);
        FindItemResult pick = InvUtils.findInHotbar(stack -> stack.getItem() == Items.DIAMOND_PICKAXE || stack.getItem() == Items.NETHERITE_PICKAXE);
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);

        if (!obby.found()) {
            error("Can't find obsidian in your hotbar, disabling...");
            toggle();
            return;
        }

        if (!pick.found()) {
            error("Can't find a pickaxe in your hotbar, disabling...");
            toggle();
            return;
        }

        if (!crystal.found()) {
            error("Can't find crystals in your hotbar, disabling...");
            toggle();
            return;
        }

        if (PlayerUtils.shouldPause(false, eatPause.get(), drinkPause.get())) return;

        if (target != null) {
            // Pause if target isn't in hole
            if (!CombatHelper.isInHole(target, true)) return;

            // Set Obsidian position
            BlockPos pos = target.getBlockPos().up(2);

            // Side pos
            if (BlockHelper.isSolid(pos.up()) || !PlayerUtils.isWithinReach(pos)) pos = pos.down().east();
            else if (BlockHelper.isSolid(pos.up()) || !PlayerUtils.isWithinReach(pos)) pos = pos.down().west();
            else if (BlockHelper.isSolid(pos.up()) || !PlayerUtils.isWithinReach(pos)) pos = pos.down().north();
            else if (BlockHelper.isSolid(pos.up()) || !PlayerUtils.isWithinReach(pos)) pos = pos.down().south();

            // Trap

            if (trap.get()) {
                fillPlaceArray(target);

                for (int i = 0; i < 3; i++) {
                    if (!placePositions.isEmpty()) {
                        if (instaTrap.get()) {

                            if (BlockUtils.place(placePositions.get(placePositions.size() - 1), obby, rotate.get(), 50, true)) {
                                placePositions.remove(placePositions.get(placePositions.size() - 1));
                            }
                        } else {
                            if (BlockUtils.place(placePositions.get(placePositions.size() - 1), obby, rotate.get(), 50, true)) {
                                placePositions.remove(placePositions.get(placePositions.size() - 1));
                                return;
                            }
                        }
                    }
                }
            }

            // Interfering

            if (interfering.get()) {
                if (BlockHelper.isSolid(target.getBlockPos())) {
                    mine(target.getBlockPos(), pick);
                    obbypos = target.getBlockPos();
                    return;
                }

                if (BlockHelper.isSolid(target.getBlockPos().up())) {
                    mine(target.getBlockPos().up(), pick);
                    obbypos = target.getBlockPos().up();
                    return;
                }

                if (BlockHelper.isSolid(pos.up())) {
                    mine(pos.up(), pick);
                    obbypos = pos.up();
                    return;
                }
            }

            if (antiSuicide.get()) {
                double selfDamage = DamageUtils.crystalDamage(mc.player, Utils.vec3d(pos.up()));
                if (selfDamage > maxDamage.get() || (antiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player))) return;
            }

            if (BlockHelper.getBlockState(pos).isAir() && getCrystal(pos) == null) {
                if (delay.get() == 0 || timer > delay.get()) {
                    for (Entity entity : mc.world.getEntities()) {
                        if (entity instanceof EndCrystalEntity && BlockHelper.distanceBetweenXZ(pos, entity.getBlockPos()) <= 1.5) {
                            attack((EndCrystalEntity) entity);
                            return;
                        }
                    }

                    BlockUtils.place(pos, obby, rotate.get(), 50);
                    obbypos = pos;
                    timer = delay.get() + 1;
                    ctimer = crystalDelay.get() + 1;
                } else timer++;
            }

            if (BlockHelper.getBlock(pos) == Blocks.OBSIDIAN && getCrystal(pos) == null) {
                if (crystalDelay.get() == 0 || ctimer > crystalDelay.get()) {
                    for (Entity entity : mc.world.getEntities()) {
                        if (entity instanceof EndCrystalEntity && BlockHelper.distanceBetween(pos, entity.getBlockPos()) <= 2.5) {
                            attack((EndCrystalEntity) entity);
                            ctimer = 0;
                            return;
                        }
                    }

                    interact(pos, crystal, Direction.UP);
                    ctimer = 0;
                } else ctimer++;
            }

            if (BlockHelper.getBlock(pos) == Blocks.OBSIDIAN && getCrystal(pos) != null) {
                mine(pos, pick);

                ctimer = crystalDelay.get() + 1;
                obbypos = pos;
            }

            if (BlockHelper.getBlockState(pos).isAir() && getCrystal(pos) != null) {
                if (crystalDelay.get() == 0 || ctimer > crystalDelay.get()) {
                    attack(getCrystal(pos));
                    ctimer = 0;
                } else ctimer++;
            }
        }
    }

    // Utils

    private EndCrystalEntity getCrystal(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && pos.up().equals(entity.getBlockPos())) return (EndCrystalEntity) entity;
        }

        return null;
    }

    private void mine(BlockPos pos, FindItemResult pick) {
        if (mc.player.getInventory().selectedSlot != pick.slot()) InvUtils.swap(pick.slot(), false);

        switch (mode.get()) {
            case Vanilla -> BlockUtils.breakBlock(pos, renderSwing.get());

            case Packet -> {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));

                if (renderSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
            }

            case Instant -> {
                if (rotate.get()) {

                    Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> {

                        if (!broken) {
                            BlockUtils.breakBlock(pos, renderSwing.get());
                            broken = true;
                        }

                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));

                    });

                } else {

                    if (!broken) {
                        BlockUtils.breakBlock(pos, renderSwing.get());
                        broken = true;
                    }

                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));

                }

                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

                if (renderSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                //else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }
    }

    private void attack(EndCrystalEntity entity) {
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));

        if (renderSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private void interact(BlockPos pos, FindItemResult item, Direction direction) {
        if (item == null) return;
        if (item.getHand() == null || !silentSwitch.get()) InvUtils.swap(item.slot(), false);

        if (silentSwitch.get()) {
            if (NORM.get()) mc.interactionManager.interactBlock(mc.player, item.getHand() == null ? Hand.MAIN_HAND : item.getHand(), new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), direction, pos, true));
            else BlockUtils.place(pos.up(1), item, 90, rotate.get());
            //BlockUtils.interact(new BlockHitResult(mc.player.getPos(), direction, pos, true), item.getHand() == null ? Hand.MAIN_HAND : item.getHand(), renderSwing.get());
        } else {
            if (NORM.get()) mc.interactionManager.interactBlock(mc.player, item.getHand() == null ? Hand.MAIN_HAND : item.getHand(), new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), direction, pos, true));
            else BlockUtils.place(pos.up(1), item, 90, rotate.get());
            //BlockUtils.interact(new BlockHitResult(mc.player.getPos(), direction, pos, true), Hand.MAIN_HAND, renderSwing.get());
        }
    }

    // Trap

    private void fillPlaceArray(PlayerEntity target) {
        placePositions.clear();
        BlockPos targetPos = target.getBlockPos();

        switch (trapMode.get()) {
            case Full -> {
                add(targetPos.add(1, 1, 0));
                add(targetPos.add(-1, 1, 0));
                add(targetPos.add(0, 1, 1));
                add(targetPos.add(0, 1, -1));

                add(targetPos.add(1, 0, 0));
                add(targetPos.add(-1, 0, 0));
                add(targetPos.add(0, 0, 1));
                add(targetPos.add(0, 0, -1));
            }
            case Face -> {
                add(targetPos.add(1, 1, 0));
                add(targetPos.add(-1, 1, 0));
                add(targetPos.add(0, 1, 1));
                add(targetPos.add(0, 1, -1));
            }
            case Feet -> {
                add(targetPos.add(1, 0, 0));
                add(targetPos.add(-1, 0, 0));
                add(targetPos.add(0, 0, 1));
                add(targetPos.add(0, 0, -1));
            }
        }
    }


    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos) && BlockUtils.canPlace(blockPos, true)) placePositions.add(blockPos);
    }

    // Render

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (render.get() && obbypos != null) event.renderer.box(obbypos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    // Enums
    public enum Mode {
        Vanilla,
        Instant,
        Packet
    }

    public enum TrapMode {
        Full,
        Face,
        Feet,
        None
    }
}
