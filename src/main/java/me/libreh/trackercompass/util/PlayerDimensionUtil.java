package me.libreh.trackercompass.util;

import me.libreh.trackercompass.data.TrackerCompassSavedData;
import me.libreh.trackercompass.tracking.PlayerDimensionPositions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class PlayerDimensionUtil {
    public static String getName(ResourceKey<Level> dimension) {
        if (dimension == Level.OVERWORLD) {
            return "Overworld";
        } else if (dimension == Level.NETHER) {
            return "Nether";
        } else if (dimension == Level.END) {
            return "End";
        }
        return "Unknown";
    }

    public static String getName(ServerPlayer player) {
        return getName(player.level().dimension());
    }

    public static BlockPos findPlayerInDimension(UUID targetUuid, ServerPlayer observer, MinecraftServer server, TrackerCompassSavedData persistentState) {
        ResourceKey<Level> observerDimension = observer.level().dimension();

        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.level().dimension().equals(observerDimension)) {
            return targetPlayer.blockPosition();
        }

        PlayerDimensionPositions dimPositions = persistentState.getPlayerDimensionPositions().get(targetUuid);
        if (dimPositions == null) {
            return null;
        }

        return dimPositions.getPosition(observerDimension);
    }
}
