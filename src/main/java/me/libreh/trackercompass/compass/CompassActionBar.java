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
    private final TrackerCompassSavedData persistentState;
    private final Map<UUID, Boolean> wasHoldingCompass = new HashMap<>();

    public CompassActionBar(TrackerCompassSavedData persistentState) {
        this.persistentState = persistentState;
    }

    public void updateActionBars(MinecraftServer server) {
        if (!ConfigManager.config().actionBarInfo) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            boolean currentlyHolding = isPlayerHoldingTrackerCompass(player);
            Boolean wasHolding = wasHoldingCompass.get(playerId);

            wasHoldingCompass.put(playerId, currentlyHolding);

            if (ConfigManager.config().onlyShowWhenHoldingCompass) {
                if (wasHolding != null && wasHolding && !currentlyHolding) {
                    player.sendSystemMessage(Component.empty(), true);
                    continue;
                }
            }

            if (ConfigManager.config().onlyShowWhenHoldingCompass && !currentlyHolding) {
                continue;
            }

            UUID targetUuid = persistentState.getTargetPlayer(player.getUUID());
            if (targetUuid == null) {
                continue;
            }

            Component actionBarText = buildActionBarText(player, targetUuid, server);
            if (actionBarText != null) {
                player.sendSystemMessage(actionBarText, true);
            }
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        wasHoldingCompass.remove(player.getUUID());
    }

    private boolean isPlayerHoldingTrackerCompass(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return TrackerCompassItem.isTrackerCompass(mainHand) || TrackerCompassItem.isTrackerCompass(offHand);
    }

    private Component buildActionBarText(ServerPlayer player, UUID targetUuid, MinecraftServer server) {
        BlockPos targetPos = PlayerDimensionUtil.findPlayerInDimension(targetUuid, player, server, persistentState);
        if (targetPos == null) {
            return null;
        }

        double distance = Math.sqrt(player.blockPosition().distSqr(targetPos));

        String arrow = "";
        if (ConfigManager.config().showDirectionArrow) {
            arrow = DirectionArrow.calculate(player, targetPos) + " ";
        }

        String targetName;
        ServerPlayer targetPlayerEntity = server.getPlayerList().getPlayer(targetUuid);

        if (targetPlayerEntity != null) {
            targetName = targetPlayerEntity.getName().getString();

            if (ConfigManager.config().showStatusIndicators &&
                !targetPlayerEntity.level().dimension().equals(player.level().dimension())) {
                targetName += " (Portal)";
            }
        } else {
            Optional<NameAndId> playerConfigEntry = server.services().nameToIdCache().get(targetUuid);

            if (playerConfigEntry.isPresent()) {
                targetName = playerConfigEntry.get().name();
            } else {
                targetName = "Player";
            }

            if (ConfigManager.config().showStatusIndicators) {
                targetName += " (Offline)";
            }
        }

        String actionBarFormat;
        if (ConfigManager.config().showDistance) {
            actionBarFormat = String.format("%s%dm [%s]", arrow, (int)distance, targetName);
        } else {
            actionBarFormat = String.format("%s[%s]", arrow, targetName);
        }

        return Component.literal(actionBarFormat).withStyle(ChatFormatting.AQUA);
    }
}
