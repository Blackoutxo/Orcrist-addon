package me.blackout.orcrist.utils.misc;

import me.blackout.orcrist.utils.world.BlockHelper;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PacketManager {

    //Mining packets
    public static void sendPacketMine(BlockPos pos, boolean swing) {
        if (pos == null) return;
        sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
        sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
    }

    // Sending packets
    public static void sendPacket(Packet packet) {
        if (packet == null) return;
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);
    }

    // Interacting Item & slot change
    public static void updateSlot(int slot) {
        if (slot == -1) return;
        sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void interactItem(Hand hand) {
        sendPacket(new PlayerInteractItemC2SPacket(hand, 0, mc.player.getYaw(), mc.player.getPitch()));
    }

    // Block Interactions
    public static void interact(BlockPos pos, Hand hand) {
        if (pos == null || hand == null) return;
        mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false));
    }

    public static void interactSphere(BlockPos pos, double radius, Hand hand) {
        for (BlockPos ITpos : BlockHelper.getSphere(pos, radius, radius)) mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(new Vec3d(ITpos.getX(), ITpos.getY(), ITpos.getZ()), Direction.UP, ITpos, false));
    }
}
