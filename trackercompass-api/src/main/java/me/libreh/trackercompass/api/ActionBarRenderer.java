package me.libreh.trackercompass.api;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class ActionBarRenderer {
    private final Map<UUID, Boolean> wasHolding = new HashMap<>();

    public void update(ServerPlayer player, BlockPos targetPos, boolean showActionBar, boolean holdToShow,
                       Predicate<ServerPlayer> isHolding, Component actionBarText) {
        if (!showActionBar) return;

        UUID playerId = player.getUUID();
        boolean holding = isHolding.test(player);
        Boolean wasHoldingBefore = wasHolding.put(playerId, holding);

        if (targetPos == null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
            return;
        }

        if (holdToShow && !holding) {
            if (wasHoldingBefore != null && wasHoldingBefore) {
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
            }
            return;
        }

        player.connection.send(new ClientboundSetActionBarTextPacket(actionBarText));
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        wasHolding.remove(player.getUUID());
    }

    public static Component buildDefaultText(ServerPlayer player, BlockPos targetPos, String targetName,
                                              boolean showArrow, boolean showDistance) {
        int distance = (int) Math.sqrt(player.blockPosition().distSqr(targetPos));
        String arrow = showArrow ? DirectionArrow.calculate(player, targetPos) + " " : "";

        String text = showDistance
                ? String.format("%s%dm [%s]", arrow, distance, targetName)
                : String.format("%s[%s]", arrow, targetName);

        return Component.literal(text).withStyle(ChatFormatting.AQUA);
    }
}
