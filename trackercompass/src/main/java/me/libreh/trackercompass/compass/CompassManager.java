package me.libreh.trackercompass.compass;

import me.libreh.trackercompass.api.CompassHelper;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class CompassManager {
    private final TrackerCompassSavedData data;

    public CompassManager(TrackerCompassSavedData data) {
        this.data = data;
    }

    public void update(ServerPlayer player, BlockPos targetPos) {
        if (targetPos != null) {
            CompassHelper.pointToTarget(player, targetPos, TrackerCompassItem::isTrackerCompass);
        }
    }

    public void syncInventory(ServerPlayer player) {
        if (hasCompassEnabled(player)) {
            CompassHelper.giveCompass(player, TrackerCompassItem::create, TrackerCompassItem::isTrackerCompass);
        } else {
            CompassHelper.removeCompasses(player, TrackerCompassItem::isTrackerCompass);
        }
    }

    private boolean hasCompassEnabled(ServerPlayer player) {
        Boolean toggle = data.getPlayerCompassToggle(player.getUUID());
        return toggle != null ? toggle : ConfigManager.config().giveCompassByDefault;
    }
}
