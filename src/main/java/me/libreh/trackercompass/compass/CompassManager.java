package me.libreh.trackercompass.compass;

import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassPersistentState;
import me.libreh.trackercompass.util.PlayerDimensionUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public class CompassManager {
    private final TrackerCompassPersistentState persistentState;

    public CompassManager(TrackerCompassPersistentState persistentState) {
        this.persistentState = persistentState;
    }

    public void updateAllCompasses(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (shouldPlayerHaveCompass(player)) {
                ensurePlayerHasOneTrackerCompass(player);
            } else {
                removeAllTrackerCompasses(player);
            }
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            updatePlayerCompass(player, server);
        }
    }

    public void updatePlayerCompassImmediate(ServerPlayerEntity player) {
        if (shouldPlayerHaveCompass(player)) {
            ensurePlayerHasOneTrackerCompass(player);
        } else {
            removeAllTrackerCompasses(player);
        }
    }

    private boolean shouldPlayerHaveCompass(ServerPlayerEntity player) {
        Boolean playerToggle = persistentState.getPlayerCompassToggle(player.getUuid());
        if (playerToggle != null) {
            return playerToggle;
        }
        return ConfigManager.getConfig().giveCompassByDefault;
    }

    private void ensurePlayerHasOneTrackerCompass(ServerPlayerEntity player) {
        int trackerSlot = -1;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (TrackerCompassItem.isTrackerCompass(stack)) {
                if (trackerSlot == -1) {
                    trackerSlot = i;
                } else {
                    player.getInventory().removeStack(i);
                }
            }
        }

        if (trackerSlot == -1) {
            ItemStack trackerCompass = TrackerCompassItem.create();
            player.getInventory().insertStack(trackerCompass);
        }
    }

    private void removeAllTrackerCompasses(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (TrackerCompassItem.isTrackerCompass(stack)) {
                player.getInventory().removeStack(i);
            }
        }
    }

    private void updatePlayerCompass(ServerPlayerEntity player, MinecraftServer server) {
        UUID targetUuid = persistentState.getTargetPlayer(player.getUuid());
        if (targetUuid == null) {
            return;
        }

        BlockPos targetPos = PlayerDimensionUtil.findPlayerInDimension(targetUuid, player, server, persistentState);
        if (targetPos == null) {
            return;
        }

        RegistryKey<World> observerDimension = player.getEntityWorld().getRegistryKey();
        updateTrackerCompassesInInventory(player, targetPos, observerDimension);
    }

    private void updateTrackerCompassesInInventory(ServerPlayerEntity player, BlockPos targetPos, RegistryKey<World> dimension) {
        GlobalPos globalPos = GlobalPos.create(dimension, targetPos);
        LodestoneTrackerComponent tracker = new LodestoneTrackerComponent(Optional.of(globalPos), false);

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (TrackerCompassItem.isTrackerCompass(stack)) {
                stack.set(DataComponentTypes.LODESTONE_TRACKER, tracker);
            }
        }
    }
}
