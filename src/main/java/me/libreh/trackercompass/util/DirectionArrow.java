package me.libreh.trackercompass.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class DirectionArrow {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    public static String calculate(ServerPlayer player, BlockPos target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - player.getYRot() - 90;

        angle = (angle % 360 + 360) % 360;
        int index = (int) (((angle + 22.5) % 360) / 45);

       return ARROWS[index];
    }
}
