package me.libreh.trackercompass.tracking;

import me.libreh.trackercompass.data.TrackerCompassPersistentState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class PlayerPositionTracker {
    private final TrackerCompassPersistentState persistentState;

    public PlayerPositionTracker(TrackerCompassPersistentState persistentState) {
        this.persistentState = persistentState;
    }

    public void updateAllPositions(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isDead()) {
                persistentState.clearPlayerPositions(player.getUuid());
                continue;
            }

            UUID playerId = player.getUuid();
            RegistryKey<World> dimension = player.getEntityWorld().getRegistryKey();
            BlockPos position = player.getBlockPos();

            persistentState.updatePlayerPosition(playerId, dimension, position);
        }
    }
}
