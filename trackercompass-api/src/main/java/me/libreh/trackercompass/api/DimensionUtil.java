package me.libreh.trackercompass.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class DimensionUtil {
    public static String getDimensionName(ResourceKey<Level> dimension) {
        String path = dimension.identifier().getPath();
        if (path.equals("overworld")) return "Overworld";
        if (path.equals("the_nether") || path.equals("nether")) return "Nether";
        if (path.equals("the_end")) return "End";
        return dimension.identifier().toString();
    }

    public static String getDimensionName(ServerPlayer player) {
        return getDimensionName(player.level().dimension());
    }

    private DimensionUtil() {}
}
