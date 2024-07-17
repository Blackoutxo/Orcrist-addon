package me.blackout.orcrist.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class CancellableMoveEvent extends Cancellable {
    private static final CancellableMoveEvent INSTANCE = new CancellableMoveEvent();

    public MovementType type;
    public Vec3d movement;

    public static CancellableMoveEvent get(MovementType type, Vec3d movement) {
        INSTANCE.setCancelled(false);
        INSTANCE.type = type;
        INSTANCE.movement = movement;
        return INSTANCE;
    }
}
