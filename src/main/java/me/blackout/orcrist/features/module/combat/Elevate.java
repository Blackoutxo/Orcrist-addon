package me.blackout.orcrist.features.module.combat;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.misc.PacketManager;
import me.blackout.orcrist.utils.player.CombatHelper;
import me.blackout.orcrist.utils.world.BlockHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class  Elevate extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").description("The range in which to target enemy.").defaultValue(4).sliderMin(1).sliderMax(5).build());
    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder().name("smart").description("Will do the smart decision for you.").defaultValue(true).build());
    private final Setting<Boolean> breakBurrow = sgGeneral.add(new BoolSetting.Builder().name("break-burrow").description("Will break enemy's burrow if they are burrowed.").defaultValue(true).build());
    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder().name("bypass").description("Bypasses the Anti Piston Push.").defaultValue(true).build());
    private final Setting<BypassMode> bypassMode = sgGeneral.add(new EnumSetting.Builder<BypassMode>().name("bypass-mode").description("The bypass mode.").defaultValue(BypassMode.Block).visible(bypass :: get).build());

    private int pistonTimer, redstoneTimer, interactTimer;
    private List<BlockPos> PPos = new ArrayList<>();
    private PlayerEntity target;
    private boolean rotated;

    public Elevate() {
        super(OrcristAddon.Combat, "elevate", "Pushes enemy out of their hole.");
    }

    @Override
    public void onActivate() {
        pistonTimer = 0;
        interactTimer = 0;
        redstoneTimer = 0;
    }

    @Override
    public void onDeactivate() {
        if (target != null) {

            FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
            PPos = BlockHelper.getSphere(target.getBlockPos(), range.get(), range.get());

            if (bypass.get()) {
                for (BlockPos pos : PPos) {
                    if (BlockHelper.getBlock(pos) == Blocks.PISTON_HEAD) {
                        if (bypassMode.get() == BypassMode.Block) {
                            BlockUtils.place(pos.down(), InvUtils.findInHotbar(Items.OBSIDIAN), false, 0, true);
                        } else if (bypassMode.get() == BypassMode.Crystal) {
                            BlockUtils.place(pos.down(), InvUtils.findInHotbar(Items.END_CRYSTAL), false, 0, true);
                        }
                    }
                }
            }

            if (smart.get()) {
                for (BlockPos pos : PPos) {
                    if (BlockHelper.getBlock(pos) == Blocks.PISTON_HEAD) {
                        if (clear(pos.east())) BlockUtils.place(pos.east(), crystal, false, 0, true);
                        else if (clear(pos.west())) BlockUtils.place(pos.west(), crystal, false, 0, true);
                        else if (clear(pos.north())) BlockUtils.place(pos.north(), crystal, false, 0, true);
                        else if (clear(pos.south())) BlockUtils.place(pos.south(), crystal, false, 0, true);
                    }
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult redstoneBlock = InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
        FindItemResult buttons = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.POLISHED_BLACKSTONE_BUTTON || itemStack.getItem() == Items.STONE_BUTTON || itemStack.getItem() == Items.OAK_BUTTON || itemStack.getItem() == Items.SPRUCE_BUTTON || itemStack.getItem() == Items.ACACIA_BUTTON || itemStack.getItem() == Items.JUNGLE_BUTTON || itemStack.getItem() == Items.BIRCH_BUTTON || itemStack.getItem() == Items.DARK_OAK_BUTTON || itemStack.getItem() == Items.WARPED_BUTTON || itemStack.getItem() == Items.CRIMSON_BUTTON);

        if (TargetUtils.isBadTarget(target, range.get())) target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (TargetUtils.isBadTarget(target, range.get())) return;

        PPos = BlockHelper.getSphere(target.getBlockPos(), range.get(), range.get());

        // Toggle in no target
        if (target == null) {
            info("Target not found, disabling...");
            toggle();
            return;
        }

        // Toggle when target is out of hole
        if (!CombatHelper.isInHole(target, true)) {
            info("Target is out the hole, disabling...");
            toggle();
            return;
        }

        // Toggle if enemy is in swimming pose
        if (target.isInSwimmingPose() && !CombatHelper.isInHole(target, false)) {
            info("Target is in swim mode instead of getting pushed out, disabling...");
            toggle();
        }

        // Break burrow
        if (breakBurrow.get() && CombatHelper.isBurrowed(target, false)) {
            FindItemResult pick = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);

            if (pick.found()) {
                PacketManager.updateSlot(pick.slot());
                PacketManager.sendPacketMine(target.getBlockPos(), true);
                return;
            }
        }

        // Interact if interactable
        if (interactTimer >= 15) {
            if (isInteractable(target.getBlockPos())) PacketManager.interactSphere(target.getBlockPos(), range.get(), Hand.MAIN_HAND);
            interactTimer = 0;
        } else interactTimer++;

        BlockPos TarPos = target.getBlockPos();

        // Main placing
        if (isPushable(Direction.EAST) && distanceTo(TarPos.east().up()) <= range.get()) {
            placePiston(target.getBlockPos().east().up(), -90);

            for (BlockPos pos : PPos) {
                if (BlockHelper.getBlock(pos) instanceof  PistonBlock) {
                    if (redstoneBlock.found()) {
                        if (clear(pos.east())) placeRedstone(pos.east(), redstoneBlock);
                        else if (clear(pos.up())) placeRedstone(pos.up(), redstoneBlock);
                        else if (clear(pos.north())) placeRedstone(pos.north(), redstoneBlock);
                        else if (clear(pos.south())) placeRedstone(pos.south(), redstoneBlock);
                    } else if (buttons.found()) {
                        if (clear(pos.east())) placeRedstone(pos.east(), buttons);
                        else if (!clear(pos.east()) && !clear(pos.east().up()) && clear(pos.east(2))) placeRedstone(pos.east(2), buttons);
                        else if (!clear(pos.north().down()) && clear(pos.north())) placeRedstone(pos.north(), buttons);
                        else if (!clear(pos.south().down()) && clear(pos.south())) placeRedstone(pos.south(), buttons);
                        else if (!clear(pos.north(2)) && clear(pos.north())) placeRedstone(pos.north(), buttons);
                        else if (clear(pos.up(2)) && !clear(pos.up())) placeRedstone(pos.up(2), buttons);
                    }
                }
            }
        } else if (isPushable(Direction.NORTH)  && distanceTo(TarPos.north().up()) <= range.get()) {
            placePiston(target.getBlockPos().north().up(), 180);

            for (BlockPos pos : PPos) {
                if (BlockHelper.getBlock(pos) instanceof  PistonBlock) {
                    if (redstoneBlock.found()) {
                        if (clear(pos.north())) placeRedstone(pos.north(), redstoneBlock);
                        else if (clear(pos.up())) placeRedstone(pos.up(), redstoneBlock);
                        else if (clear(pos.east())) placeRedstone(pos.east(), redstoneBlock);
                        else if (clear(pos.west())) placeRedstone(pos.west(), redstoneBlock);
                    } else if (buttons.found()) {
                        if (clear(pos.north())) placeRedstone(pos.north(), buttons);
                        else if (!clear(pos.north()) && !clear(pos.north().up()) && clear(pos.north(2))) placeRedstone(pos.north(2), buttons);
                        else if (!clear(pos.east().down()) && clear(pos.east())) placeRedstone(pos.east(), buttons);
                        else if (!clear(pos.west().down()) && clear(pos.west())) placeRedstone(pos.west(), buttons);
                        else if (!clear(pos.east(2)) && clear(pos.east())) placeRedstone(pos.east(), buttons);
                        else if (clear(pos.up(2)) && !clear(pos.up())) placeRedstone(pos.up(2), buttons);
                    }
                }
            }
        } else if (isPushable(Direction.WEST)  && distanceTo(TarPos.west().up()) <= range.get()) {
            placePiston(target.getBlockPos().west().up(), 90);

            for (BlockPos pos : PPos) {
                if (BlockHelper.getBlock(pos) instanceof  PistonBlock) {
                    if (redstoneBlock.found()) {
                        if (clear(pos.west())) placeRedstone(pos.west(), redstoneBlock);
                        else if (clear(pos.up())) placeRedstone(pos.up(), redstoneBlock);
                        else if (clear(pos.north())) placeRedstone(pos.north(), redstoneBlock);
                        else if (clear(pos.south())) placeRedstone(pos.south(), redstoneBlock);
                    } else if (buttons.found()) {
                        if (clear(pos.west())) placeRedstone(pos.west(), buttons);
                        else if (!clear(pos.west()) && !clear(pos.west().up()) && clear(pos.west(2))) placeRedstone(pos.west(2), buttons);
                        else if (!clear(pos.north().down()) && clear(pos.north())) placeRedstone(pos.north(), buttons);
                        else if (!clear(pos.south().down()) && clear(pos.south())) placeRedstone(pos.south(), buttons);
                        else if (!clear(pos.north(2)) && clear(pos.north())) placeRedstone(pos.north(), buttons);
                        else if (clear(pos.up(2)) && !clear(pos.up())) placeRedstone(pos.up(2), buttons);
                    }
                }
            }
        } else if (isPushable(Direction.SOUTH)  && distanceTo(TarPos.south().up()) <= range.get()) {
            placePiston(target.getBlockPos().south().up(), -180);

            for (BlockPos pos : PPos) {
                if (BlockHelper.getBlock(pos) instanceof  PistonBlock) {
                     if (redstoneBlock.found()) {
                        if (clear(pos.south())) placeRedstone(pos.south(), redstoneBlock);
                        else if (clear(pos.up())) placeRedstone(pos.up(), redstoneBlock);
                        else if (clear(pos.east())) placeRedstone(pos.east(), redstoneBlock);
                        else if (clear(pos.west())) placeRedstone(pos.west(), redstoneBlock);
                    } else if (buttons.found()) {
                        if (clear(pos.south())) placeRedstone(pos.south(), buttons);
                        else if (!clear(pos.south()) && !clear(pos.south().up()) && clear(pos.north(2))) placeRedstone(pos.north(2), buttons);
                        else if (!clear(pos.east().down()) && clear(pos.east())) placeRedstone(pos.east(), buttons);
                        else if (!clear(pos.west().down()) && clear(pos.west())) placeRedstone(pos.west(), buttons);
                        else if (!clear(pos.east(2)) && clear(pos.east())) placeRedstone(pos.east(), buttons);
                        else if (clear(pos.up(2)) && !clear(pos.up())) placeRedstone(pos.up(2), buttons);
                    }
                }
            }
        } else {
            info("Can't place piston, disabling...");
            toggle();
        }
    }

    private void placePiston(BlockPos pos, int yaw) {
        FindItemResult piston = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.PISTON || itemStack.getItem() == Items.STICKY_PISTON);
        if (!piston.found()) {
            info("Can't find pistons, disabling...");
            toggle();
            return;
        }

        Rotations.rotate(yaw, 0, () -> {
            rotated = true;
        });

        if (pistonTimer >= 5 && rotated) {
            BlockUtils.place(pos, piston, false, 0, true);
            rotated = false;
            pistonTimer = 0;
        } else pistonTimer++;
    }

    private void placeRedstone(BlockPos pos, FindItemResult result) {
        if (!result.found()) {
            info("Can't find redstone, disabling...");
            toggle();
            return;
        }

        if (redstoneTimer >= 5) {
            BlockUtils.place(pos, result, false, 0, true);
            redstoneTimer = 0;
        } else redstoneTimer++;
    }

    private boolean isPushable(Direction direction) {
        return switch (direction) {
            case EAST -> clear(target.getBlockPos().add(1, 1, 0)) && clear(target.getBlockPos().add(-1, 1, 0)) && clear(target.getBlockPos().add(-1, 2, 0));
            case WEST -> clear(target.getBlockPos().add(-1, 1, 0)) && clear(target.getBlockPos().add(1, 1, 0)) && clear(target.getBlockPos().add(1, 2, 0));
            case NORTH -> clear(target.getBlockPos().add(0, 1, -1)) && clear(target.getBlockPos().add(0, 1, 1))  && clear(target.getBlockPos().add(0, 2, 1));
            case SOUTH -> clear(target.getBlockPos().add(0, 1, 1)) && clear(target.getBlockPos().add(0, 1, -1))  && clear(target.getBlockPos().add(0, 2, -1));
            case UP -> clear(target.getBlockPos().add(1, 1, 1));
            case DOWN -> clear(target.getBlockPos().add(0, 1, 1));
        };
    }

    private boolean isInteractable(BlockPos pos) {
        List<BlockPos> IPos = BlockHelper.getSphere(pos, 4, 4);
        for (BlockPos InteractablePos : IPos) {
            if (isInteractableRedstone(InteractablePos)) mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(InteractablePos.getX(), InteractablePos.getY(), InteractablePos.getZ()), Direction.UP, InteractablePos, false));
        }
        return true;
    }

    private boolean isInteractableRedstone(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.POLISHED_BLACKSTONE_BUTTON || BlockHelper.getBlock(pos) == Blocks.STONE_BUTTON || BlockHelper.getBlock(pos) == Block.getBlockFromItem(Items.STRING) || BlockHelper.getBlock(pos) == Blocks.OAK_BUTTON || BlockHelper.getBlock(pos) == Blocks.SPRUCE_BUTTON || BlockHelper.getBlock(pos) == Blocks.ACACIA_BUTTON || BlockHelper.getBlock(pos) == Blocks.JUNGLE_BUTTON || BlockHelper.getBlock(pos) == Blocks.WARPED_BUTTON || BlockHelper.getBlock(pos) == Blocks.CRIMSON_BUTTON || BlockHelper.getBlock(pos) == Blocks.LEVER;
    }

    private double distanceTo(BlockPos blockPos) {
        return PlayerUtils.distanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    private boolean clear(BlockPos pos) {
        return !BlockHelper.isBlastResistant(pos);
    }

    public enum BypassMode {
        Block, Crystal
    }
}
