package me.libreh.trackercompass.compass;

import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import me.libreh.trackercompass.util.DirectionArrow;
import me.libreh.trackercompass.util.PlayerDimensionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CompassActionBar {
    private final Map<UUID, Boolean> wasHolding = new HashMap<>();

    public void update(ServerPlayer player, MinecraftServer server, UUID targetUuid, BlockPos targetPos) {
        if (!ConfigManager.config().actionBarInfo || targetPos == null) {
            return;
        }

        UUID playerId = player.getUUID();
        boolean holding = isHoldingCompass(player);
        Boolean wasHoldingBefore = wasHolding.put(playerId, holding);

        if (ConfigManager.config().onlyShowWhenHoldingCompass) {
            if (!holding) {
                if (wasHoldingBefore != null && wasHoldingBefore) {
                    player.sendSystemMessage(Component.empty(), true);
                }
                return;
            }
        }

        player.sendSystemMessage(buildActionBar(player, targetUuid, targetPos, server), true);
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        wasHolding.remove(player.getUUID());
    }

    private boolean isHoldingCompass(ServerPlayer player) {
        return TrackerCompassItem.isTrackerCompass(player.getMainHandItem())
            || TrackerCompassItem.isTrackerCompass(player.getOffhandItem());
    }

    private Component buildActionBar(ServerPlayer player, UUID targetUuid, BlockPos targetPos, MinecraftServer server) {
        int distance = (int) Math.sqrt(player.blockPosition().distSqr(targetPos));

        String arrow = ConfigManager.config().showDirectionArrow
            ? DirectionArrow.calculate(player, targetPos) + " "
            : "";

        String targetName;
        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);

        if (target != null) {
            targetName = target.getName().getString();

            if (ConfigManager.config().showStatusIndicators &&
                    !target.level().dimension().equals(player.level().dimension())) {
                targetName += " (Portal)";
            }
        } else {
            Optional<NameAndId> cached = server.services().nameToIdCache().get(targetUuid);
            targetName = cached.map(NameAndId::name).orElse("Player");

            if (ConfigManager.config().showStatusIndicators) {
                targetName += " (Offline)";
            }
        }

        String text = ConfigManager.config().showDistance
            ? String.format("%s%dm [%s]", arrow, distance, targetName)
            : String.format("%s[%s]", arrow, targetName);

        return Component.literal(text).withStyle(ChatFormatting.AQUA);
    }
}