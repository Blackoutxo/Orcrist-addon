package me.blackout.orcrist.utils.misc;

import baritone.api.BaritoneAPI;
import baritone.api.utils.Rotation;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaritoneUtils {
    private static Field targetField;

    public static Rotation getTarget() {
        findField();
        if (targetField == null) return null;

        targetField.setAccessible(true);

        try {
            return (Rotation) targetField.get(BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void findField() {
        if (targetField != null) return;

        Class<?> klass = BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior().getClass();

        for (Field field : klass.getDeclaredFields()) {
            if (field.getType() == Rotation.class) {
                targetField = field;
                break;
            }
        }
    }

    /**
     * Cancel everything baritone is doing
     */
    public static boolean isInRenderDistance(BlockPos pos) {
        int chunkX = (pos.getX() / 16);
        int chunkZ = (pos.getZ() / 16);
        return (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ));
    }
}
