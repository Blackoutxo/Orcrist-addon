package me.blackout.orcrist.features.module.movement.elytrabot.botutil;

import net.minecraft.util.math.Direction;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * A list of directions including diagonal directions
 * P = Plus
 * M = Minus
 */
public enum DirectionUtils {
	XP("X-Plus"),
	XM("X-Minus"),
	ZP("Z-Plus"),
	ZM("Z-Minus"),
	XP_ZP("X-Plus, Z-Plus"),
	XM_ZP("X-Minus, Z-Plus"),
	XM_ZM("X-Minus, Z-Minus"),
	XP_ZM("X-Plus, Z-Minus");

	public String name;
	DirectionUtils(String name) {
		this.name = name;
	}

	/**
	 * Gets the direction the player is looking at
	 */
	public static DirectionUtils getDirection() {
		Direction facing = mc.player.getHorizontalFacing();
		return facing == facing.NORTH ? ZM : facing == facing.WEST ? XM : facing == facing.SOUTH ? ZP : XP;
	}

	/**
	 * Gets the closest diagonal direction player is looking at
	 */
	public static DirectionUtils getDiagonalDirection() {
		Direction facing = mc.player.getHorizontalFacing();

		if (facing.equals(facing.NORTH)) {
			double closest = getClosest(135, -135);
			return closest == -135 ? XP_ZM : XM_ZM;
		} else if (facing.equals(facing.WEST)) {
			double closest = getClosest(135, 45);
			return closest == 135 ? XM_ZM : XM_ZP;
		} else if (facing.equals(facing.EAST)) {
			double closest = getClosest(-45, -135);
			return closest == -135 ? XP_ZM : XP_ZP;
		} else {
			double closest = getClosest(45, -45);
			return closest == 45 ? XM_ZP : XP_ZP;
		}
	}

	//Returns the closer given yaw to the real yaw from a and b
	private static double getClosest(double a, double b) {
		double yaw = mc.player.getYaw();
		yaw = yaw < -180 ? yaw += 360 : yaw > 180 ? yaw -= 360 : yaw;

		if (Math.abs(yaw - a) < Math.abs(yaw - b)) {
			return a;
		} else {
			return b;
		}
	}
}
