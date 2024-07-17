package me.blackout.orcrist.features.module.combat;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.CombatHelper;
import me.blackout.orcrist.utils.world.BlockHelper;
import me.blackout.orcrist.utils.world.TimerUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoCityPlus extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<MiningMode> mode = sgGeneral.add(new EnumSetting.Builder<MiningMode>().name("mode").description("Which mode to mine blocks from.").defaultValue(MiningMode.Normal).build());
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("The radius in which players get targeted.").defaultValue(5.5).min(0).sliderMax(7).build());
    private final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder().name("break-range").description("How close a block must be to you to be considered.").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<AutoCityPlus.SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>().name("switch-mode").description("How to switch to a pickaxe.").defaultValue(SwitchMode.Normal).build());
    private final Setting<Boolean> crystal = sgGeneral.add(new BoolSetting.Builder().name("crystal").description("Places down crystal to your enemy's surround to break it.").defaultValue(true).build());
    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder().name("support").description("If there is no block below a city block it will place one before mining.").defaultValue(true).build());
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").description("How far away to try and place a block.").defaultValue(4.5).min(0).sliderMax(6).visible(support::get).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder().name("chat-info").description("Whether the module should send messages in chat.").defaultValue(true).build());

    // Render
    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder().name("swing-hand").description("Whether to render your hand swinging.").defaultValue(false).build());
    private final Setting<Boolean> renderBlock = sgRender.add(new BoolSetting.Builder().name("render-block").description("Whether to render the block being broken.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(renderBlock::get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the rendering.").defaultValue(new SettingColor(225, 0, 0, 75)).visible(() -> renderBlock.get() && shapeMode.get().sides()).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the rendering.").defaultValue(new SettingColor(225, 0, 0, 255)).visible(() -> renderBlock.get() && shapeMode.get().lines()).build());

    private boolean firstMined, crystalPlaced;
    private PlayerEntity target;
    private BlockPos targetPos;
    private FindItemResult pick;
    private float progress;

    private TimerUtils timer = new TimerUtils();

    public AutoCityPlus() {
        super(OrcristAddon.Combat, "auto-city+", "Automatically mines target's surround block.");
    }

    @Override
    public void onActivate() {
        timer.reset();

        firstMined = false;
        crystalPlaced = false;

        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.ClosestAngle);
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            if (chatInfo.get()) error("Couldn't find a target, disabling.");
            toggle();
            return;
        }

        targetPos = EntityUtils.getCityBlock(target);
        if (targetPos == null || PlayerUtils.squaredDistanceTo(targetPos) > Math.pow(breakRange.get(), 2)) {
            if (chatInfo.get()) error("Couldn't find a good block, disabling.");
            toggle();
            return;
        }

        if (support.get()) {
            BlockPos supportPos = targetPos.down();
            if (!(PlayerUtils.squaredDistanceTo(supportPos) > Math.pow(placeRange.get(), 2))) {
                BlockUtils.place(supportPos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            }
        }

        pick = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
        if (!pick.isHotbar()) {
            error("No pickaxe found... disabling.");
            toggle();
            return;
        }

        progress = 0.0f;
        mine(false);
    }

    @Override
    public void onDeactivate() {
        timer.reset();
        target = null;
        targetPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        AutoCrystal ac = Modules.get().get(AutoCrystal.class);

        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            toggle();
            return;
        }

        if (PlayerUtils.squaredDistanceTo(targetPos) > Math.pow(breakRange.get(), 2)) {
            if (chatInfo.get()) error("Couldn't find a target, disabling.");
            toggle();
            return;
        }

        if (progress < 1.0f) {
            pick = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
            if (!pick.isHotbar()) {
                error("No pickaxe found... disabling.");
                toggle();
                return;
            }
            progress += (float) BlockUtils.getBreakDelta(pick.slot(), mc.world.getBlockState(targetPos));
            if (progress < 1.0f) return;
        }

        // Place Crystal
        if (crystal.get()) placeCrystal(targetPos);

        // Mine
        mine(true);

        // Toggle
        if ((crystalPlaced && crystal.get()) && ac.placing) toggle();

        else if (ac.placing) toggle();
    }

    public void mine(boolean done) {
        InvUtils.swap(pick.slot(), switchMode.get() == SwitchMode.Silent);
        if (rotate.get()) Rotations.rotate(Rotations.getYaw(targetPos), Rotations.getPitch(targetPos));

        Direction direction = BlockUtils.getDirection(targetPos);

        switch (mode.get()) {

            case Normal -> BlockUtils.breakBlock(targetPos, rotate.get());

            case Packet -> {
                if (!done) mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, targetPos, direction));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, direction));
            }

            case Instant -> {

                if (!firstMined) {
                    BlockUtils.breakBlock(targetPos, swingHand.get());
                    firstMined = true;
                }

                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, direction));

            }
        }

        if (swingHand.get()) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (switchMode.get() == SwitchMode.Silent) InvUtils.swapBack();
    }

    private void placeCrystal(BlockPos cityPos) {
        for (CardinalDirection dir : CardinalDirection.values()) {

            FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);

            BlockPos offsetPos = cityPos.offset(dir.toDirection());

            if (progress >= 90 || !CombatHelper.isInHole(target, true)) {

                if (BlockUtils.canPlace(offsetPos) && !target.getBlockPos().equals(offsetPos)) {

                    if (getCrystal(offsetPos) == null) {
                        //if (BlockUtils.place(offsetPos, crystal, rotate.get(), 90)) crystalPlaced = true;
                        //BlockUtils.interact(new BlockHitResult(Utils.vec3d(offsetPos), BlockUtils.getPlaceSide(offsetPos), offsetPos, true), crystal.getHand() == null ? Hand.MAIN_HAND : crystal.getHand(), true);

                        mc.interactionManager.interactBlock(mc.player, crystal.getHand() == null ? Hand.MAIN_HAND : crystal.getHand(), new BlockHitResult(new Vec3d(offsetPos.down().getX(), offsetPos.down().getY(), offsetPos.down().getZ()), Direction.UP, offsetPos.down(), true));

                        crystalPlaced = true;
                    }

                    break;

                } else if (BlockUtils.canPlace(offsetPos.down()) && !target.getBlockPos().down().equals(offsetPos.down())) {

                    if (getCrystal(offsetPos.down()) == null) {
                        //if (BlockUtils.place(offsetPos.down(), crystal, rotate.get(), 90)) crystalPlaced = true;
                        //BlockUtils.interact(new BlockHitResult(Utils.vec3d(offsetPos.down()), BlockUtils.getPlaceSide(offsetPos.down()), offsetPos.down(), true), crystal.getHand() == null ? Hand.MAIN_HAND : crystal.getHand(), true);;

                        mc.interactionManager.interactBlock(mc.player, crystal.getHand() == null ? Hand.MAIN_HAND : crystal.getHand(), new BlockHitResult(new Vec3d(offsetPos.down(2).getX(), offsetPos.down(2).getY(), offsetPos.down(2).getZ()), Direction.UP, offsetPos.down(2), true));

                        crystalPlaced = true;
                    }

                    break;
                }
            }
        }
    }

    private EndCrystalEntity getCrystal(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && pos.up().equals(entity.getBlockPos())) return (EndCrystalEntity) entity;
        }

        return null;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (targetPos == null || !renderBlock.get()) return;
        event.renderer.box(targetPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    public enum SwitchMode {
        Normal,
        Silent
    }

    public enum MiningMode {
        Normal,
        Instant,
        Packet,
    }
}
