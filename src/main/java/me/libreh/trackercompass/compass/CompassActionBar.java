package me.libreh.trackercompass.compass;

import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassPersistentState;
import me.libreh.trackercompass.util.DirectionArrow;
import me.libreh.trackercompass.util.PlayerDimensionUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CompassActionBar {
    private final TrackerCompassPersistentState persistentState;
    private final Map<UUID, Boolean> wasHoldingCompass = new HashMap<>();

    public CompassActionBar(TrackerCompassPersistentState persistentState) {
        this.persistentState = persistentState;
    }

    public void updateActionBars(MinecraftServer server) {
        if (!ConfigManager.getConfig().actionBarInfo) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            boolean currentlyHolding = isPlayerHoldingTrackerCompass(player);
            Boolean wasHolding = wasHoldingCompass.get(playerId);

            wasHoldingCompass.put(playerId, currentlyHolding);

            if (ConfigManager.getConfig().onlyShowWhenHoldingCompass) {
                if (wasHolding != null && wasHolding && !currentlyHolding) {
                    player.sendMessage(Text.empty(), true);
                    continue;
                }
            }

            if (ConfigManager.getConfig().onlyShowWhenHoldingCompass && !currentlyHolding) {
                continue;
            }

            UUID targetUuid = persistentState.getTargetPlayer(player.getUuid());
            if (targetUuid == null) {
                continue;
            }

            Text actionBarText = buildActionBarText(player, targetUuid, server);
            if (actionBarText != null) {
                player.sendMessage(actionBarText, true);
            }
        }
    }

    public void onPlayerDisconnect(ServerPlayerEntity player) {
        wasHoldingCompass.remove(player.getUuid());
    }

    private boolean isPlayerHoldingTrackerCompass(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        return TrackerCompassItem.isTrackerCompass(mainHand) || TrackerCompassItem.isTrackerCompass(offHand);
    }

    private Text buildActionBarText(ServerPlayerEntity player, UUID targetUuid, MinecraftServer server) {
        BlockPos targetPos = PlayerDimensionUtil.findPlayerInDimension(targetUuid, player, server, persistentState);
        if (targetPos == null) {
            return null;
        }

        double distance = Math.sqrt(player.getBlockPos().getSquaredDistance(targetPos));

        String arrow = "";
        if (ConfigManager.getConfig().showDirectionArrow) {
            arrow = DirectionArrow.calculate(player, targetPos) + " ";
        }

        String targetName;
        ServerPlayerEntity targetPlayerEntity = server.getPlayerManager().getPlayer(targetUuid);

        if (targetPlayerEntity != null) {
            targetName = targetPlayerEntity.getName().getString();

            if (ConfigManager.getConfig().showStatusIndicators &&
                !targetPlayerEntity.getEntityWorld().getRegistryKey().equals(player.getEntityWorld().getRegistryKey())) {
                targetName += " (Portal)";
            }
        } else {
            Optional<PlayerConfigEntry> playerConfigEntry = server.getApiServices().nameToIdCache().getByUuid(targetUuid);

            if (playerConfigEntry.isPresent()) {
                targetName = playerConfigEntry.get().name();
            } else {
                targetName = "Player";
            }

            if (ConfigManager.getConfig().showStatusIndicators) {
                targetName += " (Offline)";
            }
        }

        String actionBarFormat;
        if (ConfigManager.getConfig().showDistance) {
            actionBarFormat = String.format("%s%dm [%s]", arrow, (int)distance, targetName);
        } else {
            actionBarFormat = String.format("%s[%s]", arrow, targetName);
        }

        return Text.literal(actionBarFormat).formatted(Formatting.AQUA);
    }
}
