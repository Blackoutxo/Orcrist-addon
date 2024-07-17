package me.blackout.orcrist.features.module.combat;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.misc.PacketManager;
import me.blackout.orcrist.utils.player.CombatHelper;
import me.blackout.orcrist.utils.world.BlockHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static me.blackout.orcrist.utils.world.BlockHelper.getBlock;

public class PistonCrystal extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // ------------------ General ------------------ //
    private final Setting<Integer> actionInterval = sgGeneral.add(new IntSetting.Builder().name("action-interval").description("delay between actions").defaultValue(0).sliderRange(0, 10).build());
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder().name("range").description("The range in which to target enemy.").defaultValue(4).sliderMin(1).sliderMax(6).build());
    private final Setting<Boolean> breakBurrow = sgGeneral.add(new BoolSetting.Builder().name("break-burrow").description("Will break burrow.").defaultValue(true).build());
    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder().name("support").description("Will put support block if needed.").defaultValue(true).build());

    private int redstoneTimer, crystalTimer, pistonTimer, interactTimer;
    private PlayerEntity target;

    public PistonCrystal() {
        super(OrcristAddon.Combat, "piston-crystal", "Moves crystals into enemy's hole with piston.");
    }

    @Override
    public void onActivate() {
        interactTimer = 0;
        redstoneTimer = 0;
        crystalTimer = 0;
        pistonTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult interactables = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.STONE_BUTTON || itemStack.getItem() == Items.POLISHED_BLACKSTONE_BUTTON || itemStack.getItem() == Items.OAK_BUTTON || itemStack.getItem() == Items.SPRUCE_BUTTON || itemStack.getItem() == Items.ACACIA_BUTTON || itemStack.getItem() == Items.JUNGLE_BUTTON || itemStack.getItem() == Items.CRIMSON_BUTTON || itemStack.getItem() == Items.WARPED_BUTTON || itemStack.getItem() == Items.LEVER);
        FindItemResult redstoneBlock = InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
        FindItemResult torch = InvUtils.findInHotbar(Items.REDSTONE_TORCH);

        if (TargetUtils.isBadTarget(target, range.get())) target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (TargetUtils.isBadTarget(target, range.get())) return;

        if (mc.player.distanceTo(target) < 4) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity crystalEntity) {
                    if (DamageUtils.crystalDamage(target, crystalEntity.getPos()) >= 8)
                        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
                }
            }
        }

        // Do Interaction
        if (interactTimer >= 25) {
            if (isInteractable(target.getBlockPos())) PacketManager.interactSphere(target.getBlockPos(), range.get(), Hand.MAIN_HAND);
            interactTimer = 0;
        } else interactTimer++;

        //Break Burrow
        if (breakBurrow.get() && CombatHelper.isBurrowed(target, true)) {
            BlockUtils.breakBlock(target.getBlockPos().add(0, 0, 0), true);
        }

        if (isPlaceAble(Direction.EAST)) {
            placePiston(target.getBlockPos().east(2).up(), -90);

            List<BlockPos> Sphere = BlockHelper.getSphere(target.getBlockPos(), range.get(), range.get());
            for (BlockPos pos : Sphere) {
                if (getBlock(pos) instanceof PistonBlock) {
                    if (redstoneBlock.found() && getCrystal(pos.west()) != null) placeRedstone(pos.east(), redstoneBlock);
                    else if (interactables.found()) placeRedstone(pos.east(), interactables);
                    else if (!isSolid(pos.east().down()) && torch.found() && support.get()) placeBlock(pos.east().down(), InvUtils.find(Items.OBSIDIAN));
                    else if (isSolid(pos.east().down()) || isSolid(pos.east(2)) || isSolid(pos.east().north()) || isSolid(pos.east().south()) && torch.found()) placeRedstone(pos.east(), torch);

                    placeCrystal(pos.west());
                }
            }
        } else if (isPlaceAble(Direction.WEST)) {
            placePiston(target.getBlockPos().west(2).up(), 90);

            List<BlockPos> Sphere = BlockHelper.getSphere(target.getBlockPos(), range.get(), range.get());
            for (BlockPos pos : Sphere) {
                if (getBlock(pos) instanceof PistonBlock) {
                    if (redstoneBlock.found() && getCrystal(pos.east()) != null) placeRedstone(pos.west(), redstoneBlock);
                    else if (interactables.found()) placeRedstone(pos.west(), interactables);
                    else if (!isSolid(pos.west().down()) && torch.found() && support.get()) placeBlock(pos.west().down(), InvUtils.find(Items.OBSIDIAN));
                    else if (isSolid(pos.west().down()) || isSolid(pos.west(2)) || isSolid(pos.west().north()) || isSolid(pos.west().south()) && torch.found()) placeRedstone(pos.west(), torch);

                    placeCrystal(pos.east());
                }
            }
        } else if (isPlaceAble(Direction.NORTH)) {
            placePiston(target.getBlockPos().north(2).up(), 180);

            List<BlockPos> Sphere = BlockHelper.getSphere(target.getBlockPos(), range.get(), range.get());
            for (BlockPos pos : Sphere) {
                if (getBlock(pos) instanceof PistonBlock) {
                    if (redstoneBlock.found() && getCrystal(pos.south()) != null) placeRedstone(pos.north(), redstoneBlock);
                    else if (interactables.found()) placeRedstone(pos.north(), interactables);
                    else if (!isSolid(pos.north().down()) && torch.found() && support.get()) placeBlock(pos.north().down(), InvUtils.find(Items.OBSIDIAN));
                    else if (isSolid(pos.north().down()) || isSolid(pos.north(2)) || isSolid(pos.north().east()) || isSolid(pos.north().west()) && torch.found()) placeRedstone(pos.north(), torch);

                    placeCrystal(pos.south());
                }
            }
        } else if (isPlaceAble(Direction.SOUTH)) {
            placePiston(target.getBlockPos().south(2).up(), -180);

            List<BlockPos> Sphere = BlockHelper.getSphere(target.getBlockPos(), range.get(), range.get());
            for (BlockPos pos : Sphere) {
                if (getBlock(pos) instanceof PistonBlock) {
                    if (redstoneBlock.found() && getCrystal(pos.north()) != null) placeRedstone(pos.south(), redstoneBlock);
                    else if (interactables.found()) placeRedstone(pos.south(), interactables);
                    else if (!isSolid(pos.south().down()) && torch.found() && support.get()) placeBlock(pos.south().down(), InvUtils.find(Items.OBSIDIAN));
                    else if (isSolid(pos.south().down()) || isSolid(pos.north(2)) || isSolid(pos.south().east()) || isSolid(pos.south().west()) && torch.found()) placeRedstone(pos.south(), torch);

                    placeCrystal(pos.north());
                }
            }
        } else if (isTopPlaceAble(Direction.EAST)) {
            placePiston(target.getBlockPos().east(2).up(2), -90);

            List<BlockPos> Sphere = BlockHelper.getSphere(target.getBlockPos(), range.get(), range.get());
            for (BlockPos pos : Sphere) {
                if (getBlock(pos) instanceof PistonBlock) {
                    if (!isSolid(target.getBlockPos().up(2))) {
                        if (redstoneBlock.found() && getCrystal(pos.west()) != null) placeRedstone(pos.east(), redstoneBlock);
                        else if (interactables.found()) placeRedstone(pos.east(), interactables);
                        else if (!isSolid(pos.east().down()) && torch.found() && support.get()) placeBlock(pos.east().down(), InvUtils.find(Items.OBSIDIAN));
                        else if (isSolid(pos.east().down()) || isSolid(pos.east(2)) || isSolid(pos.east().north()) || isSolid(pos.east().south()) && torch.found()) placeRedstone(pos.east(), torch);
                    } else if (isSolid(target.getBlockPos().up(2))) {
                        if (clear(pos.east())) placeRedstone(pos.east(), interactables);
                    }

                    placeCrystal(pos.west());
                }
            }
        }
    }

    private boolean isPlaceAble(Direction direction) {
        return switch (direction) {
            case EAST -> clear(target.getBlockPos().add(1, 1, 0)) && clear(target.getBlockPos().add(2, 1, 0));
            case WEST -> clear(target.getBlockPos().add(-1, 1, 0)) && clear(target.getBlockPos().add(-2, 1, 0));
            case NORTH -> clear(target.getBlockPos().add(0, 1, -1)) && clear(target.getBlockPos().add(0, 1, -2));
            case SOUTH -> clear(target.getBlockPos().add(0, 1, 1)) && clear(target.getBlockPos().add(0, 1, 2));
            case DOWN -> clear(target.getBlockPos().add(1,1,1));
            case UP -> clear(target.getBlockPos().add(1,2,3));
        };
    }

    private EndCrystalEntity getCrystal(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && pos.up().equals(entity.getBlockPos())) return (EndCrystalEntity) entity;
        }

        return null;
    }

    private boolean isSolid(BlockPos pos) {
        try {
            return mc.world.getBlockState(pos).isSolid();
        } catch (NullPointerException e) {
            return false;
        }
    }

    private boolean isTopPlaceAble(Direction direction) {
        return switch (direction) {
            case EAST -> clear(target.getBlockPos().add(1, 2, 0)) && clear(target.getBlockPos().add(2, 2, 0));
            case WEST -> clear(target.getBlockPos().add(-1, 2, 0)) && clear(target.getBlockPos().add(-2, 2, 0));
            case NORTH -> clear(target.getBlockPos().add(0, 2, -1)) && clear(target.getBlockPos().add(0, 2, -2));
            case SOUTH -> clear(target.getBlockPos().add(0, 2, 1)) && clear(target.getBlockPos().add(0, 2, 2));
            case DOWN -> clear(target.getBlockPos().add(1 ,1 ,1));
            case UP -> clear(target.getBlockPos().add(1, 2, 3));
        };
    }

    private void placePiston(BlockPos pos, int yaw) {
        FindItemResult piston = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.PISTON || itemStack.getItem() == Items.STICKY_PISTON);
        if (pos == null) return;

        Rotations.rotate(yaw, 0);

        if (pistonTimer >= actionInterval.get()) {
            BlockUtils.place(pos, piston, false, 0, true);
            pistonTimer = 0;
        } else pistonTimer++;

        if (!piston.found()) {
            info("Can't find piston, disabling...");
            toggle();
        }
    }

    private void placeBlock(BlockPos pos, FindItemResult result) {
        if (pos == null) return;
        BlockUtils.place(pos, result, false, 0, true);
    }

    private void placeCrystal(BlockPos pos) {
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (pos == null) return;

        if (crystalTimer >= actionInterval.get()) {
            BlockUtils.place(pos, crystal, false, 0, false);
            crystalTimer = 0;
        } else crystalTimer++;

        if (!crystal.found()) {
            info("Can't find crystals, disabling...");
            toggle();
        }
    }

    private void placeRedstone(BlockPos pos, FindItemResult result) {
        if (pos == null) return;

        if (redstoneTimer >= actionInterval.get()) {
            BlockUtils.place(pos, result, false, 0, true);
            redstoneTimer = 0;
        } else redstoneTimer++;

        if (!result.found()) {
            info("Can't find redstone, disabling...");
            toggle();
        }
    }

    private boolean isInteractable(BlockPos pos) {
        List<BlockPos> IPos = BlockHelper.getSphere(pos, range.get(), range.get());
        for (BlockPos InteractablePos : IPos) {
            if (!(getBlock(InteractablePos) instanceof ButtonBlock)) return false;
            if (getBlock(InteractablePos) instanceof ButtonBlock) mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(InteractablePos.getX(), InteractablePos.getY(), InteractablePos.getZ()), Direction.UP, InteractablePos, false));
        }
        return true;
    }

    private boolean clear(BlockPos pos) {
        return !BlockHelper.isBlastResistant(pos);
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
