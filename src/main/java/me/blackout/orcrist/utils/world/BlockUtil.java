package me.blackout.orcrist.utils.world;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.world.BlockUtils.*;

@SuppressWarnings("ConstantConditions")
public class BlockUtil {

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(BlockUtil.class);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotatePriority, boolean airplace, boolean packetPlace) {
        return place(blockPos, findItemResult, rotate, rotatePriority, airplace, packetPlace, true, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotatePriority, boolean airplace, boolean packetPlace, boolean swingHand) {
        return place(blockPos, findItemResult, rotate, rotatePriority, airplace, packetPlace, swingHand, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean airplace, boolean packetPlace, boolean swingHand, boolean checkEntities) {
        return place(blockPos, findItemResult, rotate, rotationPriority, airplace, packetPlace, swingHand, checkEntities, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean airPlace, boolean packetPlace, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (findItemResult.isOffhand()) {
            return place(blockPos, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, rotate, rotationPriority, airPlace, packetPlace, swingHand, checkEntities, swapBack, Direction.UP);
        } else if (findItemResult.isHotbar()) {
            return place(blockPos, Hand.MAIN_HAND, findItemResult.slot(), rotate, rotationPriority, airPlace, packetPlace, swingHand, checkEntities, swapBack, Direction.UP);
        }
        return false;
    }

    public static boolean place(BlockPos pos, Hand hand, int slot, boolean rotate, int rotationPriority, boolean airPlace, boolean packetPlace, boolean swingHand, boolean checkEntities, boolean swapBack, Direction dir) {
        if (slot < 0 || slot > 8) return false;

        Block toPlace = Blocks.OBSIDIAN;
        ItemStack i = hand == Hand.MAIN_HAND ? mc.player.getInventory().getStack(slot) : mc.player.getInventory().getStack(SlotUtils.OFFHAND);
        if (i.getItem() instanceof BlockItem blockItem) toPlace = blockItem.getBlock();
        if (!canPlaceBlock(pos, checkEntities, toPlace)) return false;

        Vec3d hitPos = Vec3d.ofCenter(pos);

        BlockPos neighbour;
        Direction side = getPlaceSide(pos);

        if (side == null || airPlace) {
            side = Direction.DOWN;
            neighbour = pos;
        } else {
            neighbour = pos.offset(side.getOpposite());
            hitPos = hitPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        }

        BlockHitResult bhr = new BlockHitResult(hitPos, side.getOpposite(), neighbour, false);

        if (rotate) {
            Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), rotationPriority, () -> {
                InvUtils.swap(slot, swapBack);

                interact(bhr, hand, packetPlace, swingHand);

                if (swapBack) InvUtils.swapBack();
            });
        } else {
            InvUtils.swap(slot, swapBack);

            interact(bhr, hand, packetPlace, swingHand);

            if (swapBack) InvUtils.swapBack();
        }

        return true;
    }

    public static void interact(BlockHitResult blockHitResult, Hand hand, boolean packet, boolean swing) {
        boolean wasSneaking = mc.player.input.sneaking;
        mc.player.input.sneaking = false;

        ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);

        if (packet) {
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, blockHitResult, 0));

            if (swing) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        } else if (result.shouldSwingHand()) {

            if (swing) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        }

        mc.player.input.sneaking = wasSneaking;
    }
}
