package me.blackout.orcrist.utils.player;

import com.ibm.icu.text.ArabicShaping;
import me.blackout.orcrist.utils.world.BlockHelper;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static me.blackout.orcrist.utils.world.BlockHelper.isBlastResistant;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CombatHelper {

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(CombatHelper.class);
    }

    // Interfering
    public static boolean isBurrowed(PlayerEntity p, boolean holeCheck) {
        if (p == null) return false;
        BlockPos pos = p.getBlockPos();
        if (holeCheck && !CombatHelper.isInHole(p, true)) return false;
        return BlockHelper.getBlock(pos) == Blocks.ENDER_CHEST || BlockHelper.getBlock(pos) == Blocks.OBSIDIAN || isAnvilBlock(pos);
    }

    public static boolean isWebbed(PlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        if (isWeb(pos)) return true;
        return isWeb(pos.up());
    }

    public static boolean isAnvilBlock(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.ANVIL || BlockHelper.getBlock(pos) == Blocks.CHIPPED_ANVIL || BlockHelper.getBlock(pos) == Blocks.DAMAGED_ANVIL;
    }

    public static boolean isWeb(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.COBWEB || BlockHelper.getBlock(pos) == Block.getBlockFromItem(Items.STRING);
    }

    public static boolean isFaceTrapped(PlayerEntity target, boolean requireHole) {
        if (requireHole && !isInHole(target, true)) return false;
        for (CardinalDirection dir : CardinalDirection.values()) return isBlastResistant(target.getBlockPos().offset(dir.toDirection()));

        return false;
    }

    public static boolean isTrapped(PlayerEntity target, boolean requireHole) {
        if (requireHole && !isInHole(target, true)) return false;

        for (CardinalDirection dir : CardinalDirection.values())
            return isBlastResistant(target.getBlockPos().up().offset(dir.toDirection()))
                && isBlastResistant(target.getBlockPos().up(2));

        return false;
    }

    // Raytraces
    public static Direction rayTraceCheck(BlockPos pos, boolean forceReturn) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        Direction[] var3 = Direction.values();
        int var4 = var3.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            Direction direction = var3[var5];
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d((double) pos.getX() + 0.5D + (double) direction.getVector().getX() * 0.5D, (double) pos.getY() + 0.5D + (double) direction.getVector().getY() * 0.5D, (double) pos.getZ() + 0.5D + (double) direction.getVector().getZ() * 0.5D), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return direction;
            }
        }

        if (forceReturn) {
            if ((double) pos.getY() > eyesPos.y) {
                return Direction.DOWN;
            } else {
                return Direction.UP;
            }
        } else {
            return null;
        }
    }

    // Player Pos
    public static Vec3d eyePos(PlayerEntity player) {
        return player.getPos().add(0.0, player.getEyeHeight(player.getPose()), 0.0);
    }

    public static boolean positionChanged(PlayerEntity target) {
        return (int) mc.player.prevX != (int) mc.player.getX()
            || (int) mc.player.prevY != (int) mc.player.getY()
            || (int) mc.player.prevZ != (int) mc.player.getZ();
    }

    // World
    public static PlayerEntity worldEntity() {
        for (PlayerEntity entity : mc.world.getPlayers()) return entity;

        return null;
    }

    // Hole
    public static boolean isInHole(PlayerEntity target, boolean doubles) {
        if (!Utils.canUpdate()) return false;

        BlockPos blockPos = target.getBlockPos();
        int air = 0;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;

            BlockState state = mc.world.getBlockState(blockPos.offset(direction));

            if (state.getBlock().getBlastResistance() < 600) {
                if (!doubles || direction == Direction.DOWN) return false;

                air++;

                for (Direction dir : Direction.values()) {
                    if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                    BlockState blockState1 = mc.world.getBlockState(blockPos.offset(direction).offset(dir));

                    if (blockState1.getBlock().getBlastResistance() < 600) {
                        return false;
                    }
                }
            }
        }

        return air < 2;
    }

    public static boolean isSurroundBroken(PlayerEntity targetEntity) {
        return (!isBlastResistant(targetEntity.getBlockPos().add(1, 0, 0))
            && isBlastResistant(targetEntity.getBlockPos().add(-1, 0, 0))
            && isBlastResistant(targetEntity.getBlockPos().add(0, 0, 1))
            && isBlastResistant(targetEntity.getBlockPos().add(0, 0, -1)))

            || (isBlastResistant(targetEntity.getBlockPos().add(1, 0, 0))
            && !isBlastResistant(targetEntity.getBlockPos().add(-1, 0, 0))
            && isBlastResistant(targetEntity.getBlockPos().add(0, 0, 1))
            && isBlastResistant(targetEntity.getBlockPos().add(0, 0, -1)))

            || (isBlastResistant(targetEntity.getBlockPos().add(1, 0, 0))
            && isBlastResistant(targetEntity.getBlockPos().add(-1, 0, 0))
            && !isBlastResistant(targetEntity.getBlockPos().add(0, 0, 1))
            && isBlastResistant(targetEntity.getBlockPos().add(0, 0, -1)))

            || (isBlastResistant(targetEntity.getBlockPos().add(1, 0, 0))
            && isBlastResistant(targetEntity.getBlockPos().add(-1, 0, 0))
            && isBlastResistant(targetEntity.getBlockPos().add(0, 0, 1))
            && !isBlastResistant(targetEntity.getBlockPos().add(0, 0, -1)));
    }

    public static BlockPos getSelfTrapBlock(PlayerEntity p, boolean escapePrevention) {
        if (p == null) return null;
        BlockPos tpos = p.getBlockPos();
        List<BlockPos> selfTrapBlocks = new ArrayList<>();
        if (!escapePrevention && isTrapBlock(tpos.up(2))) return tpos.up(2);
        for (Vec3d stp : selfTrapPositions) {
            BlockPos stb = tpos.add((int) stp.x, (int) stp.y, (int) stp.z);
            if (isTrapBlock(stb)) selfTrapBlocks.add(stb);
        }
        if (selfTrapBlocks.isEmpty()) return null;
        return selfTrapBlocks.get(new Random().nextInt(selfTrapBlocks.size()));
    }

    public static boolean isTrapBlock(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.OBSIDIAN || BlockHelper.getBlock(pos) == Blocks.ENDER_CHEST;
    }

    // Health
    public static float getTotalHealth(PlayerEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    // Movement
    public static double getSpeed(Entity entity) {
        return new Vec3d(mc.player.getPos().x, mc.player.getPos().y, mc.player.getPos().z).distanceTo(new Vec3d(mc.player.prevX, mc.player.prevY, mc.player.prevZ));
    }

    public static boolean isMoving(PlayerEntity target) {
        return target.forwardSpeed != 0 || target.sidewaysSpeed != 0;
    }

    // Message
    public static void sendDM(String message, String msgFormat, PlayerEntity player) {
        mc.player.networkHandler.sendChatCommand(msgFormat + " " + player.getName().getString() + " " + message);
    }

    public static void sendChatMessage(String message) {
        mc.player.networkHandler.sendChatMessage(message);
    }

    // Place Pos ArrayList
    public static ArrayList<Vec3d> selfTrapPositions = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    public static ArrayList<BlockPos> expandedSurround = new ArrayList<>() {{
        add(new BlockPos(2, 0, 0));
        add(new BlockPos(-2, 0, 0));
        add(new BlockPos(0, 0, 2));
        add(new BlockPos(0, 0, -2));
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(1, 0, -1));
        add(new BlockPos(-1, 0, -1));
    }};
}
