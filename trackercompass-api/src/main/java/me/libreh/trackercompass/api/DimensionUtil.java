package me.libreh.trackercompass.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class DimensionUtil {
    public static String getDimensionName(ResourceKey<Level> dimension) {
        if (dimension == Level.OVERWORLD) {
            return "Overworld";
        } else if (dimension == Level.NETHER) {
            return "Nether";
        } else if (dimension == Level.END) {
            return "End";
        }
        return "Unknown";
    }

    public static String getDimensionName(ServerPlayer player) {
        return getDimensionName(player.level().dimension());
    }

    private DimensionUtil() {}
}
