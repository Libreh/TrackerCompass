package me.libreh.trackercompass.compass;

import eu.pb4.placeholders.api.ServerPlaceholderContext;
import me.libreh.trackercompass.api.ActionBarRenderer;
import me.libreh.trackercompass.api.CompassHelper;
import me.libreh.trackercompass.api.TargetPlaceholders;
import me.libreh.trackercompass.api.TrackerText;
import me.libreh.trackercompass.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class CompassActionBar {
    private final ActionBarRenderer renderer = new ActionBarRenderer();

    public void update(ServerPlayer player, MinecraftServer server, UUID targetUuid, BlockPos targetPos, @Nullable String actionbarFormat) {
        ServerPlayer target = targetPos != null ? server.getPlayerList().getPlayer(targetUuid) : null;

        Component text = null;
        if (targetPos != null && actionbarFormat != null) {
            TrackerText parsed = TrackerText.of(actionbarFormat);
            if (!parsed.isEmpty()) {
                Map<String, Component> placeholders = TrackerPlaceholders.build(player, target, targetPos, targetUuid, server);

                ServerPlaceholderContext ctx = ServerPlaceholderContext.of(server);
                text = TargetPlaceholders.withTarget(target, () -> parsed.resolve(placeholders, ctx));
            }
        }

        if (text != null) {
            renderer.update(player, targetPos,
                    ConfigManager.config().actionBarInfo,
                    ConfigManager.config().holdCompassForActionBar,
                    p -> CompassHelper.isHoldingCompass(p, TrackerCompassItem::isTrackerCompass),
                    text);
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        renderer.onPlayerDisconnect(player);
    }
}
