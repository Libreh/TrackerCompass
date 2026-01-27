package me.libreh.trackercompass.tracking;

import me.libreh.trackercompass.data.TrackerCompassSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class PlayerPositionTracker {
    private final TrackerCompassSavedData persistentState;

    public PlayerPositionTracker(TrackerCompassSavedData persistentState) {
        this.persistentState = persistentState;
    }

    public void updateAllPositions(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive()) {
                persistentState.clearPlayerPositions(player.getUUID());
                continue;
            }

            UUID playerId = player.getUUID();
            ResourceKey<Level> dimension = player.level().dimension();
            BlockPos position = player.blockPosition();

            persistentState.updatePlayerPosition(playerId, dimension, position);
        }
    }
}
