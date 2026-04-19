package me.libreh.trackercompass.compass;

import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;

import java.util.Optional;

public class CompassManager {
    private final TrackerCompassSavedData data;

    public CompassManager(TrackerCompassSavedData data) {
        this.data = data;
    }

    public void update(ServerPlayer player, BlockPos targetPos) {
        syncInventory(player);
        if (targetPos != null) {
            pointToTarget(player, targetPos);
        }
    }

    public void syncInventory(ServerPlayer player) {
        if (hasCompassEnabled(player)) {
            giveCompass(player);
        } else {
            removeCompasses(player);
        }
    }

    private boolean hasCompassEnabled(ServerPlayer player) {
        Boolean toggle = data.getPlayerCompassToggle(player.getUUID());
        return toggle != null ? toggle : ConfigManager.config().giveCompassByDefault;
    }

    private void giveCompass(ServerPlayer player) {
        int found = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (TrackerCompassItem.isTrackerCompass(stack)) {
                if (found == -1) {
                    found = i;
                } else {
                    player.getInventory().removeItemNoUpdate(i);
                }
            }
        }

        if (found == -1) {
            player.getInventory().add(TrackerCompassItem.create());
        }
    }

    private void removeCompasses(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (TrackerCompassItem.isTrackerCompass(stack)) {
                player.getInventory().removeItemNoUpdate(i);
            }
        }
    }

    private void pointToTarget(ServerPlayer player, BlockPos targetPos) {
        GlobalPos globalPos = GlobalPos.of(player.level().dimension(), targetPos);
        LodestoneTracker tracker = new LodestoneTracker(Optional.of(globalPos), false);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (TrackerCompassItem.isTrackerCompass(stack)) {
                stack.set(DataComponents.LODESTONE_TRACKER, tracker);
            }
        }
    }
}
