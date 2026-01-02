package me.libreh.trackercompass.util;

import me.libreh.trackercompass.data.TrackerCompassPersistentState;
import me.libreh.trackercompass.tracking.PlayerDimensionPositions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class PlayerDimensionUtil {
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

    public static BlockPos findPlayerInDimension(UUID targetUuid, ServerPlayerEntity observer, MinecraftServer server, TrackerCompassPersistentState persistentState) {
        RegistryKey<World> observerDimension = observer.getEntityWorld().getRegistryKey();

        ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.getEntityWorld().getRegistryKey().equals(observerDimension)) {
            return targetPlayer.getBlockPos();
        }

        PlayerDimensionPositions dimPositions = persistentState.getPlayerDimensionPositions().get(targetUuid);
        if (dimPositions == null) {
            return null;
        }

        return dimPositions.getPosition(observerDimension);
    }
}
