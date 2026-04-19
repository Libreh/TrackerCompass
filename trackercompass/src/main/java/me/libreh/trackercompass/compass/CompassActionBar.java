package me.libreh.trackercompass.compass;

import me.libreh.trackercompass.api.ActionBarRenderer;
import me.libreh.trackercompass.api.CompassHelper;
import me.libreh.trackercompass.api.DimensionUtil;
import me.libreh.trackercompass.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Optional;
import java.util.UUID;

public class CompassActionBar {
    private final ActionBarRenderer renderer = new ActionBarRenderer();

    public void update(ServerPlayer player, MinecraftServer server, UUID targetUuid, BlockPos targetPos) {
        String targetName = targetPos != null ? resolveTargetName(player, server, targetUuid) : null;

        Component text = targetPos != null
                ? ActionBarRenderer.buildDefaultText(player, targetPos, targetName,
                    ConfigManager.config().showDirectionArrow, ConfigManager.config().showDistance)
                : null;

        renderer.update(player, targetPos,
                ConfigManager.config().actionBarInfo,
                ConfigManager.config().onlyShowWhenHoldingCompass,
                p -> CompassHelper.isHoldingCompass(p, TrackerCompassItem::isTrackerCompass),
                text);
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        renderer.onPlayerDisconnect(player);
    }

    private String resolveTargetName(ServerPlayer player, MinecraftServer server, UUID targetUuid) {
        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);

        if (target != null) {
            String name = target.getName().getString();

            if (!target.level().dimension().equals(player.level().dimension())) {
                if (ConfigManager.config().showDimension) {
                    name += " (" + DimensionUtil.getDimensionName(target) + ")";
                } else if (ConfigManager.config().showStatusIndicators) {
                    name += " (Other Dim)";
                }
            }
            return name;
        }

        Optional<NameAndId> cached = server.services().nameToIdCache().get(targetUuid);
        String name = cached.map(NameAndId::name).orElse("Player");

        if (ConfigManager.config().showStatusIndicators) {
            name += " (Offline)";
        }
        return name;
    }
}
