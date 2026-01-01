package me.libreh.trackercompass.util;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class DimensionHelper {
    public static String getName(RegistryKey<World> dimension) {
        if (dimension == World.OVERWORLD) {
            return "Overworld";
        } else if (dimension == World.NETHER) {
            return "Nether";
        } else if (dimension == World.END) {
            return "End";
        }
        return "Unknown";
    }

    public static String getName(ServerPlayerEntity player) {
        return getName(player.getEntityWorld().getRegistryKey());
    }
}
