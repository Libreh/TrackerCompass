package me.libreh.trackercompass.compass;

import me.libreh.trackercompass.api.DimensionUtil;
import me.libreh.trackercompass.api.DirectionArrow;
import me.libreh.trackercompass.api.PlayerDimensionPositions;
import me.libreh.trackercompass.api.TrackerText;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrackerPlaceholders {
    public static Map<String, Component> build(ServerPlayer viewer, @Nullable ServerPlayer target, @Nullable BlockPos targetPos, UUID targetUuid, MinecraftServer server) {
        Map<String, Component> placeholders = new HashMap<>();

        boolean isOnline = target != null;
        String name;
        if (isOnline) {
            name = target.getName().getString();
        } else {
            name = server.services().nameToIdCache()
                    .get(targetUuid)
                    .map(NameAndId::name)
                    .orElse("Unknown");
        }

        placeholders.put("player", Component.literal(name));
        placeholders.put("offline", isOnline ? Component.empty() : Component.literal("Offline "));
        placeholders.put("online", isOnline ? Component.literal("Online") : Component.empty());

        String dimension = "";
        if (targetPos != null) {
            if (ConfigManager.config().showDimension) {
                boolean sameDimension = isOnline && DimensionUtil.resolve(target).equals(DimensionUtil.resolve(viewer));
                if (isOnline && (!sameDimension || ConfigManager.config().showSameDimension)) {
                    String dimensionName = DimensionUtil.getDimensionName(target);
                    String color = resolveDimensionColor(target);
                    dimension = color + dimensionName;
                } else if (!isOnline) {
                    PlayerDimensionPositions positions = TrackerCompassSavedData.get(server).getPlayerDimensionPositions().get(targetUuid);
                    if (positions != null) {
                        net.minecraft.resources.ResourceKey<Level> lastDim = getLastKnownDimension(positions);
                        if (lastDim != null) {
                            String dimensionName = DimensionUtil.getDimensionName(lastDim);
                            String color = resolveDimensionColor(lastDim);
                            dimension = color + dimensionName;
                        }
                    }
                }
            }
        }
        placeholders.put("dimension", TrackerText.of(dimension).resolve(Map.of()));

        int distance = 0;
        if (targetPos != null) {
            distance = (int) Math.sqrt(viewer.blockPosition().distSqr(targetPos));
        }
        placeholders.put("distance", Component.literal(String.valueOf(distance)));

        String arrow = "";
        if (targetPos != null && ConfigManager.config().showDirectionArrow) {
            arrow = DirectionArrow.calculate(viewer, targetPos);
        }
        placeholders.put("arrow", Component.literal(arrow));

        return placeholders;
    }

    private static String resolveDimensionColor(ServerPlayer target) {
        var dimId = DimensionUtil.resolve(target).identifier();
        String color = ConfigManager.config().dimensionColors.get(dimId.toString());
        if (color != null) return color;

        String path = dimId.getPath();
        if (path.contains("overworld")) {
            color = ConfigManager.config().dimensionColors.get("minecraft:overworld");
        } else if (path.contains("nether")) {
            color = ConfigManager.config().dimensionColors.get("minecraft:the_nether");
        } else if (path.contains("end")) {
            color = ConfigManager.config().dimensionColors.get("minecraft:the_end");
        }
        return color != null ? color : "<gray>";
    }

    private static String resolveDimensionColor(net.minecraft.resources.ResourceKey<Level> dimension) {
        var dimId = dimension.identifier();
        String color = ConfigManager.config().dimensionColors.get(dimId.toString());
        if (color != null) return color;

        String path = dimId.getPath();
        if (path.contains("overworld")) {
            color = ConfigManager.config().dimensionColors.get("minecraft:overworld");
        } else if (path.contains("nether")) {
            color = ConfigManager.config().dimensionColors.get("minecraft:the_nether");
        } else if (path.contains("end")) {
            color = ConfigManager.config().dimensionColors.get("minecraft:the_end");
        }
        return color != null ? color : "<gray>";
    }

    private static net.minecraft.resources.ResourceKey<Level> getLastKnownDimension(PlayerDimensionPositions positions) {
        for (var entry : positions.getPositions().entrySet()) {
            String path = entry.getKey().identifier().getPath();
            if (path.contains("overworld")) return entry.getKey();
        }
        for (var entry : positions.getPositions().entrySet()) {
            String path = entry.getKey().identifier().getPath();
            if (path.contains("nether")) return entry.getKey();
        }
        for (var entry : positions.getPositions().entrySet()) {
            String path = entry.getKey().identifier().getPath();
            if (path.contains("end")) return entry.getKey();
        }
        return positions.getPositions().keySet().stream().findFirst().orElse(null);
    }
}
