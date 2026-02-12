package me.libreh.trackercompass.compass;

import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import me.libreh.trackercompass.util.PlayerDimensionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class CompassManager {
    private final TrackerCompassSavedData persistentState;

    public CompassManager(TrackerCompassSavedData persistentState) {
        this.persistentState = persistentState;
    }

    public void updateAllCompasses(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (shouldPlayerHaveCompass(player)) {
                ensurePlayerHasOneTrackerCompass(player);
            } else {
                removeAllTrackerCompasses(player);
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerCompass(player, server);
        }
    }

    public void updatePlayerCompassImmediate(ServerPlayer player) {
        if (shouldPlayerHaveCompass(player)) {
            ensurePlayerHasOneTrackerCompass(player);
        } else {
            removeAllTrackerCompasses(player);
        }
    }

    private boolean shouldPlayerHaveCompass(ServerPlayer player) {
        Boolean playerToggle = persistentState.getPlayerCompassToggle(player.getUUID());
        if (playerToggle != null) {
            return playerToggle;
        }
        return ConfigManager.config().giveCompassByDefault;
    }

    private void ensurePlayerHasOneTrackerCompass(ServerPlayer player) {
        int trackerSlot = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (TrackerCompassItem.isTrackerCompass(stack)) {
                if (trackerSlot == -1) {
                    trackerSlot = i;
                } else {
                    player.getInventory().removeItemNoUpdate(i);
                }
            }
        }

        if (trackerSlot == -1) {
            ItemStack trackerCompass = TrackerCompassItem.create();
            player.getInventory().add(trackerCompass);
        }
    }

    private void removeAllTrackerCompasses(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (TrackerCompassItem.isTrackerCompass(stack)) {
                player.getInventory().removeItemNoUpdate(i);
            }
        }
    }

    private void updatePlayerCompass(ServerPlayer player, MinecraftServer server) {
        UUID targetUuid = persistentState.getTargetPlayer(player.getUUID());
        if (targetUuid == null) {
            return;
        }

        BlockPos targetPos = PlayerDimensionUtil.findPlayerInDimension(targetUuid, player, server, persistentState);
        if (targetPos == null) {
            return;
        }

        ResourceKey<Level> observerDimension = player.level().dimension();
        updateTrackerCompassesInInventory(player, targetPos, observerDimension);
    }

    private void updateTrackerCompassesInInventory(ServerPlayer player, BlockPos targetPos, ResourceKey<Level> dimension) {
        GlobalPos globalPos = GlobalPos.of(dimension, targetPos);
        LodestoneTracker tracker = new LodestoneTracker(Optional.of(globalPos), false);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (TrackerCompassItem.isTrackerCompass(stack)) {
                stack.set(DataComponents.LODESTONE_TRACKER, tracker);
            }
        }
    }
}
