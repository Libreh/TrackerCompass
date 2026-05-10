package me.libreh.trackercompass.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public class DimensionUtil {
    private static volatile Function<ServerLevel, ResourceKey<Level>> resolver = defaultResolver();

    private static Function<ServerLevel, ResourceKey<Level>> defaultResolver() {
        // Collapse arcade-dimensions spoofed levels back to vanilla keys when arcade-utils is present,
        // otherwise per-reset dimension UUIDs leak into saved data and break tracking after dim swaps.
        // The lambda only links arcade classes when invoked, so JVM verification is safe without arcade-utils.
        if (FabricLoader.getInstance().isModLoaded("arcade-utils")) {
            return ArcadeBridge::resolve;
        }
        return ServerLevel::dimension;
    }

    public static ResourceKey<Level> resolve(ServerLevel level) {
        return resolver.apply(level);
    }

    public static ResourceKey<Level> resolve(ServerPlayer player) {
        return resolve((ServerLevel) player.level());
    }

    public static void setResolver(Function<ServerLevel, ResourceKey<Level>> custom) {
        resolver = custom != null ? custom : ServerLevel::dimension;
    }

    public static String getDimensionName(ResourceKey<Level> dimension) {
        String path = dimension.identifier().getPath();
        if (path.equals("overworld")) return "Overworld";
        if (path.equals("the_nether") || path.equals("nether")) return "Nether";
        if (path.equals("the_end")) return "End";
        return dimension.identifier().toString();
    }

    public static String getDimensionName(ServerPlayer player) {
        return getDimensionName(resolve(player));
    }

    private DimensionUtil() {}

    private static final class ArcadeBridge {
        static ResourceKey<Level> resolve(ServerLevel level) {
            return net.casual.arcade.utils.level.LevelUtilsKt.getSpoofedOrRealDimension(level);
        }
    }
}
