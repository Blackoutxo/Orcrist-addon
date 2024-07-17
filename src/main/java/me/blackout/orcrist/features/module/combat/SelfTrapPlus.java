package me.blackout.orcrist.features.module.combat;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.CombatHelper;
import me.blackout.orcrist.utils.world.BlockHelper;
import me.blackout.orcrist.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SelfTrapPlus extends Module {
    public enum modes {
        AntiFacePlace,
        Full,
        Top,
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<modes> mode = sgGeneral.add(new EnumSetting.Builder<modes>().name("mode").description("Which positions to place on your top half.").defaultValue(modes.Top).build());
    private final Setting<Boolean> antiCev = sgGeneral.add(new BoolSetting.Builder().name("anti-Cev").description("Protect yourself from cev breaker.").defaultValue(true).build());
    private final Setting<Integer> blockPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-Per-Tick").description("How many block placements per tick.").defaultValue(4).sliderMin(1).sliderMax(10).build());
    private final Setting<Boolean> toggleOnPositionChange = sgGeneral.add(new BoolSetting.Builder().name("toggle-position-change").description("Toggles the module if your y level is changed.").defaultValue(true).build());
    private final Setting<Boolean> reinforce = sgGeneral.add(new BoolSetting.Builder().name("reinforce").description("Reinforces your self trap.").defaultValue(true).build());
    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder().name("air-place").description("Places blocks in air.").defaultValue(true).build());
    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder().name("center").description("Centers you on the block you are standing on before placing.").defaultValue(true).build());
    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder().name("toggle").description("Turns off after placing.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Sends rotation packets to the server when placing.").defaultValue(true).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Will render blocks.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("Which shape to render").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("Color for side mode.").defaultValue(new SettingColor(197, 137, 232,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("Color for the line mode.").defaultValue(new SettingColor(197, 137, 232, 255)).build());

    private final List<BlockPos> placePositions = new ArrayList<>();
    private int bpt;

    private final ArrayList<Vec3d> full = new ArrayList<Vec3d>() {{
        add(new Vec3d(0, 2, 0));
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    private final ArrayList<Vec3d> antiFacePlace = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    public SelfTrapPlus(){
        super(OrcristAddon.Combat,"self-trap+", "Automatically traps yourself.");
    }

    @Override
    public void onActivate() {
        if (!placePositions.isEmpty()) placePositions.clear();
        if (center.get()) PlayerUtils.centerPlayer();

        bpt = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN, Items.NETHERITE_BLOCK);
        bpt = 0;

        // Check for obsidian
        if (!obsidian.found()) {
            error("No obsidian in hotbar!");
            toggle();
            return;
        }

        // Toggle on complete
        if (BlockHelper.isVecComplete(getTrapDesign()) && turnOff.get()) {
            info("Finished self trap.");
            toggle();
            return;
        }

        // Toggle in Y change
        if (toggleOnPositionChange.get() && CombatHelper.positionChanged(mc.player)) {
            info("Position changed.");
            toggle();
            return;
        }

        // Place
        for (Vec3d b : getTrapDesign()) {
            if (bpt >= blockPerTick.get()) return;

            BlockPos ppos = mc.player.getBlockPos();

            BlockPos bb = ppos.add((int) b.x, (int) b.y, (int) b.z);

            attackCrystal(bb);

            if (BlockHelper.getBlock(bb) == Blocks.AIR)  {
                 BlockUtil.place(bb, obsidian, rotate.get(), 100, airPlace.get(), false, true, false);
                bpt++;
            }
        }
    }

    private void attackCrystal(BlockPos pos) {
        Box box = new Box(
            pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        );

        Predicate<Entity> entityPredicate = entity -> entity instanceof EndCrystalEntity && DamageUtils.crystalDamage(mc.player, entity.getPos()) < PlayerUtils.getTotalHealth();

        for (Entity crystal : mc.world.getOtherEntities(null, box, entityPredicate)) {
            if (rotate.get()) {
                Rotations.rotate(Rotations.getPitch(crystal), Rotations.getYaw(crystal), () -> {
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                    if (reinforce.get()) BlockUtils.place(crystal.getBlockPos(), InvUtils.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN, Items.NETHERITE_BLOCK), rotate.get(), 100);
                });
            } else {
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                if (reinforce.get()) BlockUtils.place(crystal.getBlockPos(), InvUtils.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN, Items.NETHERITE_BLOCK), rotate.get(), 100);
            }

            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || BlockHelper.isVecComplete(getTrapDesign())) return;

        for (Vec3d b: getTrapDesign()) {
            BlockPos ppos = mc.player.getBlockPos();
            BlockPos bb = ppos.add((int) b.x, (int) b.y, (int) b.z);
            if (BlockHelper.getBlock(bb) == Blocks.AIR) event.renderer.box(bb, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    private ArrayList<Vec3d> getTrapDesign() {
        ArrayList<Vec3d> trapDesign = new ArrayList<Vec3d>();

        switch (mode.get()) {
            case Full ->  trapDesign.addAll(full);
            case Top ->  trapDesign.add(new Vec3d(0, 2, 0));
            case AntiFacePlace ->  trapDesign.addAll(antiFacePlace);
        }

        if (antiCev.get())  trapDesign.add(new Vec3d(0, 3, 0));

        return trapDesign;
    }
}
