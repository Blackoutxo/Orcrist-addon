package me.blackout.orcrist.utils.world;

import me.blackout.orcrist.utils.player.CombatHelper;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.world.BlockUtils.*;

public class BlockHelper {

    // Block Poses
    public static BlockPos iterateBlock(ArrayList<BlockPos> pos) {
        for (BlockPos iPos : pos) return iPos;
        return null;
    }

    public static List<BlockPos> getSphere(BlockPos centerPos, double radius, double height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (double i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (double j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (double k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos((int) i, (int) j, (int) k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    public static BlockPos getBlockPosFromDirection(Direction direction, BlockPos orginalPos) {
        return switch (direction) {
            case UP -> orginalPos.up();
            case DOWN -> orginalPos.down();
            case EAST -> orginalPos.east();
            case WEST -> orginalPos.west();
            case NORTH -> orginalPos.north();
            case SOUTH -> orginalPos.south();
        };
    }

    public static Block getBlock(BlockPos pos) {
        if (pos == null) return null;
        return mc.world.getBlockState(pos).getBlock();
    }

    public static BlockPos blockPos(Vec3d vec) {
        return new BlockPos((int) vec.getX(), (int) vec.getY(), (int) vec.getY());
    }

    public static VoxelShape getCollision(BlockPos pos) {
        return getBlockState(pos).getCollisionShape(mc.world, pos);
    }

    // Double
//    public static boolean outOfPlaceRange(BlockPos pos, Origin origin, double range) {
//        if (origin == Origin.VANILLA) {
//            return mc.player.squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5)
//                >= range * range;
//        } else {
//            assert mc.player != null;
//            Vec3d eyesPos = CombatHelper.eyePos(mc.player);
//            double dx = eyesPos.x - (double)pos.getX() - 0.5;
//            double dy = eyesPos.y - (double)pos.getY() - 0.5;
//            double dz = eyesPos.z - (double)pos.getZ() - 0.5;
//            return dx * dx + dy * dy + dz * dz > range * range;
//        }
//    }

    public static double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static double distanceBetweenXZ(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + f * f));
    }

    // Boolean

    public static boolean isOurSurroundBlock(BlockPos bp) {
        BlockPos pos = mc.player.getBlockPos();
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;
            pos = pos.offset(direction);
            if (pos.equals(bp)) return true;
        }
        return false;
    }

    public static boolean outOfRange(BlockPos cityBlock) {
        return MathHelper.sqrt((float) mc.player.squaredDistanceTo(cityBlock.getX(), cityBlock.getY(), cityBlock.getZ())) > mc.player.getBlockInteractionRange();
    }

    public static boolean isBedrock(BlockPos pos) {
        return mc.world.getBlockState(pos).isOf(Blocks.BEDROCK);
    }

    public static boolean isBlastResistant(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock().getBlastResistance() >= 600;
    }

    public static boolean isVecComplete(ArrayList<Vec3d> vlist) {
        BlockPos ppos = mc.player.getBlockPos();
        for (Vec3d b: vlist) {
            BlockPos bb = ppos.add((int) b.x, (int) b.y, (int) b.z);
            if (getBlock(bb) == Blocks.AIR) return false;
        }
        return true;
    }

    public static BlockState getBlockState(BlockPos pos) {
        return mc.world.getBlockState(pos);
    }

    public static boolean isSolid(BlockPos pos) {
        return mc.world.getBlockState(pos).isSolid();
    }

    public static boolean isBlockPosComplete(List<BlockPos> blockPosList) {
        BlockPos ppos = mc.player.getBlockPos();
        for (BlockPos b: blockPosList) {
            BlockPos bb = ppos.add(b.getX(), b.getY(), b.getZ());
            if (getBlock(bb) == Blocks.AIR) return false;
        }
        return true;
    }

    // Mine block
    public static boolean mine(BlockPos targetPos, boolean rotate) {
        mc.interactionManager.updateBlockBreakingProgress(targetPos, Direction.UP);
        Vec3d hitPos = new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        if (rotate) Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), 50, () -> mc.player.swingHand(Hand.MAIN_HAND));
        return true;
    }

    // Block Placement
    public static boolean placeBlock(BlockPos pos, FindItemResult result, int rotationPriority, boolean rotate, boolean packetPlace, boolean airPlace, boolean swapBack, Hand hand) {
        return placeBlock(pos, result, rotationPriority, rotate, packetPlace, airPlace, false, true, swapBack, hand);
    }

    public static boolean placeBlock(BlockPos pos, FindItemResult result, int rotationPriority, boolean rotate, boolean packetPlace, boolean airPlace, boolean ignoreEntity, boolean swing, boolean swapBack, Hand hand) {
        return placeBlock(pos, result, rotationPriority, rotate, packetPlace, airPlace, false, ignoreEntity, swing, swapBack, airPlace ? null : Direction.UP, hand);
    }

    public static boolean placeBlock(BlockPos pos, FindItemResult result, int rotationPriority, boolean rotate, boolean packetPlace, boolean airPlace, boolean forceAirplace, boolean ignoreEntity, boolean swing, boolean swapBack, Direction overrideSide, Hand hand) {
        // make sure place is empty if ignoreEntity is not true
        if(ignoreEntity && !mc.world.getBlockState(pos).isReplaceable()) return false;
         else if(!mc.world.getBlockState(pos).isReplaceable() || !mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent())) return false;

        Vec3d hitVec = null;
        BlockPos neighbor = null;
        Direction side2 = null;

        if(!forceAirplace || !airPlace) {
            if(overrideSide != null) {
                neighbor = pos.offset(overrideSide.getOpposite());
                side2 = overrideSide;
            }

            for(Direction side: Direction.values()) {
                if(overrideSide == null) {
                    neighbor = pos.offset(side);
                    side2 = side.getOpposite();

                    // check if neighbor can be right clicked aka it isnt air
                    if(mc.world.getBlockState(neighbor).isAir() || mc.world.getBlockState(neighbor).getBlock() instanceof FluidBlock) {
                        neighbor = null;
                        side2 = null;
                        continue;
                    }
                }

                hitVec = new Vec3d(neighbor.getX(), neighbor.getY(), neighbor.getZ()).add(0.5, 0.5, 0.5).add(new Vec3d(side2.getUnitVector()).multiply(0.5));
                break;
            }
        }

        // Air place if no neighbour was found
        if(airPlace) {
            if(hitVec == null) hitVec = Vec3d.ofCenter(pos);
            if(neighbor == null) neighbor = pos;
            if(side2 == null) side2 = Direction.UP;
        } else if(hitVec == null || neighbor == null || side2 == null) {
            return false;
        }

        // Rotate using rotation manager and specified settings
        if(rotate) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), rotationPriority, () -> {

                if (packetPlace) {

                    InvUtils.swap(result.slot(), swapBack);

                    if (swapBack) InvUtils.swapBack();

                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));

                } else {

                    InvUtils.swap(result.slot(), swapBack);

                    if (swapBack) InvUtils.swapBack();

                }


            });
        } else {

            if (packetPlace) {

                InvUtils.swap(result.slot(), true);

                if (swapBack) InvUtils.swapBack();

                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));

            } else {

                InvUtils.swap(result.slot(), true);

                if (swapBack) InvUtils.swapBack();

            }
        }

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));

        if(packetPlace) mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(hitVec, side2, neighbor, false), 0));
        else mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(hitVec, side2, neighbor, false));

        if (swing) mc.player.swingHand(hand);

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

        return true;
    }
}
