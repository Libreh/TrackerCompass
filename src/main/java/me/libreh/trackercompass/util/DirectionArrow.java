package me.libreh.trackercompass.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class DirectionArrow {
    public static String calculate(ServerPlayerEntity player, BlockPos target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - player.getYaw() - 90;

        angle = (angle + 360) % 360;

        if (angle >= 337.5 || angle < 22.5) {
            return "↑";
        }
        if (angle >= 22.5 && angle < 67.5) {
            return "↗";
        }
        if (angle >= 67.5 && angle < 112.5) {
            return "→";
        }
        if (angle >= 112.5 && angle < 157.5) {
            return "↘";
        }
        if (angle >= 157.5 && angle < 202.5) {
            return "↓";
        }
        if (angle >= 202.5 && angle < 247.5) {
            return "↙";
        }
        if (angle >= 247.5 && angle < 292.5) {
            return "←";
        }
        return "↖";
    }
}
